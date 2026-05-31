package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTests {

    @Mock
    private AdminUserService adminUserService;

    private AdminController controller;

    private AppUser sampleUser;
    private Map<String, Object> sampleUserDto;

    @BeforeEach
    void setUp() {
        controller = new AdminController(adminUserService);
        sampleUser = AppUser.builder()
                .id(1L)
                .email("user@example.com")
                .name("Test User")
                .role(AppUser.Role.USER)
                .locked(false)
                .build();
        sampleUserDto = Map.of(
                "id", 1L,
                "email", "user@example.com",
                "name", "Test User",
                "role", "USER",
                "locked", false
        );
    }

    @Test
    void listUsers_noFilters_returnsPagedResult() {
        Page<AppUser> page = new PageImpl<>(List.of(sampleUser));
        when(adminUserService.listUsers(null, null, null, null, null, 0, 20)).thenReturn(page);
        when(adminUserService.buildUserDto(sampleUser)).thenReturn(sampleUserDto);

        ResponseEntity<Map<String, Object>> response = controller.listUsers(null, null, null, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKeys("users", "totalElements", "totalPages", "page", "size");
        assertThat(body).containsEntry("totalElements", 1L);
    }

    @Test
    void listUsers_withEmailFilter_delegatesToService() {
        Page<AppUser> page = new PageImpl<>(List.of(sampleUser));
        when(adminUserService.listUsers("user@example.com", null, null, null, null, 0, 20)).thenReturn(page);
        when(adminUserService.buildUserDto(sampleUser)).thenReturn(sampleUserDto);

        controller.listUsers("user@example.com", null, null, null, null, 0, 20);

        verify(adminUserService).listUsers("user@example.com", null, null, null, null, 0, 20);
    }

    @Test
    void listUsers_emptyResult_returnsZeroTotalElements() {
        Page<AppUser> emptyPage = new PageImpl<>(List.of());
        when(adminUserService.listUsers(null, null, null, null, null, 0, 20)).thenReturn(emptyPage);

        ResponseEntity<Map<String, Object>> response = controller.listUsers(null, null, null, null, null, 0, 20);

        assertThat(response.getBody()).containsEntry("totalElements", 0L);
    }

    @Test
    void getUserDetail_existingUser_returnsDetail() {
        Map<String, Object> detail = Map.of("id", 1L, "email", "user@example.com", "reviewHistory", List.of());
        when(adminUserService.getUserDetail(1L)).thenReturn(detail);

        ResponseEntity<Map<String, Object>> response = controller.getUserDetail(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("id", 1L);
    }

    @Test
    void getUserHistory_returnsHistoryList() {
        List<Map<String, Object>> history = List.of(Map.of("date", "2026-01-01", "action", "review"));
        when(adminUserService.getUserHistory(1L, 20)).thenReturn(history);

        ResponseEntity<List<Map<String, Object>>> response = controller.getUserHistory(1L, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void lockUser_returnsLockedUserInfo() {
        Map<String, Object> result = Map.of("id", 1L, "locked", true);
        when(adminUserService.lockUser(1L)).thenReturn(result);

        ResponseEntity<Map<String, Object>> response = controller.lockUser(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("locked", true);
        verify(adminUserService).lockUser(1L);
    }

    @Test
    void unlockUser_returnsUnlockedUserInfo() {
        Map<String, Object> result = Map.of("id", 1L, "locked", false);
        when(adminUserService.unlockUser(1L)).thenReturn(result);

        ResponseEntity<Map<String, Object>> response = controller.unlockUser(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("locked", false);
        verify(adminUserService).unlockUser(1L);
    }

    @Test
    void resetPassword_returnsSuccessResponse() {
        Map<String, Object> result = Map.of("message", "Password reset email sent");
        when(adminUserService.resetPassword(1L)).thenReturn(result);

        ResponseEntity<Map<String, Object>> response = controller.resetPassword(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminUserService).resetPassword(1L);
    }

    @Test
    void assignRole_validRole_delegatesToService() {
        Map<String, Object> result = Map.of("id", 1L, "role", "ADMIN");
        when(adminUserService.assignRole(1L, "ADMIN")).thenReturn(result);

        ResponseEntity<Map<String, Object>> response = controller.assignRole(1L, Map.of("role", "ADMIN"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("role", "ADMIN");
        verify(adminUserService).assignRole(1L, "ADMIN");
    }

    @Test
    void assignRole_missingRole_returnsBadRequest() {
        ResponseEntity<Map<String, Object>> response = controller.assignRole(1L, Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "role is required");
        verifyNoInteractions(adminUserService);
    }

    @Test
    void assignRole_blankRole_returnsBadRequest() {
        ResponseEntity<Map<String, Object>> response = controller.assignRole(1L, Map.of("role", "  "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "role is required");
    }
}

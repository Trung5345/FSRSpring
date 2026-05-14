package com.fsrspring.vocab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.repository.AppUserRepository;
import com.fsrspring.vocab.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private AppUserRepository appUserRepository;

    private AppUser testUser;

    @BeforeEach
    public void setup() {
        testUser = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .role(AppUser.Role.USER)
                .build();
                
        when(currentUserService.getCurrentUser()).thenReturn(testUser);
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    public void testUnauthenticatedAccess_ShouldReturnUnauthorizedOrRedirect() throws Exception {
        // Hitting an API without authentication should result in 302 Redirect (to /login) or 401
        mockMvc.perform(get("/api/words")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 401));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    public void testAuthenticatedAccess_GetWords_ShouldReturnOk() throws Exception {
        // Authenticated access should succeed
        mockMvc.perform(get("/api/words")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    public void testAuthenticatedAccess_GetProgress_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/progress")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    public void testAuthenticatedAccess_GetTopics_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/topics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}

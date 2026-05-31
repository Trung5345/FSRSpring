package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.Topic;
import com.fsrspring.vocab.service.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicControllerTests {

    @Mock
    private TopicService topicService;

    private TopicController controller;
    private Topic sampleTopic;

    @BeforeEach
    void setUp() {
        controller = new TopicController(topicService);
        sampleTopic = Topic.builder()
                .id(1L)
                .name("Animals")
                .slug("animals")
                .description("Animal vocabulary")
                .build();
    }

    @Test
    void getAll_returnsTopicList() {
        when(topicService.getAll()).thenReturn(List.of(sampleTopic));

        List<Topic> response = controller.getAll();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("Animals");
        assertThat(response.get(0).getSlug()).isEqualTo("animals");
    }

    @Test
    void getAll_emptyList_returnsEmpty() {
        when(topicService.getAll()).thenReturn(List.of());

        List<Topic> response = controller.getAll();

        assertThat(response).isEmpty();
    }

    @Test
    void getById_existingId_returnsOk() {
        when(topicService.getById(1L)).thenReturn(sampleTopic);

        ResponseEntity<Topic> response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleTopic);
    }

    @Test
    void getById_nonExistingId_returnsNotFound() {
        when(topicService.getById(99L)).thenThrow(new IllegalArgumentException("Topic not found"));

        ResponseEntity<Topic> response = controller.getById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getBySlug_existingSlug_returnsOk() {
        when(topicService.getBySlug("animals")).thenReturn(Optional.of(sampleTopic));

        ResponseEntity<Topic> response = controller.getBySlug("animals");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleTopic);
    }

    @Test
    void getBySlug_unknownSlug_returnsNotFound() {
        when(topicService.getBySlug("unknown-slug")).thenReturn(Optional.empty());

        ResponseEntity<Topic> response = controller.getBySlug("unknown-slug");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void create_validTopic_returnsCreatedTopic() {
        when(topicService.create(sampleTopic)).thenReturn(sampleTopic);

        ResponseEntity<Topic> response = controller.create(sampleTopic);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleTopic);
        verify(topicService).create(sampleTopic);
    }

    @Test
    void create_duplicateTopic_returnsBadRequest() {
        when(topicService.create(any())).thenThrow(new RuntimeException("Duplicate slug: animals"));

        ResponseEntity<Topic> response = controller.create(sampleTopic);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_existingTopic_returnsUpdatedTopic() {
        Topic updated = Topic.builder().id(1L).name("Animals Updated").slug("animals-updated").build();
        when(topicService.update(1L, updated)).thenReturn(updated);

        ResponseEntity<Topic> response = controller.update(1L, updated);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Animals Updated");
    }

    @Test
    void update_nonExistingTopic_returnsNotFound() {
        when(topicService.update(eq(99L), any())).thenThrow(new IllegalArgumentException("Topic not found"));

        ResponseEntity<Topic> response = controller.update(99L, sampleTopic);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_existingTopic_returnsNoContent() {
        doNothing().when(topicService).delete(1L);

        ResponseEntity<Void> response = controller.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(topicService).delete(1L);
    }

    @Test
    void delete_nonExistingTopic_returnsNotFound() {
        doThrow(new RuntimeException("Topic not found")).when(topicService).delete(99L);

        ResponseEntity<Void> response = controller.delete(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

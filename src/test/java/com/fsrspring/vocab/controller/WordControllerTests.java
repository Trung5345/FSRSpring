package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WordControllerTests {

    @Mock
    private WordService wordService;

    private WordController controller;
    private Word sampleWord;

    @BeforeEach
    void setUp() {
        controller = new WordController(wordService);
        sampleWord = Word.builder()
                .id(1L)
                .word("hello")
                .translation("xin chào")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .build();
    }

    @Test
    void getAllWords_noParams_returnsAllWords() {
        when(wordService.getAllWords()).thenReturn(List.of(sampleWord));

        ResponseEntity<?> response = controller.getAllWords(null, null, null, null, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(List.of(sampleWord));
    }

    @Test
    void getAllWords_withSearch_callsSearchWords() {
        when(wordService.searchWords("hello")).thenReturn(List.of(sampleWord));

        ResponseEntity<?> response = controller.getAllWords(null, null, "hello", null, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(List.of(sampleWord));
        verify(wordService).searchWords("hello");
    }

    @Test
    void getAllWords_withCategory_callsGetWordsByCategory() {
        when(wordService.getWordsByCategory("Business")).thenReturn(List.of(sampleWord));

        ResponseEntity<?> response = controller.getAllWords("Business", null, null, null, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(wordService).getWordsByCategory("Business");
    }

    @Test
    void getAllWords_withDifficulty_callsGetWordsByDifficulty() {
        when(wordService.getWordsByDifficulty(Word.DifficultyLevel.BEGINNER)).thenReturn(List.of(sampleWord));

        ResponseEntity<?> response = controller.getAllWords(null, Word.DifficultyLevel.BEGINNER, null, null, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(wordService).getWordsByDifficulty(Word.DifficultyLevel.BEGINNER);
    }

    @Test
    void getAllWords_withCategoryAndDifficulty_callsGetWordsByCategoryAndDifficulty() {
        when(wordService.getWordsByCategoryAndDifficulty("Business", Word.DifficultyLevel.INTERMEDIATE))
                .thenReturn(List.of(sampleWord));

        ResponseEntity<?> response = controller.getAllWords("Business", Word.DifficultyLevel.INTERMEDIATE, null, null, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(wordService).getWordsByCategoryAndDifficulty("Business", Word.DifficultyLevel.INTERMEDIATE);
    }

    @Test
    void getWordById_existingId_returnsWord() {
        when(wordService.getWordById(1L)).thenReturn(sampleWord);

        ResponseEntity<Word> response = controller.getWordById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleWord);
    }

    @Test
    void getRandomWords_returnsWords() {
        when(wordService.getRandomWords(10)).thenReturn(List.of(sampleWord));

        ResponseEntity<List<Word>> response = controller.getRandomWords(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getCategories_returnsAllCategories() {
        when(wordService.getAllCategories()).thenReturn(List.of("General", "Business", "Travel"));

        ResponseEntity<List<String>> response = controller.getCategories();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("General", "Business", "Travel");
    }

    @Test
    void getPartsOfSpeech_returnsList() {
        when(wordService.getAllPartsOfSpeech()).thenReturn(List.of("noun", "verb", "adjective"));

        ResponseEntity<List<String>> response = controller.getPartsOfSpeech();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("noun", "verb", "adjective");
    }

    @Test
    void createWord_validWord_returnsCreated() {
        when(wordService.createWord(sampleWord)).thenReturn(sampleWord);

        ResponseEntity<Word> response = controller.createWord(sampleWord);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(sampleWord);
        verify(wordService).createWord(sampleWord);
    }

    @Test
    void updateWord_existingWord_returnsUpdatedWord() {
        when(wordService.updateWord(1L, sampleWord)).thenReturn(sampleWord);

        ResponseEntity<Word> response = controller.updateWord(1L, sampleWord);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleWord);
    }

    @Test
    void deleteWord_existingId_returnsNoContent() {
        doNothing().when(wordService).deleteWord(1L);

        ResponseEntity<Void> response = controller.deleteWord(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(wordService).deleteWord(1L);
    }

    @Test
    void getWordCount_returnsCountMap() {
        when(wordService.countWords()).thenReturn(42L);

        ResponseEntity<Map<String, Long>> response = controller.getWordCount();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("count", 42L);
    }

    @Test
    void handleNotFound_returnsNotFoundWithMessage() {
        ResponseEntity<Map<String, String>> response = controller.handleNotFound(
                new NoSuchElementException("Word with id=99 not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Word with id=99 not found");
    }

    @Test
    void handleBadRequest_returnsConflictWithMessage() {
        ResponseEntity<Map<String, String>> response = controller.handleBadRequest(
                new IllegalArgumentException("Word already exists: hello"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "Word already exists: hello");
    }
}

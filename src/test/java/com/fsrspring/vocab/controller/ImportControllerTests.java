package com.fsrspring.vocab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsrspring.vocab.model.CefrLevel;
import com.fsrspring.vocab.model.ImportJob;
import com.fsrspring.vocab.model.Topic;
import com.fsrspring.vocab.model.VocabularySet;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.ImportJobRepository;
import com.fsrspring.vocab.repository.TopicRepository;
import com.fsrspring.vocab.repository.VocabularySetRepository;
import com.fsrspring.vocab.repository.WordEnrichmentJobRepository;
import com.fsrspring.vocab.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.enrichment.enabled=true",
        "app.enrichment.poll-delay-ms=600000"
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ImportControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private VocabularySetRepository vocabularySetRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private WordEnrichmentJobRepository wordEnrichmentJobRepository;

    @BeforeEach
    void resetDatabase() {
        vocabularySetRepository.deleteAll();
        wordEnrichmentJobRepository.deleteAll();
        wordRepository.deleteAll();
        importJobRepository.deleteAll();
        topicRepository.deleteAll();
    }

    @Test
    void commitBatchCreatesWordsAndJobRows() throws Exception {
        mockMvc.perform(post("/api/import/words/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "JSON",
                                "fileName", "batch.json",
                                "rows", List.of(Map.of(
                                        "clientRowId", "r1",
                                        "word", "resilient",
                                        "translation", "kien cuong",
                                        "difficulty", "INTERMEDIATE"
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.rows[0].status").value("CREATED"));

        assertThat(wordRepository.existsByWordIgnoreCase("resilient")).isTrue();
        ImportJob job = importJobRepository.findTop50ByOrderByCreatedAtDesc().get(0);
        assertThat(job.getSourceType()).isEqualTo("JSON");
        assertThat(job.getCreatedCount()).isEqualTo(1);
    }

    @Test
    void duplicateWordsAreSkippedWithoutCreatingAnotherWord() throws Exception {
        wordRepository.save(Word.builder()
                .word("database")
                .translation("co so du lieu")
                .difficulty(Word.DifficultyLevel.INTERMEDIATE)
                .build());

        mockMvc.perform(post("/api/import/words/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "CSV",
                                "rows", List.of(Map.of(
                                        "clientRowId", "r1",
                                        "word", "Database",
                                        "translation", "du lieu",
                                        "difficulty", "INTERMEDIATE"
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.rows[0].status").value("SKIPPED"));

        assertThat(wordRepository.findAll()).hasSize(1);
    }

    @Test
    void duplicateWordsInsideSameBatchAreSkipped() throws Exception {
        mockMvc.perform(post("/api/import/words/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "CSV",
                                "rows", List.of(
                                        Map.of(
                                                "clientRowId", "r1",
                                                "word", "tiger",
                                                "translation", "con ho",
                                                "difficulty", "BEGINNER"
                                        ),
                                        Map.of(
                                                "clientRowId", "r2",
                                                "word", "TIGER",
                                                "translation", "ho",
                                                "difficulty", "BEGINNER"
                                        )
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.rows[0].status").value("CREATED"))
                .andExpect(jsonPath("$.rows[1].status").value("SKIPPED"));

        assertThat(wordRepository.findAll()).hasSize(1);
    }

    @Test
    void invalidRowsFailButValidRowsStillSave() throws Exception {
        mockMvc.perform(post("/api/import/words/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "PASTE",
                                "rows", List.of(
                                        Map.of(
                                                "clientRowId", "ok",
                                                "word", "negotiate",
                                                "translation", "dam phan",
                                                "difficulty", "ADVANCED"
                                        ),
                                        Map.of(
                                                "clientRowId", "bad",
                                                "word", "missing-translation",
                                                "difficulty", "BEGINNER"
                                        ),
                                        Map.of(
                                                "clientRowId", "invalid",
                                                "word", "",
                                                "difficulty", "BEGINNER"
                                        )
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.rows", hasSize(3)))
                .andExpect(jsonPath("$.rows[1].status").value("CREATED"))
                .andExpect(jsonPath("$.rows[1].enrichmentStatus").value("PENDING"));

        assertThat(wordRepository.existsByWordIgnoreCase("negotiate")).isTrue();
        assertThat(wordRepository.existsByWordIgnoreCase("missing-translation")).isTrue();
        assertThat(wordEnrichmentJobRepository.findAll()).hasSize(2);
        ImportJob job = importJobRepository.findTop50ByOrderByCreatedAtDesc().get(0);
        assertThat(job.getStatus()).isEqualTo(ImportJob.Status.COMPLETED_WITH_ERRORS);
    }

    @Test
    @Transactional
    void targetSetCreatesSetAndLinksCreatedWords() throws Exception {
        Topic topic = topicRepository.save(Topic.builder()
                .name("Business")
                .slug("business")
                .description("Business words")
                .build());

        mockMvc.perform(post("/api/import/words/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "CSV",
                                "targetSet", Map.of(
                                        "name", "Business Import",
                                        "topicId", topic.getId(),
                                        "cefrLevel", "B2"
                                ),
                                "rows", List.of(Map.of(
                                        "clientRowId", "r1",
                                        "word", "revenue",
                                        "translation", "doanh thu",
                                        "topicId", topic.getId(),
                                        "cefrLevel", "B2",
                                        "difficulty", "INTERMEDIATE"
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(1));

        VocabularySet set = vocabularySetRepository.findAll().get(0);
        assertThat(set.getName()).isEqualTo("Business Import");
        assertThat(set.getCefrLevel()).isEqualTo(CefrLevel.B2);
        assertThat(set.getWords()).hasSize(1);
    }

    @Test
    @Transactional
    void legacyTargetSetNameStillCreatesSetForImportPageCompatibility() throws Exception {
        mockMvc.perform(post("/api/import/words/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "PASTE",
                                "targetSetName", "Legacy UI Set",
                                "rows", List.of(Map.of(
                                        "clientRowId", "r1",
                                        "word", "adaptable",
                                        "translation", "linh hoat",
                                        "difficulty", "INTERMEDIATE"
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(1));

        VocabularySet set = vocabularySetRepository.findAll().get(0);
        assertThat(set.getName()).isEqualTo("Legacy UI Set");
        assertThat(set.getWords()).hasSize(1);
    }

    @Test
    void autoEnrichFalseRequiresTranslationAndDoesNotQueueJob() throws Exception {
        mockMvc.perform(post("/api/import/words/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "PASTE",
                                "options", Map.of("autoEnrich", false),
                                "rows", List.of(Map.of(
                                        "clientRowId", "r1",
                                        "word", "untranslated",
                                        "difficulty", "BEGINNER"
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.rows[0].status").value("FAILED"))
                .andExpect(jsonPath("$.rows[0].message").value("Translation is required"));

        assertThat(wordRepository.existsByWordIgnoreCase("untranslated")).isFalse();
        assertThat(wordEnrichmentJobRepository.findAll()).isEmpty();
    }

    @Test
    void commitPersistsDictionaryEnrichedFieldsFromImportRows() throws Exception {
        mockMvc.perform(post("/api/import/words/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "PASTE",
                                "rows", List.of(Map.of(
                                        "clientRowId", "r1",
                                        "word", "meticulous",
                                        "translation", "showing great attention to detail",
                                        "example", "She kept meticulous records.",
                                        "pronunciation", "/məˈtɪkjʊləs/",
                                        "partOfSpeech", "adjective",
                                        "audioUrl", "https://audio.example/meticulous.mp3",
                                        "difficulty", "ADVANCED"
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.rows[0].enrichmentStatus").value("PENDING"));

        Word word = wordRepository.findByWordIgnoreCase("meticulous").orElseThrow();
        assertThat(word.getTranslation()).isEqualTo("showing great attention to detail");
        assertThat(word.getExample()).isEqualTo("She kept meticulous records.");
        assertThat(word.getPronunciation()).isEqualTo("/məˈtɪkjʊləs/");
        assertThat(word.getPartOfSpeech()).isEqualTo("adjective");
        assertThat(word.getAudioUrl()).isEqualTo("https://audio.example/meticulous.mp3");
        assertThat(wordEnrichmentJobRepository.findAll()).hasSize(1);
    }

    @Test
    void jobDetailReturnsPersistedRowStatuses() throws Exception {
        String body = mockMvc.perform(post("/api/import/words/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "MANUAL",
                                "rows", List.of(Map.of(
                                        "clientRowId", "r1",
                                        "word", "manual",
                                        "translation", "thu cong",
                                        "difficulty", "BEGINNER"
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long jobId = objectMapper.readTree(body).get("importJobId").asLong();

        mockMvc.perform(get("/api/import/jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].status").value("CREATED"))
                .andExpect(jsonPath("$.rows[0].word").value("manual"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}

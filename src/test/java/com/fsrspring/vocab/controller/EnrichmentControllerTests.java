package com.fsrspring.vocab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.model.WordEnrichmentJob;
import com.fsrspring.vocab.service.LibreTranslateClient;
import com.fsrspring.vocab.service.WordEnrichmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class EnrichmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WordEnrichmentService enrichmentService;

    @MockBean
    private LibreTranslateClient libreTranslateClient;

    @Test
    void enqueueReturnsAcceptedJobStatus() throws Exception {
        when(enrichmentService.retryWord(7L)).thenReturn(job(91L, 7L, WordEnrichmentJob.Status.PENDING));

        mockMvc.perform(post("/api/enrichment/words/{id}", 7L))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(91))
                .andExpect(jsonPath("$.wordId").value(7))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void statusReturnsLatestJob() throws Exception {
        when(enrichmentService.latestJob(7L)).thenReturn(Optional.of(job(91L, 7L, WordEnrichmentJob.Status.COMPLETED)));

        mockMvc.perform(get("/api/enrichment/words/{id}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(91))
                .andExpect(jsonPath("$.wordId").value(7))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void statusReturnsNotFoundWhenWordHasNoJob() throws Exception {
        when(enrichmentService.latestJob(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/enrichment/words/{id}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No enrichment job found for word: 404"));
    }

    @Test
    void translateOnlyFillsRowsMissingTranslation() throws Exception {
        when(libreTranslateClient.translateEnToVi("meticulous")).thenReturn(Optional.of("ti mi"));

        Map<String, Object> request = Map.of("rows", List.of(
                Map.of("clientRowId", "r1", "word", "meticulous"),
                Map.of("clientRowId", "r2", "word", "known", "translation", "da co")
        ));

        mockMvc.perform(post("/api/enrichment/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].translation").value("ti mi"))
                .andExpect(jsonPath("$.rows[0].aiTranslated").value(true))
                .andExpect(jsonPath("$.rows[0].translationProvider").value("LIBRE_TRANSLATE"))
                .andExpect(jsonPath("$.rows[1].translation").value("da co"));
    }

    private WordEnrichmentJob job(Long jobId, Long wordId, WordEnrichmentJob.Status status) {
        return WordEnrichmentJob.builder()
                .id(jobId)
                .word(Word.builder().id(wordId).word("meticulous").build())
                .status(status)
                .attempts(1)
                .nextRunAt(LocalDateTime.now())
                .build();
    }
}

package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.ProviderWordData;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.model.WordEnrichmentJob;
import com.fsrspring.vocab.repository.WordEnrichmentJobRepository;
import com.fsrspring.vocab.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "app.enrichment.enabled=true",
        "app.enrichment.poll-delay-ms=600000"
})
@ActiveProfiles("test")
class WordEnrichmentServiceTests {

    @Autowired
    private WordEnrichmentService enrichmentService;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private WordEnrichmentJobRepository jobRepository;

    @MockBean
    private LibreTranslateClient libreTranslateClient;

    @MockBean
    private WiktApiClient wiktApiClient;

    @MockBean
    private DictionaryApiService dictionaryApiService;

    @MockBean
    private DatamuseApiService datamuseApiService;

    @MockBean
    private WordImageService wordImageService;

    @BeforeEach
    void reset() {
        jobRepository.deleteAll();
        wordRepository.deleteAll();
    }

    @Test
    void processJobFillsBlankFieldsAndPreservesUserFields() {
        when(libreTranslateClient.translateEnToVi("meticulous")).thenReturn(Optional.of("ti mi"));
        when(wiktApiClient.lookupEnglish("meticulous")).thenReturn(ProviderWordData.builder()
                .pronunciation("/məˈtɪkjələs/")
                .partOfSpeech("adjective")
                .example("He is meticulous in his work.")
                .build());
        when(dictionaryApiService.lookup(anyString())).thenReturn(Optional.empty());
        when(datamuseApiService.getSynonyms(anyString())).thenReturn(List.of("careful"));
        when(datamuseApiService.getAntonyms(anyString())).thenReturn(List.of());
        when(wordImageService.findAndStoreImage("meticulous")).thenReturn(Optional.of(
                new WordImageService.ImageResult("/generated-images/meticulous.jpg", "{\"provider\":\"PIXABAY\"}")
        ));

        Word saved = wordRepository.save(Word.builder()
                .word("meticulous")
                .translation("nguoi dung nhap")
                .difficulty(Word.DifficultyLevel.ADVANCED)
                .build());
        WordEnrichmentJob job = enrichmentService.enqueueWord(saved.getId());

        enrichmentService.processJobNow(job.getId());

        Word enriched = wordRepository.findById(saved.getId()).orElseThrow();
        assertThat(enriched.getTranslation()).isEqualTo("nguoi dung nhap");
        assertThat(enriched.getPronunciation()).isEqualTo("/məˈtɪkjələs/");
        assertThat(enriched.getPartOfSpeech()).isEqualTo("adjective");
        assertThat(enriched.getExample()).isEqualTo("He is meticulous in his work.");
        assertThat(enriched.getSynonyms()).isEqualTo("careful");
        assertThat(enriched.getImageUrl()).isEqualTo("/generated-images/meticulous.jpg");
        assertThat(enriched.getEnrichmentStatus()).isEqualTo(Word.EnrichmentStatus.COMPLETED);
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus()).isEqualTo(WordEnrichmentJob.Status.COMPLETED);
    }

    @Test
    void processJobReplacesBrokenLocalImageUrl() {
        when(wiktApiClient.lookupEnglish("cat")).thenReturn(ProviderWordData.builder().build());
        when(dictionaryApiService.lookup(anyString())).thenReturn(Optional.empty());
        when(wordImageService.findAndStoreImage("cat")).thenReturn(Optional.of(
                new WordImageService.ImageResult("/generated-images/cat.jpg", "{\"provider\":\"PIXABAY\"}")
        ));

        Word saved = wordRepository.save(Word.builder()
                .word("cat")
                .translation("con meo")
                .pronunciation("/kat/")
                .audioUrl("https://audio.example.test/cat.mp3")
                .partOfSpeech("noun")
                .example("The cat is sleeping.")
                .synonyms("feline")
                .antonyms("dog")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .imageUrl("/generated-images/missing-from-volume.jpg")
                .build());
        WordEnrichmentJob job = enrichmentService.retryWord(saved.getId());

        enrichmentService.processJobNow(job.getId());

        Word enriched = wordRepository.findById(saved.getId()).orElseThrow();
        assertThat(enriched.getImageUrl()).isEqualTo("/generated-images/cat.jpg");
        assertThat(enriched.getImageMetadataJson()).isEqualTo("{\"provider\":\"PIXABAY\"}");
        assertThat(enriched.getEnrichmentStatus()).isEqualTo(Word.EnrichmentStatus.COMPLETED);
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus()).isEqualTo(WordEnrichmentJob.Status.COMPLETED);
    }
}

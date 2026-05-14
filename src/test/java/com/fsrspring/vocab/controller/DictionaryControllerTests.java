package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.dto.DictionaryApiResponse;
import com.fsrspring.vocab.service.WordEnrichmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DictionaryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WordEnrichmentService enrichmentService;

    @Test
    void lookupExposesComputedFieldsUsedByImportPage() throws Exception {
        DictionaryApiResponse response = new DictionaryApiResponse();
        response.setWord("meticulous");
        response.setPhonetic("/məˈtɪkjʊləs/");

        DictionaryApiResponse.Phonetic phonetic = new DictionaryApiResponse.Phonetic();
        phonetic.setAudio("//audio.example/meticulous.mp3");
        response.setPhonetics(List.of(phonetic));

        DictionaryApiResponse.Definition definition = new DictionaryApiResponse.Definition();
        definition.setDefinition("showing great attention to detail");
        definition.setExample("She kept meticulous records.");

        DictionaryApiResponse.Meaning meaning = new DictionaryApiResponse.Meaning();
        meaning.setPartOfSpeech("adjective");
        meaning.setDefinitions(List.of(definition));
        response.setMeanings(List.of(meaning));

        when(enrichmentService.previewLookup("meticulous")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/dictionary/lookup/{word}", "meticulous"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("meticulous"))
                .andExpect(jsonPath("$.firstDefinition").value("showing great attention to detail"))
                .andExpect(jsonPath("$.firstExample").value("She kept meticulous records."))
                .andExpect(jsonPath("$.bestPhonetic").value("/məˈtɪkjʊləs/"))
                .andExpect(jsonPath("$.bestAudioUrl").value("//audio.example/meticulous.mp3"))
                .andExpect(jsonPath("$.primaryPartOfSpeech").value("adjective"));
    }

    @Test
    void lookupReturnsNotFoundWhenProviderHasNoWord() throws Exception {
        when(enrichmentService.previewLookup("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dictionary/lookup/{word}", "missing"))
                .andExpect(status().isNotFound());
    }
}

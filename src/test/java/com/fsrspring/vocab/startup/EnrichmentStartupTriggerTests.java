package com.fsrspring.vocab.startup;

import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.service.WordEnrichmentService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EnrichmentStartupTriggerTests {

    @Test
    void doesNotScanWordsWhenStartupEnrichmentIsDisabled() {
        WordEnrichmentService enrichmentService = mock(WordEnrichmentService.class);
        EnrichmentProperties properties = new EnrichmentProperties();
        properties.getEnrichment().setEnabled(false);

        new EnrichmentStartupTrigger(enrichmentService, properties).onApplicationReady();

        verify(enrichmentService, never()).enrichAllMissing();
    }
}

package com.fsrspring.vocab.startup;

import com.fsrspring.vocab.service.WordEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Automatically queues unenriched words for the enrichment pipeline
 * once the application has fully started.
 *
 * Words seeded via data.sql with NULL translation/pronunciation/synonyms etc.
 * are picked up here and handed off to WordEnrichmentService, which processes
 * them asynchronously via the scheduled poller.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnrichmentStartupTrigger {

    private final WordEnrichmentService enrichmentService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            int queued = enrichmentService.enrichAllMissing();
            if (queued > 0) {
                log.info("Startup enrichment: queued {} words for background enrichment", queued);
            } else {
                log.debug("Startup enrichment: all words already enriched, nothing to queue");
            }
        } catch (Exception e) {
            log.warn("Startup enrichment trigger failed (non-fatal): {}", e.getMessage());
        }
    }
}

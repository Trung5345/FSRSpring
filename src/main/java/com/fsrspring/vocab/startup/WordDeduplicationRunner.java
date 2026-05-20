package com.fsrspring.vocab.startup;

import com.fsrspring.vocab.model.ReviewEvent;
import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.VocabularySet;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.model.WordEnrichmentJob;
import com.fsrspring.vocab.model.WordOfTheDay;
import com.fsrspring.vocab.repository.ReviewEventRepository;
import com.fsrspring.vocab.repository.UserProgressRepository;
import com.fsrspring.vocab.repository.VocabularySetRepository;
import com.fsrspring.vocab.repository.WordEnrichmentJobRepository;
import com.fsrspring.vocab.repository.WordOfTheDayRepository;
import com.fsrspring.vocab.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WordDeduplicationRunner implements ApplicationRunner {

    private static final String UNIQUE_INDEX_NAME = "uk_words_word";

    private final WordRepository wordRepository;
    private final UserProgressRepository userProgressRepository;
    private final ReviewEventRepository reviewEventRepository;
    private final WordOfTheDayRepository wordOfTheDayRepository;
    private final WordEnrichmentJobRepository wordEnrichmentJobRepository;
    private final VocabularySetRepository vocabularySetRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        int removed = mergeDuplicateWords();
        wordRepository.flush();
        ensureUniqueWordIndex();
        if (removed > 0) {
            log.info("Merged {} duplicate word rows and enforced unique words index", removed);
        }
    }

    private int mergeDuplicateWords() {
        List<Word> words = wordRepository.findAll().stream()
                .sorted(Comparator.comparing(Word::getId))
                .toList();
        Map<String, List<Word>> groups = new LinkedHashMap<>();
        for (Word word : words) {
            groups.computeIfAbsent(normalize(word.getWord()), ignored -> new ArrayList<>()).add(word);
        }

        int removed = 0;
        for (List<Word> group : groups.values()) {
            if (group.size() < 2) {
                continue;
            }
            Word canonical = group.get(0);
            List<Word> duplicates = group.subList(1, group.size());
            mergeWordData(canonical, duplicates);
            mergeVocabularySets(canonical, duplicates);
            mergeUserProgress(canonical, group);
            reassignDependentRows(canonical, duplicates);
            wordRepository.save(canonical);
            wordRepository.deleteAll(duplicates);
            removed += duplicates.size();
        }
        return removed;
    }

    private void mergeWordData(Word canonical, List<Word> duplicates) {
        canonical.setWord(canonical.getWord().trim());
        for (Word duplicate : duplicates) {
            canonical.setTranslation(firstNonBlank(canonical.getTranslation(), duplicate.getTranslation()));
            canonical.setExample(firstNonBlank(canonical.getExample(), duplicate.getExample()));
            canonical.setPronunciation(firstNonBlank(canonical.getPronunciation(), duplicate.getPronunciation()));
            canonical.setCategory(firstNonBlank(canonical.getCategory(), duplicate.getCategory()));
            canonical.setImageUrl(firstNonBlank(canonical.getImageUrl(), duplicate.getImageUrl()));
            canonical.setAudioUrl(firstNonBlank(canonical.getAudioUrl(), duplicate.getAudioUrl()));
            canonical.setSynonyms(firstNonBlank(canonical.getSynonyms(), duplicate.getSynonyms()));
            canonical.setAntonyms(firstNonBlank(canonical.getAntonyms(), duplicate.getAntonyms()));
            canonical.setOrigin(firstNonBlank(canonical.getOrigin(), duplicate.getOrigin()));
            canonical.setEnrichmentJson(firstNonBlank(canonical.getEnrichmentJson(), duplicate.getEnrichmentJson()));
            canonical.setImageMetadataJson(firstNonBlank(canonical.getImageMetadataJson(), duplicate.getImageMetadataJson()));
            if (canonical.getTopic() == null) {
                canonical.setTopic(duplicate.getTopic());
            }
            if (canonical.getCefrLevel() == null) {
                canonical.setCefrLevel(duplicate.getCefrLevel());
            }
            if (canonical.getPartOfSpeech() == null) {
                canonical.setPartOfSpeech(duplicate.getPartOfSpeech());
            }
            if (duplicate.getEnrichedAt() != null
                    && (canonical.getEnrichedAt() == null || duplicate.getEnrichedAt().isAfter(canonical.getEnrichedAt()))) {
                canonical.setEnrichedAt(duplicate.getEnrichedAt());
            }
            if (duplicate.getEnrichmentStatus().ordinal() > canonical.getEnrichmentStatus().ordinal()) {
                canonical.setEnrichmentStatus(duplicate.getEnrichmentStatus());
            }
        }
    }

    private void mergeVocabularySets(Word canonical, List<Word> duplicates) {
        for (VocabularySet set : vocabularySetRepository.findAll()) {
            if (set.getWords() == null) {
                continue;
            }
            boolean changed = false;
            for (Word duplicate : duplicates) {
                changed |= set.getWords().remove(duplicate);
            }
            if (changed) {
                set.getWords().add(canonical);
                vocabularySetRepository.save(set);
            }
        }
    }

    private void mergeUserProgress(Word canonical, List<Word> group) {
        Map<Long, UserProgress> retainedByUser = new LinkedHashMap<>();
        for (UserProgress progress : userProgressRepository.findAllByWordIn(group)) {
            Long userId = progress.getUser().getId();
            UserProgress retained = retainedByUser.get(userId);
            if (retained == null) {
                progress.setWord(canonical);
                retainedByUser.put(userId, progress);
                continue;
            }
            mergeProgress(retained, progress);
            userProgressRepository.delete(progress);
        }
        userProgressRepository.saveAll(retainedByUser.values());
    }

    private void reassignDependentRows(Word canonical, List<Word> duplicates) {
        for (Word duplicate : duplicates) {
            for (ReviewEvent reviewEvent : reviewEventRepository.findByWord(duplicate)) {
                reviewEvent.setWord(canonical);
                reviewEventRepository.save(reviewEvent);
            }
            for (WordOfTheDay wordOfTheDay : wordOfTheDayRepository.findByWord(duplicate)) {
                wordOfTheDay.setWord(canonical);
                wordOfTheDayRepository.save(wordOfTheDay);
            }
            for (WordEnrichmentJob job : wordEnrichmentJobRepository.findByWord(duplicate)) {
                job.setWord(canonical);
                wordEnrichmentJobRepository.save(job);
            }
        }
    }

    private void mergeProgress(UserProgress retained, UserProgress duplicate) {
        retained.setCorrectCount(retained.getCorrectCount() + duplicate.getCorrectCount());
        retained.setIncorrectCount(retained.getIncorrectCount() + duplicate.getIncorrectCount());
        retained.setMastery(max(retained.getMastery(), duplicate.getMastery()));
        retained.setLastStudied(latest(retained.getLastStudied(), duplicate.getLastStudied()));
        retained.setNextReview(earliest(retained.getNextReview(), duplicate.getNextReview()));
        retained.setFsrsStability(Math.max(retained.getFsrsStability(), duplicate.getFsrsStability()));
        retained.setFsrsDifficulty(Math.max(retained.getFsrsDifficulty(), duplicate.getFsrsDifficulty()));
        retained.setFsrsRepetition(Math.max(retained.getFsrsRepetition(), duplicate.getFsrsRepetition()));
        retained.setFsrsLapseCount(retained.getFsrsLapseCount() + duplicate.getFsrsLapseCount());
        retained.setFsrsRetrievability(Math.max(retained.getFsrsRetrievability(), duplicate.getFsrsRetrievability()));
        retained.setLastIntervalHours(Math.max(retained.getLastIntervalHours(), duplicate.getLastIntervalHours()));
        retained.setSequenceAccuracyEMA(Math.max(retained.getSequenceAccuracyEMA(), duplicate.getSequenceAccuracyEMA()));
        retained.setSequenceResponseMsEMA(Math.max(retained.getSequenceResponseMsEMA(), duplicate.getSequenceResponseMsEMA()));
        retained.setSequenceConsistency(Math.max(retained.getSequenceConsistency(), duplicate.getSequenceConsistency()));
        retained.setSequenceDifficultyTrend(Math.max(retained.getSequenceDifficultyTrend(), duplicate.getSequenceDifficultyTrend()));
        retained.setSequenceStep(Math.max(retained.getSequenceStep(), duplicate.getSequenceStep()));
    }

    private void ensureUniqueWordIndex() throws SQLException {
        if (hasUniqueWordIndex()) {
            return;
        }
        jdbcTemplate.execute("CREATE UNIQUE INDEX " + UNIQUE_INDEX_NAME + " ON words (word)");
    }

    private boolean hasUniqueWordIndex() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            return hasUniqueWordIndex(metadata, "words") || hasUniqueWordIndex(metadata, "WORDS");
        }
    }

    private boolean hasUniqueWordIndex(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet indexes = metadata.getIndexInfo(null, null, tableName, true, false)) {
            while (indexes.next()) {
                if (UNIQUE_INDEX_NAME.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalize(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String current, String candidate) {
        return current == null || current.isBlank() ? candidate : current;
    }

    private UserProgress.MasteryLevel max(UserProgress.MasteryLevel left, UserProgress.MasteryLevel right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    private LocalDateTime latest(LocalDateTime left, LocalDateTime right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isAfter(right) ? left : right;
    }

    private LocalDateTime earliest(LocalDateTime left, LocalDateTime right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isBefore(right) ? left : right;
    }
}

package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.CefrLevel;
import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, Long>, JpaSpecificationExecutor<Word> {

    @Override
    @EntityGraph(attributePaths = "topic")
    Page<Word> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "topic")
    Page<Word> findAll(Specification<Word> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "topic")
    Optional<Word> findById(Long id);

    @EntityGraph(attributePaths = "topic")
    List<Word> findByCategory(String category);

    @EntityGraph(attributePaths = "topic")
    List<Word> findByDifficulty(Word.DifficultyLevel difficulty);

    @EntityGraph(attributePaths = "topic")
    List<Word> findByCategoryAndDifficulty(String category, Word.DifficultyLevel difficulty);

    @EntityGraph(attributePaths = "topic")
    List<Word> findByTopicId(Long topicId);

    @EntityGraph(attributePaths = "topic")
    List<Word> findByCefrLevel(CefrLevel cefrLevel);

    @EntityGraph(attributePaths = "topic")
    List<Word> findByPartOfSpeechIgnoreCase(String partOfSpeech);

    @EntityGraph(attributePaths = "topic")
    List<Word> findByTopicIdAndCefrLevel(Long topicId, CefrLevel cefrLevel);

    @Query("SELECT DISTINCT w.category FROM Word w WHERE w.category IS NOT NULL AND w.category <> ''")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT w.partOfSpeech FROM Word w WHERE w.partOfSpeech IS NOT NULL AND w.partOfSpeech <> ''")
    List<String> findAllPartsOfSpeech();

    @Query("SELECT w FROM Word w LEFT JOIN FETCH w.topic ORDER BY FUNCTION('RAND')")
    List<Word> findRandomWords(Pageable pageable);

    @Query("SELECT DISTINCT w FROM Word w LEFT JOIN FETCH w.topic WHERE LOWER(w.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(w.translation) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Word> searchByKeyword(String keyword);

    @Query("SELECT w FROM Word w WHERE NOT EXISTS (SELECT up.id FROM UserProgress up WHERE up.word = w AND up.user = :user) ORDER BY w.createdAt ASC")
    List<Word> findWordsWithoutProgressForUser(AppUser user);

    Optional<Word> findByWordIgnoreCase(String word);

    boolean existsByWordIgnoreCase(String word);

    @Query("SELECT w FROM Word w WHERE w.translation IS NULL OR w.translation = '' OR w.audioUrl IS NULL OR w.audioUrl = '' OR w.pronunciation IS NULL OR w.pronunciation = '' OR w.synonyms IS NULL OR w.synonyms = '' OR w.partOfSpeech IS NULL OR w.partOfSpeech = '' OR w.imageUrl IS NULL OR w.imageUrl = ''")
    List<Word> findWordsNeedingEnrichment();
}

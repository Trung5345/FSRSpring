package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.CefrLevel;
import com.fsrspring.vocab.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByCategory(String category);

    List<Word> findByDifficulty(Word.DifficultyLevel difficulty);

    List<Word> findByCategoryAndDifficulty(String category, Word.DifficultyLevel difficulty);

    List<Word> findByTopicId(Long topicId);

    List<Word> findByCefrLevel(CefrLevel cefrLevel);

    List<Word> findByPartOfSpeechIgnoreCase(String partOfSpeech);

    List<Word> findByTopicIdAndCefrLevel(Long topicId, CefrLevel cefrLevel);

    @Query("SELECT DISTINCT w.category FROM Word w WHERE w.category IS NOT NULL AND w.category <> ''")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT w.partOfSpeech FROM Word w WHERE w.partOfSpeech IS NOT NULL AND w.partOfSpeech <> ''")
    List<String> findAllPartsOfSpeech();

    @Query("SELECT w FROM Word w ORDER BY RANDOM() LIMIT :limit")
    List<Word> findRandomWords(int limit);

    @Query("SELECT w FROM Word w WHERE LOWER(w.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(w.translation) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Word> searchByKeyword(String keyword);

    boolean existsByWordIgnoreCase(String word);
}

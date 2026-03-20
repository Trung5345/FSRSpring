package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByWord(Word word);

    Optional<UserProgress> findByWordId(Long wordId);

    List<UserProgress> findByMastery(UserProgress.MasteryLevel mastery);

    @Query("SELECT up FROM UserProgress up WHERE up.nextReview <= :now OR up.nextReview IS NULL ORDER BY up.nextReview ASC NULLS FIRST")
    List<UserProgress> findWordsForReview(LocalDateTime now);

    @Query("SELECT up FROM UserProgress up WHERE up.nextReview <= :now ORDER BY up.nextReview ASC")
    List<UserProgress> findDueWords(LocalDateTime now);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.nextReview <= :now")
    long countDueWords(LocalDateTime now);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.mastery = 'MASTERED'")
    long countMastered();

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.mastery = 'LEARNING'")
    long countLearning();

    @Query("SELECT SUM(up.correctCount) FROM UserProgress up")
    Long sumCorrectAnswers();

    @Query("SELECT SUM(up.incorrectCount) FROM UserProgress up")
    Long sumIncorrectAnswers();
}

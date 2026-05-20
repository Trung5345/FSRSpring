package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"word"})
    Optional<UserProgress> findByWord(Word word);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"word"})
    Optional<UserProgress> findByUserAndWord(AppUser user, Word word);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"word"})
    Optional<UserProgress> findByWordId(Long wordId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"word"})
    Optional<UserProgress> findByUserAndWordId(AppUser user, Long wordId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"word"})
    List<UserProgress> findByUser(AppUser user);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"word", "user"})
    List<UserProgress> findAllByWordIn(Collection<Word> words);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"word"})
    List<UserProgress> findByMastery(UserProgress.MasteryLevel mastery);

    @Query("SELECT up FROM UserProgress up JOIN FETCH up.word WHERE up.user = :user AND (up.nextReview <= :now OR up.nextReview IS NULL) ORDER BY up.nextReview ASC NULLS FIRST")
    List<UserProgress> findWordsForReview(AppUser user, LocalDateTime now);

    @Query("SELECT up FROM UserProgress up JOIN FETCH up.word WHERE up.user = :user AND (up.nextReview <= :now OR up.nextReview IS NULL) ORDER BY up.nextReview ASC NULLS FIRST")
    List<UserProgress> findDueWords(AppUser user, LocalDateTime now);

    @Query("SELECT up FROM UserProgress up JOIN FETCH up.user JOIN FETCH up.word WHERE up.nextReview <= :now OR up.nextReview IS NULL ORDER BY up.user.id ASC, up.nextReview ASC NULLS FIRST")
    List<UserProgress> findDueWordsForAllUsers(LocalDateTime now);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user = :user AND (up.nextReview <= :now OR up.nextReview IS NULL)")
    long countDueWords(AppUser user, LocalDateTime now);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user = :user AND up.mastery = 'MASTERED'")
    long countMastered(AppUser user);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user = :user AND up.mastery = 'LEARNING'")
    long countLearning(AppUser user);

    @Query("SELECT SUM(up.correctCount) FROM UserProgress up WHERE up.user = :user")
    Long sumCorrectAnswers(AppUser user);

    @Query("SELECT SUM(up.incorrectCount) FROM UserProgress up WHERE up.user = :user")
    Long sumIncorrectAnswers(AppUser user);
}

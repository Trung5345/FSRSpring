package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgressService {

    private final UserProgressRepository progressRepository;
    private final FsrsService fsrsService;

    public UserProgress getOrCreateProgress(Word word) {
        return fsrsService.getOrCreateProgress(word);
    }

    public UserProgress recordAnswer(Word word, boolean correct) {
        int rating = correct ? 3 : 1;
        return fsrsService.reviewWord(word, rating, 0L);
    }

    public List<UserProgress> getWordsForReview() {
        return progressRepository.findWordsForReview(LocalDateTime.now());
    }

    public List<UserProgress> getAllProgress() {
        return progressRepository.findAll();
    }

    public UserProgress getProgressByWordId(Long wordId) {
        return progressRepository.findByWordId(wordId).orElse(null);
    }

    public long countMastered() {
        return progressRepository.countMastered();
    }

    public long countLearning() {
        return progressRepository.countLearning();
    }

    public long totalCorrect() {
        Long val = progressRepository.sumCorrectAnswers();
        return val != null ? val : 0L;
    }

    public long totalIncorrect() {
        Long val = progressRepository.sumIncorrectAnswers();
        return val != null ? val : 0L;
    }
}

package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.model.WordOfTheDay;
import com.fsrspring.vocab.repository.WordOfTheDayRepository;
import com.fsrspring.vocab.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class WordOfTheDayService {

    private final WordOfTheDayRepository wotdRepository;
    private final WordRepository wordRepository;
    private final Random random = new Random();

    /**
     * Returns today's word of the day, creating one if it doesn't exist yet.
     */
    @Transactional
    public WordOfTheDay getTodaysWord() {
        LocalDate today = LocalDate.now();
        Optional<WordOfTheDay> existing = wotdRepository.findByDate(today);
        if (existing.isPresent()) {
            return existing.get();
        }
        return pickNewWordForDate(today);
    }

    /**
     * Scheduled daily at midnight to pre-populate next day's WotD.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void pickDailyWord() {
        LocalDate today = LocalDate.now();
        if (wotdRepository.findByDate(today).isEmpty()) {
            pickNewWordForDate(today);
            log.info("Picked new Word of the Day for {}", today);
        }
    }

    private WordOfTheDay pickNewWordForDate(LocalDate date) {
        List<Word> allWords = wordRepository.findAll();
        if (allWords.isEmpty()) {
            throw new IllegalStateException("No words available to pick as Word of the Day");
        }
        Word chosen = allWords.get(random.nextInt(allWords.size()));
        WordOfTheDay wotd = WordOfTheDay.builder()
                .word(chosen)
                .date(date)
                .build();
        return wotdRepository.save(wotd);
    }
}

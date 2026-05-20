package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.WordOfTheDay;
import com.fsrspring.vocab.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WordOfTheDayRepository extends JpaRepository<WordOfTheDay, Long> {
    Optional<WordOfTheDay> findByDate(LocalDate date);

    List<WordOfTheDay> findByWord(Word word);
}

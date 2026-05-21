package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByDeckId(Long deckId);

    List<Card> findByWordContainingIgnoreCase(String keyword);

    List<Card> findByLevel(String level);

    List<Card> findByCefrLevel(String cefrLevel);
}
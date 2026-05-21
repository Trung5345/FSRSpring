package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.Card;
import com.fsrspring.vocab.model.Deck;
import com.fsrspring.vocab.model.CardReviewState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class FsrsRepositoryTests {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardReviewStateRepository cardReviewStateRepository;

    private AppUser testUser;
    private Deck testDeck;
    private Card testCard1;
    private Card testCard2;

    @BeforeEach
    public void setup() {
        testUser = AppUser.builder()
                .email("tester@fsrspring.com")
                .name("Tester")
                .role(AppUser.Role.USER)
                .build();
        appUserRepository.save(testUser);

        testDeck = Deck.builder()
                .name("English Basics")
                .description("Basic words")
                .isPublic(true)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        deckRepository.save(testDeck);

        testCard1 = Card.builder()
                .word("Apple")
                .meaning("Quả táo")
                .level("A1")
                .cefrLevel("A1")
                .deck(testDeck)
                .createdAt(LocalDateTime.now())
                .build();
        cardRepository.save(testCard1);

        testCard2 = Card.builder()
                .word("Algorithm")
                .meaning("Thuật toán")
                .level("B2")
                .cefrLevel("B2")
                .deck(testDeck)
                .createdAt(LocalDateTime.now())
                .build();
        cardRepository.save(testCard2);
    }

    @Test
    public void testCardRepository_findByDeckId() {
        List<Card> cards = cardRepository.findByDeckId(testDeck.getId());
        assertThat(cards).hasSize(2);
        assertThat(cards).extracting(Card::getWord).contains("Apple", "Algorithm");
    }

    @Test
    public void testCardRepository_findByWordContainingIgnoreCase() {
        List<Card> cards = cardRepository.findByWordContainingIgnoreCase("algo");
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getWord()).isEqualTo("Algorithm");
    }

    @Test
    public void testCardReviewStateRepository_dueAtQuery() {
        LocalDateTime now = LocalDateTime.now();
        
        CardReviewState state1 = CardReviewState.builder()
                .user(testUser)
                .card(testCard1)
                .dueAt(now.minusDays(1)) // Due yesterday (PAST DUE)
                .intervalDays(1)
                .build();
        cardReviewStateRepository.save(state1);

        CardReviewState state2 = CardReviewState.builder()
                .user(testUser)
                .card(testCard2)
                .dueAt(now.plusDays(1)) // Due tomorrow (NOT DUE YET)
                .intervalDays(5)
                .build();
        cardReviewStateRepository.save(state2);

        // Test count
        long dueCount = cardReviewStateRepository.countByUserIdAndDueAtLessThanEqual(testUser.getId(), now);
        assertThat(dueCount).isEqualTo(1L);

        // Test fetching ordered items
        List<CardReviewState> dueItems = cardReviewStateRepository
                .findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(testUser.getId(), now, PageRequest.of(0, 10));
        assertThat(dueItems).hasSize(1);
        assertThat(dueItems.get(0).getCard().getWord()).isEqualTo("Apple");
    }
}
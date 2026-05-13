package com.linguist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String word;

    private String pronunciation;

    @Column(nullable = false)
    private String meaning;

    @Column(name = "example_sentence")
    private String exampleSentence;

    private String level;

    @Column(name = "cefr_level")
    private String cefrLevel;

    @Column(name = "part_of_speech")
    private String partOfSpeech;

    @ManyToOne
    @JoinColumn(name = "deck_id")
    private Deck deck;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
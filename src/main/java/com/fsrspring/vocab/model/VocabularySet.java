package com.fsrspring.vocab.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vocabulary_set")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularySet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Enumerated(EnumType.STRING)
    private CefrLevel cefrLevel;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "vocabulary_set_word",
        joinColumns = @JoinColumn(name = "set_id"),
        inverseJoinColumns = @JoinColumn(name = "word_id")
    )
    @JsonIgnore
    private Set<Word> words = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    public int getWordCount() {
        return words != null ? words.size() : 0;
    }
}

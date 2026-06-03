package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.model.Topic;
import com.fsrspring.vocab.repository.TopicRepository;
import com.fsrspring.vocab.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class WordServiceTests {

    @Autowired
    private WordService wordService;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        wordRepository.deleteAll();
        topicRepository.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void createWordTrimsInputAndRejectsCaseInsensitiveDuplicates() {
        Word created = wordService.createWord(Word.builder()
                .word(" tiger ")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .build());

        assertThat(created.getWord()).isEqualTo("tiger");
        assertThatThrownBy(() -> wordService.createWord(Word.builder()
                .word("TIGER")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Word already exists: TIGER");
        assertThat(wordRepository.findAll()).hasSize(1);
    }

    @Test
    void updateWordRejectsRenamingToExistingWord() {
        Word tiger = wordRepository.save(Word.builder()
                .word("tiger")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .build());
        Word lion = wordRepository.save(Word.builder()
                .word("lion")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .build());

        assertThatThrownBy(() -> wordService.updateWord(lion.getId(), Word.builder()
                .word(" tiger ")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Word already exists: tiger");
        assertThat(wordRepository.findById(tiger.getId())).isPresent();
        assertThat(wordRepository.findById(lion.getId()).orElseThrow().getWord()).isEqualTo("lion");
    }

    @Test
    void getRandomWordsLoadsTopicOutsideRepositorySession() {
        Topic topic = topicRepository.save(Topic.builder()
                .name("Food")
                .slug("food")
                .build());
        wordRepository.save(Word.builder()
                .word("apple")
                .translation("táo")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .topic(topic)
                .build());

        Word randomWord = wordService.getRandomWords(1).get(0);

        assertThat(randomWord.getTopic().getName()).isEqualTo("Food");
    }
}

package com.fsrspring.vocab.startup;

import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WordDeduplicationRunnerTests {

    @Autowired
    private WordDeduplicationRunner runner;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("DROP INDEX IF EXISTS uk_words_word");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        wordRepository.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void startupMergeKeepsOneWordAndPreservesRicherData() throws Exception {
        wordRepository.save(Word.builder()
                .word("tiger")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .build());
        wordRepository.save(Word.builder()
                .word(" TIGER ")
                .translation("con ho")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .build());

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(wordRepository.findAll()).hasSize(1);
        assertThat(wordRepository.findByWordIgnoreCase("tiger").orElseThrow().getTranslation()).isEqualTo("con ho");
    }
}

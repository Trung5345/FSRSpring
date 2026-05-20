package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.WordRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class WordService {

    private final WordRepository wordRepository;
    private final WordEnrichmentService wordEnrichmentService;

    public List<Word> getAllWords() {
        return wordRepository.findAll();
    }

    public Page<Word> getWordsPage(
            String category,
            Word.DifficultyLevel difficulty,
            String search,
            Long topicId,
            com.fsrspring.vocab.model.CefrLevel cefrLevel,
            String partOfSpeech,
            int offset,
            int size) {
        int safeOffset = Math.max(0, offset);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageRequest = new OffsetLimitPageable(safeOffset, safeSize, Sort.by("id").ascending());
        return wordRepository.findAll(wordSpecification(category, difficulty, search, topicId, cefrLevel, partOfSpeech), pageRequest);
    }

    private record OffsetLimitPageable(int offset, int pageSize, Sort sort) implements Pageable {
        private OffsetLimitPageable {
            if (offset < 0) {
                throw new IllegalArgumentException("Offset must not be negative");
            }
            if (pageSize < 1) {
                throw new IllegalArgumentException("Page size must be greater than zero");
            }
            if (sort == null) {
                sort = Sort.unsorted();
            }
        }

        @Override
        public int getPageNumber() {
            return offset / pageSize;
        }

        @Override
        public int getPageSize() {
            return pageSize;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        @Override
        public Sort getSort() {
            return sort;
        }

        @Override
        public Pageable next() {
            return new OffsetLimitPageable(offset + pageSize, pageSize, sort);
        }

        @Override
        public Pageable previousOrFirst() {
            if (!hasPrevious()) return first();
            return new OffsetLimitPageable(Math.max(offset - pageSize, 0), pageSize, sort);
        }

        @Override
        public Pageable first() {
            return new OffsetLimitPageable(0, pageSize, sort);
        }

        @Override
        public Pageable withPage(int pageNumber) {
            if (pageNumber < 0) {
                throw new IllegalArgumentException("Page index must not be negative");
            }
            return new OffsetLimitPageable(pageNumber * pageSize, pageSize, sort);
        }

        @Override
        public boolean hasPrevious() {
            return offset > 0;
        }
    }

    private Specification<Word> wordSpecification(
            String category,
            Word.DifficultyLevel difficulty,
            String search,
            Long topicId,
            com.fsrspring.vocab.model.CefrLevel cefrLevel,
            String partOfSpeech) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }
            if (topicId != null) {
                predicates.add(cb.equal(root.get("topic").get("id"), topicId));
            }
            if (cefrLevel != null) {
                predicates.add(cb.equal(root.get("cefrLevel"), cefrLevel));
            }
            if (partOfSpeech != null && !partOfSpeech.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.<String>get("partOfSpeech")), partOfSpeech.toLowerCase()));
            }
            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("word")), keyword),
                        cb.like(cb.lower(root.<String>get("translation")), keyword)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public Word getWordById(Long id) {
        return wordRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Word not found with id: " + id));
    }

    public Word getWordByText(String word) {
        return wordRepository.findByWordIgnoreCase(normalizeWord(word))
                .orElseThrow(() -> new NoSuchElementException("Word not found: " + word));
    }

    public Word getOrCreateWord(Word word) {
        String normalizedWord = normalizeWord(word.getWord());
        word.setWord(normalizedWord);
        return wordRepository.findByWordIgnoreCase(normalizedWord)
                .orElseGet(() -> createWord(word));
    }

    public List<Word> getWordsByCategory(String category) {
        return wordRepository.findByCategory(category);
    }

    public List<Word> getWordsByDifficulty(Word.DifficultyLevel difficulty) {
        return wordRepository.findByDifficulty(difficulty);
    }

    public List<Word> getWordsByCategoryAndDifficulty(String category, Word.DifficultyLevel difficulty) {
        return wordRepository.findByCategoryAndDifficulty(category, difficulty);
    }

    public List<String> getAllCategories() {
        return wordRepository.findAllCategories();
    }

    public List<Word> getRandomWords(int limit) {
        return wordRepository.findRandomWords(limit);
    }

    public List<Word> searchWords(String keyword) {
        return wordRepository.searchByKeyword(keyword);
    }

    public Word createWord(Word word) {
        String normalizedWord = normalizeWord(word.getWord());
        word.setWord(normalizedWord);
        if (wordRepository.existsByWordIgnoreCase(normalizedWord)) {
            throw new IllegalArgumentException("Word already exists: " + normalizedWord);
        }
        Word saved;
        try {
            saved = wordRepository.saveAndFlush(word);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Word already exists: " + normalizedWord);
        }
        wordEnrichmentService.enqueueWord(saved.getId());
        return saved;
    }

    public List<Word> getWordsByTopic(Long topicId) {
        return wordRepository.findByTopicId(topicId);
    }

    public List<Word> getWordsByCefrLevel(com.fsrspring.vocab.model.CefrLevel cefrLevel) {
        return wordRepository.findByCefrLevel(cefrLevel);
    }

    public List<Word> getWordsByPartOfSpeech(String partOfSpeech) {
        return wordRepository.findByPartOfSpeechIgnoreCase(partOfSpeech);
    }

    public List<Word> getWordsByTopicAndCefr(Long topicId, com.fsrspring.vocab.model.CefrLevel cefrLevel) {
        return wordRepository.findByTopicIdAndCefrLevel(topicId, cefrLevel);
    }

    public List<String> getAllPartsOfSpeech() {
        return wordRepository.findAllPartsOfSpeech();
    }

    public Word updateWord(Long id, Word updatedWord) {
        Word existing = getWordById(id);
        String normalizedWord = normalizeWord(updatedWord.getWord());
        wordRepository.findByWordIgnoreCase(normalizedWord)
                .filter(word -> !word.getId().equals(id))
                .ifPresent(word -> {
                    throw new IllegalArgumentException("Word already exists: " + normalizedWord);
                });
        existing.setWord(normalizedWord);
        existing.setTranslation(updatedWord.getTranslation());
        existing.setExample(updatedWord.getExample());
        existing.setPronunciation(updatedWord.getPronunciation());
        existing.setCategory(updatedWord.getCategory());
        existing.setDifficulty(updatedWord.getDifficulty());
        existing.setImageUrl(updatedWord.getImageUrl());
        existing.setTopic(updatedWord.getTopic());
        existing.setCefrLevel(updatedWord.getCefrLevel());
        existing.setPartOfSpeech(updatedWord.getPartOfSpeech());
        existing.setAudioUrl(updatedWord.getAudioUrl());
        existing.setSynonyms(updatedWord.getSynonyms());
        existing.setAntonyms(updatedWord.getAntonyms());
        existing.setOrigin(updatedWord.getOrigin());
        return wordRepository.save(existing);
    }

    public void deleteWord(Long id) {
        if (!wordRepository.existsById(id)) {
            throw new NoSuchElementException("Word not found with id: " + id);
        }
        wordRepository.deleteById(id);
    }

    public long countWords() {
        return wordRepository.count();
    }

    private String normalizeWord(String word) {
        if (word == null) {
            return null;
        }
        return word.trim();
    }
}

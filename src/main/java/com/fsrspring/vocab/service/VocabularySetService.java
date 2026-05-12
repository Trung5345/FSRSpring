package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.CefrLevel;
import com.fsrspring.vocab.model.Topic;
import com.fsrspring.vocab.model.VocabularySet;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.TopicRepository;
import com.fsrspring.vocab.repository.VocabularySetRepository;
import com.fsrspring.vocab.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VocabularySetService {

    private final VocabularySetRepository setRepository;
    private final TopicRepository topicRepository;
    private final WordRepository wordRepository;

    public List<VocabularySet> getAll() {
        return setRepository.findAll();
    }

    public VocabularySet getById(Long id) {
        return setRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Set not found: " + id));
    }

    public List<VocabularySet> getByTopic(Long topicId) {
        return setRepository.findByTopicId(topicId);
    }

    public List<VocabularySet> getByCefrLevel(CefrLevel level) {
        return setRepository.findByCefrLevel(level);
    }

    @Transactional
    public VocabularySet createSet(String name, String description, Long topicId, CefrLevel cefrLevel) {
        Topic topic = topicId != null ? topicRepository.findById(topicId).orElse(null) : null;
        VocabularySet set = VocabularySet.builder()
                .name(name)
                .description(description)
                .topic(topic)
                .cefrLevel(cefrLevel)
                .build();
        return setRepository.save(set);
    }

    @Transactional
    public VocabularySet addWord(Long setId, Long wordId) {
        VocabularySet set = getById(setId);
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found: " + wordId));
        set.getWords().add(word);
        return setRepository.save(set);
    }

    @Transactional
    public VocabularySet removeWord(Long setId, Long wordId) {
        VocabularySet set = getById(setId);
        set.getWords().removeIf(w -> w.getId().equals(wordId));
        return setRepository.save(set);
    }

    @Transactional
    public void deleteSet(Long id) {
        setRepository.deleteById(id);
    }

    public List<Word> getWordsInSet(Long setId) {
        return new java.util.ArrayList<>(getById(setId).getWords());
    }
}

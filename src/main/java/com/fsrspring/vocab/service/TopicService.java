package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.Topic;
import com.fsrspring.vocab.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public List<Topic> getAll() {
        return topicRepository.findAll();
    }

    public Optional<Topic> getBySlug(String slug) {
        return topicRepository.findBySlug(slug);
    }

    public Topic getById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + id));
    }
}

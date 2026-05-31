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

    public Topic create(Topic topic) {
        if (topic.getSlug() == null || topic.getSlug().isBlank()) {
            topic.setSlug(topic.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", ""));
        }
        return topicRepository.save(topic);
    }

    public Topic update(Long id, Topic patch) {
        Topic existing = getById(id);
        if (patch.getName() != null && !patch.getName().isBlank()) existing.setName(patch.getName());
        if (patch.getSlug() != null && !patch.getSlug().isBlank()) existing.setSlug(patch.getSlug());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getIconEmoji() != null) existing.setIconEmoji(patch.getIconEmoji());
        if (patch.getColorHex() != null) existing.setColorHex(patch.getColorHex());
        return topicRepository.save(existing);
    }

    public void delete(Long id) {
        topicRepository.deleteById(id);
    }
}

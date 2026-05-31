package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    Optional<UserStreak> findByUser(AppUser user);
}

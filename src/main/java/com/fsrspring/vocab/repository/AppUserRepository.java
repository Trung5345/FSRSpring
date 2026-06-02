package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM AppUser u WHERE u.reviewRemindersEnabled = true OR u.reviewRemindersEnabled IS NULL")
    List<AppUser> findUsersWithRemindersEnabled();

    @Query("""
        SELECT u FROM AppUser u
        WHERE (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
          AND (:locked IS NULL OR u.locked = :locked)
          AND (:role IS NULL OR u.role = :role)
          AND (:from IS NULL OR u.createdAt >= :from)
          AND (:to IS NULL OR u.createdAt <= :to)
        ORDER BY u.createdAt DESC
        """)
    Page<AppUser> searchUsers(
            @Param("email") String email,
            @Param("locked") Boolean locked,
            @Param("role") AppUser.Role role,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}

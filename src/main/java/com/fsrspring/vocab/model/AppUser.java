package com.fsrspring.vocab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "app_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String name;

    @Column
    private String avatarUrl;

    @Column(unique = true)
    private String googleId;

    public enum Role {
        USER, ADMIN
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    @Builder.Default
    private LocalDateTime lastLoginAt = LocalDateTime.now();

    @Column
    @Builder.Default
    private Boolean reviewRemindersEnabled = true;

    @Column
    @Builder.Default
    private LocalTime preferredReminderTime = LocalTime.of(20, 0);

    @Column
    @Builder.Default
    private Boolean eveningReminderEnabled = true;

    @Column
    @Builder.Default
    private LocalTime eveningReminderTime = LocalTime.of(21, 30);

    @Column
    @Builder.Default
    private Boolean comebackReminderEnabled = true;

    @Column
    @Builder.Default
    private Integer comebackReminderIntervalDays = 3;

    public Boolean getReviewRemindersEnabled() {
        return reviewRemindersEnabled != null ? reviewRemindersEnabled : true;
    }

    public LocalTime getPreferredReminderTime() {
        return preferredReminderTime != null ? preferredReminderTime : LocalTime.of(20, 0);
    }

    public Boolean getEveningReminderEnabled() {
        return eveningReminderEnabled != null ? eveningReminderEnabled : true;
    }

    public LocalTime getEveningReminderTime() {
        return eveningReminderTime != null ? eveningReminderTime : LocalTime.of(21, 30);
    }

    public Boolean getComebackReminderEnabled() {
        return comebackReminderEnabled != null ? comebackReminderEnabled : true;
    }

    public Integer getComebackReminderIntervalDays() {
        return comebackReminderIntervalDays != null ? comebackReminderIntervalDays : 3;
    }
}

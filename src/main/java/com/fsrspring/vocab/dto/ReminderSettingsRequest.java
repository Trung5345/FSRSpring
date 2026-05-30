package com.fsrspring.vocab.dto;

public record ReminderSettingsRequest(
        Boolean reviewRemindersEnabled,
        String preferredReminderTime,
        Boolean eveningReminderEnabled,
        String eveningReminderTime,
        Boolean comebackReminderEnabled,
        Integer comebackReminderIntervalDays,
        Integer eveningRemainingThreshold
) {
}

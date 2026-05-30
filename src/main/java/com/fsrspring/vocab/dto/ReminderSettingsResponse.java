package com.fsrspring.vocab.dto;

public record ReminderSettingsResponse(
        boolean reviewRemindersEnabled,
        String preferredReminderTime,
        boolean eveningReminderEnabled,
        String eveningReminderTime,
        boolean comebackReminderEnabled,
        int comebackReminderIntervalDays,
        int eveningRemainingThreshold
) {
}

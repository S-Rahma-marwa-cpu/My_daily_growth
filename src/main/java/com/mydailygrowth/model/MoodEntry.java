package com.mydailygrowth.model;

import java.time.LocalDate;

public record MoodEntry(LocalDate date, String mood, String note) { }

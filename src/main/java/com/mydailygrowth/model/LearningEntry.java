package com.mydailygrowth.model;

import java.time.LocalDate;

public record LearningEntry(int id, LocalDate date, String title, String details) { }

package com.example.brightpath.dto;

import com.example.brightpath.enums.ConflictType;
import java.time.LocalDate;
import java.util.List;

public record ConflictReport(
        ConflictType type,
        LocalDate date,
        String tutorId,
        String roomId,
        List<String> lessonIds,
        String message
) {}
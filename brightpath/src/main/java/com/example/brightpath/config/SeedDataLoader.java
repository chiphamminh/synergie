package com.example.brightpath.config;

import com.example.brightpath.entity.Booking;
import com.example.brightpath.entity.Tutor;
import com.example.brightpath.enums.BookingStatus;
import com.example.brightpath.enums.BookingType;
import com.example.brightpath.repository.BookingRepository;
import com.example.brightpath.repository.TutorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Component
public class SeedDataLoader implements CommandLineRunner {
    private final TutorRepository tutors;
    private final BookingRepository bookings;

    public SeedDataLoader(TutorRepository tutors, BookingRepository bookings) {
        this.tutors = tutors;
        this.bookings = bookings;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (tutors.count() == 0) loadTutors();
        if (bookings.count() == 0) loadBookings();
    }

    private void loadTutors() throws Exception {
        for (String[] row : rows("seed/tutors.csv")) {
            tutors.save(new Tutor(row[0], row[1], row[2], row[3]));
        }
    }

    private void loadBookings() throws Exception {
        for (String[] row : rows("seed/lessons_export.csv")) {
            Tutor tutor = tutors.findById(row[5])
                    .orElseThrow(() -> new IllegalStateException("Missing tutor in seed: " + row[5]));
            BookingStatus status = BookingStatus.valueOf(row[7].toUpperCase(Locale.ROOT));
            BookingType type = row[9] != null && row[9].toLowerCase(Locale.ROOT).contains("exam pair")
                    ? BookingType.PAIRED : BookingType.SINGLE;
            LocalDateTime cancelledAt = row[8].isBlank() ? null : OffsetDateTime.parse(row[8]).toLocalDateTime();
            bookings.save(new Booking(row[0], LocalDate.parse(row[1]), LocalTime.parse(row[2]),
                    Integer.valueOf(row[3]), row[4], tutor, row[6], status, type, cancelledAt,
                    row[9].isBlank() ? null : row[9]));
        }
    }

    private List<String[]> rows(String path) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> result = new ArrayList<>();
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                if (!line.isBlank()) result.add(line.split(",", -1));
            }
            return result;
        }
    }
}

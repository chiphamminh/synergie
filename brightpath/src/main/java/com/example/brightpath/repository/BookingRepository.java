package com.example.brightpath.repository;

import com.example.brightpath.entity.Booking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByLessonId(String lessonId);
    List<Booking> findByDateOrderByStartTime(LocalDate date);
    List<Booking> findByDateAndTutorId(LocalDate date, String tutorId);
}

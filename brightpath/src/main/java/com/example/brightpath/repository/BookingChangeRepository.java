package com.example.brightpath.repository;

import com.example.brightpath.entity.BookingChange;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingChangeRepository extends JpaRepository<BookingChange, Long> {
    List<BookingChange> findByBookingIdOrderByChangedAt(Long bookingId);
}

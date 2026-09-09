package com.example.synergie.controller;

import com.example.synergie.dto.BookingRequest;
import com.example.synergie.dto.BookingUpdateRequest;
import com.example.synergie.entity.Booking;
import com.example.synergie.entity.BookingChange;
import com.example.synergie.service.BookingService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;
    public BookingController(BookingService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody BookingRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}")
    public Booking update(@PathVariable Long id, @RequestBody BookingUpdateRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    public Booking cancel(@PathVariable Long id) { return service.cancel(id); }

    @GetMapping
    public List<Booking> byDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.byDate(date);
    }

    @GetMapping("/{id}/changes")
    public List<BookingChange> changes(@PathVariable Long id) { return service.changes(id); }
}

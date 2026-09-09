package com.example.brightpath.service;

import com.example.brightpath.dto.BookingRequest;
import com.example.brightpath.dto.BookingUpdateRequest;
import com.example.brightpath.entity.Booking;
import com.example.brightpath.entity.BookingChange;
import com.example.brightpath.entity.Tutor;
import com.example.brightpath.enums.BookingStatus;
import com.example.brightpath.enums.BookingType;
import com.example.brightpath.enums.ChangeType;
import com.example.brightpath.exception.ApiException;
import com.example.brightpath.repository.BookingChangeRepository;
import com.example.brightpath.repository.BookingRepository;
import com.example.brightpath.repository.TutorRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final TutorRepository tutors;
    private final BookingChangeRepository changes;
    private final Clock clock;

    public BookingService(BookingRepository bookings, TutorRepository tutors,
                           BookingChangeRepository changes,
                           @Value("${app.fixed-now:2026-03-06T16:30:00+07:00}") String fixedNow) {
        this.bookings = bookings; this.tutors = tutors; this.changes = changes;
        this.clock = Clock.fixed(OffsetDateTime.parse(fixedNow).toInstant(), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Transactional
    public Booking create(BookingRequest request) {
        validateInput(request.getDate(), request.getStartTime(), request.getDurationMin(), request.getTutorId(), request.getRoomId());

        Tutor tutor = tutor(request.getTutorId());
        List<Booking> sameDay = bookings.findByDateAndTutorId(request.getDate(), tutor.getId());

        checkConflicts(request.getDate(), request.getStartTime(), request.getDurationMin(), request.getRoomId(),
                request.bookingTypeOrSingle(), sameDay, null);
        if (sameDay.stream().filter(this::usesCapacity).count() >= 6 && !Boolean.TRUE.equals(request.getOverride())) {
            throw new ApiException(HttpStatus.CONFLICT, "Tutor " + tutor.getId() + " already has 6 bookings on " + request.getDate() + "; set override=true to continue");
        }

        Booking booking = new Booking("NEW-" + UUID.randomUUID(), request.getDate(), request.getStartTime(),
                request.getDurationMin(), request.getStudentName(), tutor, request.getRoomId(), BookingStatus.BOOKED,
                request.bookingTypeOrSingle(), null, null);
        bookings.save(booking);

        changes.save(new BookingChange(booking, now(), ChangeType.CREATED, null, describe(booking), false));
        if (Boolean.TRUE.equals(request.getOverride())) {
            changes.save(new BookingChange(booking, now(), ChangeType.OVERRIDDEN, null,
                    "Tutor already had 6 or more bookings on " + request.getDate(), afterCutoff(request.getDate())));
        }

        return booking;
    }

    @Transactional
    public Booking update(Long id, BookingUpdateRequest request) {
        Booking booking = get(id);
        LocalDate date = request.getDate() == null ? booking.getDate() : request.getDate();
        LocalTime start = request.getStartTime() == null ? booking.getStartTime() : request.getStartTime();
        int duration = request.getDurationMin() == null ? booking.getDurationMin() : request.getDurationMin();
        String room = request.getRoomId() == null ? booking.getRoomId() : request.getRoomId();

        validateInput(date, start, duration, booking.getTutor().getId(), room);

        List<Booking> sameDay = bookings.findByDateAndTutorId(date, booking.getTutor().getId());
        checkConflicts(date, start, duration, room, booking.getBookingType(), sameDay, id);

        String oldValue = describe(booking);
        booking.reschedule(date, start, duration, room);
        bookings.save(booking);
        changes.save(new BookingChange(booking, now(), ChangeType.RESCHEDULED, oldValue, describe(booking), afterCutoff(date)));

        return booking;
    }

    @Transactional
    public Booking cancel(Long id) {
        Booking booking = get(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking " + id + " is already cancelled");
        }

        LocalDateTime cancelledAt = now();
        boolean charged = !cancelledAt.isBefore(LocalDateTime.of(booking.getDate(), booking.getStartTime()).minusHours(4));
        String oldValue = describe(booking);

        booking.cancel(cancelledAt, charged);
        bookings.save(booking);
        changes.save(new BookingChange(booking, cancelledAt, ChangeType.CANCELLED, oldValue,
                "status=CANCELLED, charged=" + charged, afterCutoff(booking.getDate())));

        return booking;
    }

    public List<Booking> byDate(LocalDate date) { 
        return bookings.findByDateOrderByStartTime(date); 
    }

    public List<BookingChange> changes(Long id) { 
        get(id); return changes.findByBookingIdOrderByChangedAt(id); 
    }

    public Booking get(Long id) { 
        return bookings.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found: " + id)); 
    }

    private void checkConflicts(LocalDate date, LocalTime start, int duration, String room,
                                BookingType type, List<Booking> candidates, Long ignoredId) {
        LocalDateTime startAt = LocalDateTime.of(date, start);
        LocalDateTime endAt = startAt.plusMinutes(duration);

        for (Booking other : candidates) {
            if (ignoredId != null && other.getId().equals(ignoredId)) continue;
            if (!usesCapacity(other) || !overlaps(startAt, endAt, other)) continue;
            boolean pairedException = type == BookingType.PAIRED && other.getBookingType() == BookingType.PAIRED
                    && room.equals(other.getRoomId()) && startAt.equals(LocalDateTime.of(other.getDate(), other.getStartTime()))
                    && endAt.equals(other.endDateTime());
            if (!pairedException) {
                throw new ApiException(HttpStatus.CONFLICT, "Conflict with booking " + other.getLessonId() + " (id " + other.getId() + ")");
            }
        }
        
        for (Booking other : bookings.findByDateOrderByStartTime(date)) {
            if (ignoredId != null && other.getId().equals(ignoredId)) continue;
            if (!usesCapacity(other) || !overlaps(startAt, endAt, other)) continue;
            boolean pairedException = type == BookingType.PAIRED && other.getBookingType() == BookingType.PAIRED
                    && room.equals(other.getRoomId()) && startAt.equals(LocalDateTime.of(other.getDate(), other.getStartTime()))
                    && endAt.equals(other.endDateTime());
            if (!pairedException && room.equals(other.getRoomId())) {
                throw new ApiException(HttpStatus.CONFLICT, "Room conflict with booking " + other.getLessonId() + " (id " + other.getId() + ")");
            }
        }
    }

    private boolean usesCapacity(Booking booking) { 
        return booking.getStatus() != BookingStatus.CANCELLED;
    }

    private boolean overlaps(LocalDateTime start, LocalDateTime end, Booking other) {
        return start.isBefore(other.endDateTime()) && end.isAfter(LocalDateTime.of(other.getDate(), other.getStartTime()));
    }

    private Tutor tutor(String id) {
        return tutors.findById(id).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown tutor: " + id));
    }

    private void validateInput(LocalDate date, LocalTime start, Integer duration, String tutorId, String room) {
        if (date == null || date.getDayOfWeek() == DayOfWeek.MONDAY)
            throw new ApiException(HttpStatus.BAD_REQUEST, "The centre is closed on Monday");
        if (start == null || duration == null || duration <= 0 || tutorId == null || room == null || !room.matches("R[1-6]"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid booking fields");
    }

    private boolean afterCutoff(LocalDate bookingDate) {
        return now().isAfter(LocalDateTime.of(bookingDate.minusDays(1), LocalTime.of(16, 0)));
    }

    private LocalDateTime now() { 
        return LocalDateTime.now(clock); 
    }

    private String describe(Booking b) { 
        return b.getDate() + " " + b.getStartTime() + " duration=" + b.getDurationMin() + " room=" + b.getRoomId(); 
    }
}

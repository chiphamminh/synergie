package com.example.brightpath.entity;

import com.example.brightpath.enums.BookingStatus;
import com.example.brightpath.enums.BookingType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_lesson_id", columnNames = "lesson_id"),
        check = @CheckConstraint(
                name = "ck_booking_status_type",
                constraint = "status in ('BOOKED','CANCELLED','NO_SHOW') and booking_type in ('SINGLE','PAIRED')"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false, length = 30)
    private String lessonId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_min", nullable = false)
    private Integer durationMin;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "room_id", nullable = false, length = 2)
    private String roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false, length = 20)
    private BookingType bookingType = BookingType.SINGLE;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    private String note;

    @Column(nullable = false)
    private boolean charged;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Booking(String lessonId, LocalDate date, LocalTime startTime, Integer durationMin,
                   String studentName, Tutor tutor, String roomId, BookingStatus status,
                   BookingType bookingType, LocalDateTime cancelledAt, String note) {
        this.lessonId = lessonId;
        this.date = date;
        this.startTime = startTime;
        this.durationMin = durationMin;
        this.studentName = studentName;
        this.tutor = tutor;
        this.roomId = roomId;
        this.status = status;
        this.bookingType = bookingType == null ? BookingType.SINGLE : bookingType;
        this.cancelledAt = cancelledAt;
        this.note = note;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }

    public LocalDateTime endDateTime() { return LocalDateTime.of(date, startTime).plusMinutes(durationMin); }
    public void reschedule(LocalDate date, LocalTime startTime, Integer durationMin, String roomId) {
        this.date = date; this.startTime = startTime; this.durationMin = durationMin; this.roomId = roomId;
    }
    public void cancel(LocalDateTime cancelledAt, boolean charged) {
        this.status = BookingStatus.CANCELLED; this.cancelledAt = cancelledAt; this.charged = charged;
    }
}

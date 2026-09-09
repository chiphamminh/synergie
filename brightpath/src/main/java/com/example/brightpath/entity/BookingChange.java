package com.example.brightpath.entity;

import com.example.brightpath.enums.ChangeType;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_changes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonIgnore
    private Booking booking;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "after_cutoff", nullable = false)
    private boolean afterCutoff;

    public BookingChange(Booking booking, LocalDateTime changedAt, ChangeType type,
                         String oldValue, String newValue, boolean afterCutoff) {
        this.booking = booking; this.changedAt = changedAt; this.changeType = type;
        this.oldValue = oldValue; this.newValue = newValue; this.afterCutoff = afterCutoff;
    }
}

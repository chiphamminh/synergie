package com.example.synergie.dto;

import com.example.synergie.enums.BookingType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    private LocalDate date;
    private LocalTime startTime;
    private Integer durationMin;
    private String studentName;
    private String tutorId;
    private String roomId;
    private BookingType bookingType;
    private Boolean override;

    public BookingType bookingTypeOrSingle() {
        return bookingType == null ? BookingType.SINGLE : bookingType;
    }
}

package com.example.brightpath.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingUpdateRequest {
    private LocalDate date;
    private LocalTime startTime;
    private Integer durationMin;
    private String roomId;
    private Boolean override;
}

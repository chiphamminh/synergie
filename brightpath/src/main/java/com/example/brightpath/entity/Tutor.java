package com.example.brightpath.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tutors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tutor {
    @Id
    @Column(name = "tutor_id", length = 20, nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String phone;

    public Tutor(String id, String name, String subject, String phone) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.phone = phone;
    }

}

package com.uietpapers.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "papers")
public class Paper {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String branch; // e.g., CSE, ECE, ME, CE

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private Integer year; // e.g., 2024

    @Column(nullable = false)
    private Integer semester; // 1..8

    @Column(nullable = false)
    private String examType; // MIDSEM or ENDSEM

    @Column(nullable = false, length = 2048)
    private String fileUrl; // public URL from Supabase

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    @Column
    private String title;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}

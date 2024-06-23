package com.spring_boot_file_upload_monitoring.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "file")
@Data
public class FileModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "path", length = 500, nullable = false)
    private String path;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "type", length = 100, nullable = false)
    private String type;

    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;
}
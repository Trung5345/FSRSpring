package com.fsrspring.vocab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "import_jobs", indexes = {
        @Index(name = "idx_import_job_created_at", columnList = "created_at"),
        @Index(name = "idx_import_job_source_type", columnList = "source_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(length = 500)
    private String fileName;

    @Column(length = 200)
    private String targetSetName;

    private Long targetSetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private Status status = Status.COMPLETED;

    @Column(nullable = false)
    @Builder.Default
    private int totalRows = 0;

    @Column(nullable = false)
    @Builder.Default
    private int createdCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int skippedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int failedCount = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rowNumber ASC")
    @JsonIgnoreProperties({"job"})
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    private List<ImportJobRow> rows = new ArrayList<>();

    public void addRow(ImportJobRow row) {
        rows.add(row);
        row.setJob(this);
    }

    public enum Status {
        COMPLETED,
        COMPLETED_WITH_ERRORS
    }
}

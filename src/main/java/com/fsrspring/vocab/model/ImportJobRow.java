package com.fsrspring.vocab.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "import_job_rows", indexes = {
        @Index(name = "idx_import_job_row_status", columnList = "status"),
        @Index(name = "idx_import_job_row_word", columnList = "word_text")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ImportJob job;

    @Column(name = "source_row_number", nullable = false)
    private int rowNumber;

    @Column(length = 100)
    private String clientRowId;

    @Column(name = "word_text", nullable = false, length = 200)
    private String wordText;

    @Column(length = 500)
    private String translation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    private Long wordId;

    @Column(length = 1000)
    private String message;

    public enum Status {
        CREATED,
        SKIPPED,
        FAILED
    }
}

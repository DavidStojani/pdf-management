package org.papercloud.de.pdfdatabase.entity;

import jakarta.persistence.*;
import lombok.*;
import org.papercloud.de.pdfdatabase.config.EncryptedByteArrayConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentPdfEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  private String filename;

  private String contentType;

  private Long size;

  @Enumerated(EnumType.STRING)


  @Lob
  @Column(name = "pdf_content")
  @Convert(converter = EncryptedByteArrayConverter.class)
  private byte[] pdfContent;

  private LocalDateTime uploadedAt;

  @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PagesPdfEntity> pages;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserEntity owner;

  private List<String> tags; //TODO : do this better

  private LocalDate dateOnDocument;

  private boolean failedEnrichment = false;
}

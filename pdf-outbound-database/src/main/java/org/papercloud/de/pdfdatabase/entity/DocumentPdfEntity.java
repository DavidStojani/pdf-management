package org.papercloud.de.pdfdatabase.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.papercloud.de.core.domain.Document;
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
  private Document.Status status;

  @Lob
  @JdbcType(org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType.class)
  @Column(name = "pdf_content")
  @Convert(converter = EncryptedByteArrayConverter.class)
  private byte[] pdfContent;

  private LocalDateTime uploadedAt;

  @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PagesPdfEntity> pages;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserEntity owner;

  @ElementCollection
  @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
  @Column(name = "tag")
  private List<String> tags; //TODO : do this better

  private LocalDate dateOnDocument;

  private boolean failedEnrichment = false;
}

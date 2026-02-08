package org.papercloud.de.pdfdatabase.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.papercloud.de.pdfdatabase.config.EncryptedStringConverter;

@Entity
@Table(name = "document_pages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagesPdfEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int pageNumber;

  @Column(name = "page_text", columnDefinition = "TEXT")
  @Convert(converter = EncryptedStringConverter.class)
  private String pageText; // Extracted text for searching

  @ManyToOne
  @JoinColumn(name = "document_id")
  private DocumentPdfEntity document;
}

package org.papercloud.de.pdfdatabase.entity;

import jakarta.persistence.*;
import lombok.*;

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

  @Lob
  private byte[] pageData; // PDF page as image or PDF blob

  @Lob
  private String pageText; // Extracted text for searching

  @ManyToOne
  @JoinColumn(name = "document_id")
  private DocumentPdfEntity document;
}

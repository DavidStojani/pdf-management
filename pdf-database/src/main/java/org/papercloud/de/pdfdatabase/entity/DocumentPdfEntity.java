package org.papercloud.de.pdfdatabase.entity;

import jakarta.persistence.*;
import lombok.*;

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

  @Lob
  @Column(name = "pdf_content")
  private byte[] pdfContent;

  private LocalDateTime uploadedAt;

  @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PagesPdfEntity> pages;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserEntity owner;

  private List<String> tags; //TODO : do this better

  private String dateOnDocument;
}

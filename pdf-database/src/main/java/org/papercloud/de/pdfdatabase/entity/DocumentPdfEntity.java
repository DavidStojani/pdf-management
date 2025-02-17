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

  private LocalDateTime uploadedAt;

  @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PagesPdfEntity> pages;
}

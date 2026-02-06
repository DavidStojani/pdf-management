package org.papercloud.de.pdfdatabase.repository;

import java.util.List;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PageRepository extends JpaRepository<PagesPdfEntity, Long> {

  List<PagesPdfEntity> findByDocumentId(Long documentId);

  List<PagesPdfEntity> findByDocumentIdOrderByPageNumber(Long id);

  @Query("SELECT p FROM PagesPdfEntity p WHERE p.pageText LIKE %:searchTerm%")
  List<PagesPdfEntity> findByExtractedTextContaining(@Param("searchTerm") String searchTerm);

  // More advanced version with pagination
  @Query("SELECT p FROM PagesPdfEntity p WHERE p.pageText LIKE %:searchTerm%")
  Page<PagesPdfEntity> findByExtractedTextContaining(@Param("searchTerm") String searchTerm, Pageable pageable);

  // Count matches per document
  @Query("SELECT p.document.id, COUNT(p) FROM PagesPdfEntity p WHERE p.pageText LIKE %:searchTerm% GROUP BY p.document.id")
  List<Object[]> countPageMatchesPerDocument(@Param("searchTerm") String searchTerm);
}

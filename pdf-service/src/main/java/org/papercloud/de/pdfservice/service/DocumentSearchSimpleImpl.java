package org.papercloud.de.pdfservice.service;

import java.util.List;
import java.util.stream.Collectors;
import jdk.jfr.Timestamp;
import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.SearchHitDTO;
import org.papercloud.de.common.dto.SearchResultDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentSearchSimpleImpl {

  private final DocumentRepository documentRepository;
  private final PageRepository pageRepository;

  @Transactional(readOnly = true)
  public SearchResultDTO searchDocumentByText(String searchTerm) {

    List<PagesPdfEntity> matchingPages = pageRepository.findByExtractedTextContaining(searchTerm);
    List<SearchHitDTO> searchHits = matchingPages.stream()
        .map(p -> mapToSearchHit(p, searchTerm))
        .collect(Collectors.toList());

  return SearchResultDTO.builder()
      .hits(searchHits)
      .totalHits(matchingPages.size())
      .totalPages(matchingPages.size())
      .currentPage(999)
      .build();

  }

  private SearchHitDTO mapToSearchHit(PagesPdfEntity page, String searchTerm) {
    DocumentPdfEntity doc = page.getDocument();

    // Extract a snippet of text surrounding the search term
    String snippet = extractSnippet(page.getPageText(), searchTerm);

    return SearchHitDTO.builder()
        .documentId(doc.getId().toString())
        .documentName(doc.getTitle())
        .pageNumber(page.getPageNumber())
        .textSnippet(snippet)
        .build();
  }

  private String extractSnippet(String text, String searchTerm) {
    // Find the position of the search term
    int pos = text.toLowerCase().indexOf(searchTerm.toLowerCase());
    if (pos == -1) {
      return "";
    }

    // Extract context around the term (100 chars before and after)
    int start = Math.max(0, pos - 100);
    int end = Math.min(text.length(), pos + searchTerm.length() + 100);

    String snippet = text.substring(start, end);

    // Add ellipsis if needed
    if (start > 0) {
      snippet = "..." + snippet;
    }
    if (end < text.length()) {
      snippet = snippet + "...";
    }

    return snippet;
  }

}

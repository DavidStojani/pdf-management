package org.papercloud.de.pdfapi.controller;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.dto.search.IndexableDocumentDTO;
import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.papercloud.de.pdfservice.search.DocumentSearchSimpleImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

  //private final DocumentSearchSimpleImpl searchService;

  private final SearchService searchService;

  @PostMapping
  public ResponseEntity<SearchResultDTO> search(@RequestBody SearchRequestDTO request) {
    request.setUsername(getCurrentUsername());

    SearchResultDTO result = searchService.search(request);
    return ResponseEntity.ok(result);
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
  /*
  @GetMapping
  public ResponseEntity<SearchResultDTO> search(
      @RequestParam String query) {

    SearchResultDTO results = searchService.searchDocumentByText(query);
    return ResponseEntity.ok(results);
  }

   */
}

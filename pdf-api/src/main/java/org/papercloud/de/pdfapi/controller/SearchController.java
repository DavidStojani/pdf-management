package org.papercloud.de.pdfapi.controller;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.search.SearchResultDTO;
import org.papercloud.de.pdfservice.search.DocumentSearchSimpleImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

  private final DocumentSearchSimpleImpl searchService;

  @GetMapping
  public ResponseEntity<SearchResultDTO> search(
      @RequestParam String query) {

    SearchResultDTO results = searchService.searchDocumentByText(query);
    return ResponseEntity.ok(results);
  }
}

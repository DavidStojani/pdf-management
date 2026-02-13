package org.papercloud.de.pdfservice.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.dto.search.SearchHitDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
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
                .map(page -> mapToSearchHit(page, searchTerm))
                .toList();

        return SearchResultDTO.builder()
                .hits(searchHits)
                .totalHits(searchHits.size())
                .totalPages(1) // Placeholder for future pagination logic
                .currentPage(1)
                .build();
    }

    private SearchHitDTO mapToSearchHit(PagesPdfEntity page, String searchTerm) {
        DocumentPdfEntity document = page.getDocument();

        return SearchHitDTO.builder()
                .documentId(document.getId().toString())
                .documentName(document.getTitle())
                .pageNumber(page.getPageNumber())
                .textSnippet(extractSnippet(page.getPageText(), searchTerm))
                .build();
    }

    private String extractSnippet(String text, String searchTerm) {
        String lowerText = text.toLowerCase();
        String lowerSearchTerm = searchTerm.toLowerCase();

        int index = lowerText.indexOf(lowerSearchTerm);
        if (index == -1) return "";

        final int CONTEXT_CHARS = 100;

        int snippetStart = Math.max(0, index - CONTEXT_CHARS);
        int snippetEnd = Math.min(text.length(), index + searchTerm.length() + CONTEXT_CHARS);

        String snippet = text.substring(snippetStart, snippetEnd);

        if (snippetStart > 0) snippet = "..." + snippet;
        if (snippetEnd < text.length()) snippet += "...";

        return snippet;
    }
}


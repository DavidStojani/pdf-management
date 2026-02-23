package org.papercloud.de.pdfservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.dto.document.DocumentListItemDTO;
import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.FavouriteRepository;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSearchServiceImpl implements DocumentSearchService {

    private final DocumentRepository documentRepository;
    private final FavouriteRepository favouriteRepository;
    private final SearchService searchService;
    private final DocumentServiceMapper documentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DocumentListItemDTO> searchDocuments(String username, String query) {
        Set<Long> favouriteIds = favouriteRepository.findFavouriteDocumentIdsByUsername(username);

        if (query == null || query.isBlank()) {
            List<DocumentPdfEntity> documents = documentRepository.findByOwnerUsername(username);
            return mapToListItems(documents, favouriteIds);
        }

        try {
            return searchViaElasticsearch(username, query, favouriteIds);
        } catch (Exception e) {
            log.warn("Elasticsearch search failed, falling back to in-memory filtering", e);
            return searchInMemory(username, query, favouriteIds);
        }
    }

    private List<DocumentListItemDTO> searchViaElasticsearch(String username, String query, Set<Long> favouriteIds) {
        SearchRequestDTO request = SearchRequestDTO.builder()
                .query(query)
                .username(username)
                .page(0)
                .size(50)
                .build();

        SearchResultDTO result = searchService.search(request);

        List<Long> documentIds = result.getHits().stream()
                .map(hit -> Long.parseLong(hit.getDocumentId()))
                .toList();

        if (documentIds.isEmpty()) {
            return List.of();
        }

        Map<Long, DocumentPdfEntity> documentMap = documentRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(DocumentPdfEntity::getId, doc -> doc));

        return documentIds.stream()
                .map(documentMap::get)
                .filter(doc -> doc != null)
                .map(doc -> documentMapper.toListItemDTO(doc, favouriteIds.contains(doc.getId())))
                .toList();
    }

    private List<DocumentListItemDTO> searchInMemory(String username, String query, Set<Long> favouriteIds) {
        List<DocumentPdfEntity> documents = documentRepository.findByOwnerUsername(username);
        String q = query.toLowerCase(Locale.ROOT);
        List<DocumentPdfEntity> filtered = documents.stream()
                .filter(doc -> {
                    String displayTitle = documentMapper.toListItemDTO(doc, false).getTitle();
                    String filename = doc.getFilename();
                    return displayTitle.toLowerCase(Locale.ROOT).contains(q)
                            || (filename != null && filename.toLowerCase(Locale.ROOT).contains(q));
                })
                .toList();
        return mapToListItems(filtered, favouriteIds);
    }

    private List<DocumentListItemDTO> mapToListItems(List<DocumentPdfEntity> documents, Set<Long> favouriteIds) {
        return documents.stream()
                .map(doc -> documentMapper.toListItemDTO(doc, favouriteIds.contains(doc.getId())))
                .toList();
    }
}

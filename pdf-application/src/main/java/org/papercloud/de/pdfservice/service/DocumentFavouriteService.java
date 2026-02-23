package org.papercloud.de.pdfservice.service;

import org.papercloud.de.core.dto.document.DocumentListItemDTO;

import java.util.List;

public interface DocumentFavouriteService {

    List<DocumentListItemDTO> getFavourites(String username);

    void addFavourite(Long documentId, String username);

    void removeFavourite(Long documentId, String username);
}

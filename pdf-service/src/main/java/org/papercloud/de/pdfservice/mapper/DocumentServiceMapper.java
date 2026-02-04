package org.papercloud.de.pdfservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.PageDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;

import java.util.List;

/**
 * MapStruct mapper for converting between JPA entities and DTOs.
 * This mapper is used by the service layer.
 */
@Mapper(componentModel = "spring")
public interface DocumentServiceMapper {

    @Mapping(source = "filename", target = "fileName")
    @Mapping(source = "pages", target = "pages")
    DocumentDTO toDocumentDTO(DocumentPdfEntity entity);

    @Mapping(source = "filename", target = "fileName")
    @Mapping(source = "pdfContent", target = "content")
    @Mapping(source = "contentType", target = "contentType")
    DocumentDownloadDTO toDownloadDTO(DocumentPdfEntity entity);

    @Mapping(source = "pageNumber", target = "pageNumber")
    @Mapping(source = "pageText", target = "extractedText")
    PageDTO toPageDTO(PagesPdfEntity entity);

    List<PageDTO> toPageDTOList(List<PagesPdfEntity> entities);
}

package org.papercloud.de.common.dto.document;


import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    // Map from entity to DTO
    @Mapping(source = "filename", target = "fileName")
    @Mapping(source = "pages", target = "pages")
    DocumentDTO toDocumentDTO(DocumentPdfEntity entity);

    // Map from entity to download DTO
    @Mapping(source = "filename", target = "fileName")
    @Mapping(source = "pdfContent", target = "content")
    @Mapping(source = "contentType", target = "contentType")
    DocumentDownloadDTO toDownloadDTO(DocumentPdfEntity entity);

    // Map from upload DTO to entity
    @Mapping(source = "fileName", target = "filename")
    @Mapping(source = "inputPdfBytes", target = "pdfContent")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "pages", ignore = true)
    @Mapping(target = "title", source = "fileName")
    DocumentPdfEntity toEntity(DocumentUploadDTO dto);

    // Map from PagesPdfEntity to PageDTO
    @Mapping(source = "pageNumber", target = "pageNumber")
    @Mapping(source = "pageText", target = "extractedText")
    PageDTO toPageDTO(PagesPdfEntity entity);

    // Map list of PagesPdfEntity to list of PageDTO
    List<PageDTO> toPageDTOList(List<PagesPdfEntity> entities);

}

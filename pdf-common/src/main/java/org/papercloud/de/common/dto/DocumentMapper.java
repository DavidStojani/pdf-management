package org.papercloud.de.common.dto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
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
  @Mapping(source = "inputStream", target = "pdfContent", qualifiedByName = "inputStreamToByteArray")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "uploadedAt", expression = "java(java.time.LocalDateTime.now())")
  @Mapping(target = "pages", ignore = true)
  @Mapping(target = "title", source = "fileName")
  DocumentPdfEntity toEntity(DocumentUploadDTO dto);

  // Map from PagesPdfEntity to PageDTO
  PageDTO toPageDTO(PagesPdfEntity entity);

  // Map list of PagesPdfEntity to list of PageDTO
  List<PageDTO> toPageDTOList(List<PagesPdfEntity> entities);

  // Custom method to convert InputStream to byte[]
  @Named("inputStreamToByteArray")
  default byte[] inputStreamToByteArray(InputStream inputStream) {
    if (inputStream == null) {
      return null;
    }
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int nRead;
      byte[] data = new byte[4096];
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      buffer.flush();
      return buffer.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to convert InputStream to byte array", e);
    }
  }
}

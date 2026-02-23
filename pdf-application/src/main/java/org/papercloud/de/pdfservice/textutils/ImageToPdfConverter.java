package org.papercloud.de.pdfservice.textutils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ImageToPdfConverter {

    public byte[] convert(List<MultipartFile> images) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (MultipartFile file : images) {
                byte[] imageBytes = file.getBytes();
                String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "camera";
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, imageBytes, name);
                PDPage page = new PDPage(new PDRectangle(pdImage.getWidth(), pdImage.getHeight()));
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImage, 0, 0, pdImage.getWidth(), pdImage.getHeight());
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}

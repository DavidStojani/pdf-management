//package org.papercloud.de.pdfocr.config;
//
//import net.sourceforge.tess4j.Tesseract;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class TesseractConfig {
//    @Bean
//    @ConditionalOnMissingBean
//    public Tesseract tesseract(@Value("${tesseract.datapath}") String dataPath,
//                               @Value("${tesseract.lang}") String language) {
//        Tesseract tesseract = new Tesseract();
//        tesseract.setDatapath(dataPath);
//        tesseract.setLanguage(language);
//        tesseract.setOcrEngineMode(1);
//        tesseract.setTessVariable("user_defined_dpi","300");
//        return tesseract;
//    }
//}

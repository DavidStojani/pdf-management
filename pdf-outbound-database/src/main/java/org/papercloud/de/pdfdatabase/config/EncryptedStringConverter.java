package org.papercloud.de.pdfdatabase.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            return attribute == null ? null : AESCryptoUtil.encrypt(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Encrypting text failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : AESCryptoUtil.decrypt(dbData);
        } catch (Exception e) {
            throw new IllegalStateException("Decrypting text failed", e);
        }
    }
}

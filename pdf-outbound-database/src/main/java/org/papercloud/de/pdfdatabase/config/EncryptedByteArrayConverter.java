package org.papercloud.de.pdfdatabase.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedByteArrayConverter implements AttributeConverter<byte[], byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(byte[] attribute) {
        try {
            return attribute == null ? null : AESCryptoUtil.encrypt(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("PDF encryption failed", e);
        }
    }

    @Override
    public byte[] convertToEntityAttribute(byte[] dbData) {
        try {
            return dbData == null ? null : AESCryptoUtil.decrypt(dbData);
        } catch (Exception e) {
            throw new IllegalStateException("PDF decryption failed", e);
        }
    }}

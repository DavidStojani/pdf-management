package org.papercloud.de.pdfdatabase.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for EncryptedByteArrayConverter.
 * Tests JPA AttributeConverter for encrypting/decrypting byte arrays at the persistence layer.
 */
@DisplayName("EncryptedByteArrayConverter")
class EncryptedByteArrayConverterTest {

    private EncryptedByteArrayConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EncryptedByteArrayConverter();
    }

    @Nested
    @DisplayName("convertToDatabaseColumn")
    class ConvertToDatabaseColumnTests {

        @Test
        @DisplayName("should encrypt byte array for database storage")
        void convertToDatabaseColumn_validByteArray_shouldReturnEncrypted() {
            // Arrange
            byte[] originalData = "PDF content to be encrypted".getBytes(StandardCharsets.UTF_8);

            // Act
            byte[] encrypted = converter.convertToDatabaseColumn(originalData);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(originalData);
        }

        @Test
        @DisplayName("should return null when input is null")
        void convertToDatabaseColumn_nullInput_shouldReturnNull() {
            // Act
            byte[] result = converter.convertToDatabaseColumn(null);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle empty byte array")
        void convertToDatabaseColumn_emptyArray_shouldSucceed() {
            // Arrange
            byte[] emptyArray = new byte[0];

            // Act
            byte[] encrypted = converter.convertToDatabaseColumn(emptyArray);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEmpty(); // encrypted data will have padding
        }

        @Test
        @DisplayName("should handle large byte array")
        void convertToDatabaseColumn_largeArray_shouldSucceed() {
            // Arrange - create 1 MB byte array
            byte[] largeArray = new byte[1024 * 1024];
            for (int i = 0; i < largeArray.length; i++) {
                largeArray[i] = (byte) (i % 256);
            }

            // Act
            byte[] encrypted = converter.convertToDatabaseColumn(largeArray);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(largeArray);
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute")
    class ConvertToEntityAttributeTests {

        @Test
        @DisplayName("should decrypt byte array from database")
        void convertToEntityAttribute_validEncryptedData_shouldReturnDecrypted() {
            // Arrange
            byte[] originalData = "PDF content".getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = converter.convertToDatabaseColumn(originalData);

            // Act
            byte[] decrypted = converter.convertToEntityAttribute(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalData);
        }

        @Test
        @DisplayName("should return null when input is null")
        void convertToEntityAttribute_nullInput_shouldReturnNull() {
            // Act
            byte[] result = converter.convertToEntityAttribute(null);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception when decrypting corrupted data")
        void convertToEntityAttribute_corruptedData_shouldThrowException() {
            // Arrange - corrupted encrypted data
            byte[] corruptedData = new byte[]{1, 2, 3, 4, 5};

            // Act & Assert
            assertThatThrownBy(() -> converter.convertToEntityAttribute(corruptedData))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PDF decryption failed");
        }
    }

    @Nested
    @DisplayName("Roundtrip conversion")
    class RoundtripTests {

        @Test
        @DisplayName("should successfully encrypt and decrypt byte array")
        void roundtrip_shouldPreserveOriginalData() {
            // Arrange
            byte[] originalData = "Test PDF binary content".getBytes(StandardCharsets.UTF_8);

            // Act
            byte[] encrypted = converter.convertToDatabaseColumn(originalData);
            byte[] decrypted = converter.convertToEntityAttribute(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalData);
            assertThat(encrypted).isNotEqualTo(originalData);
        }

        @Test
        @DisplayName("should handle multiple roundtrips")
        void multipleRoundtrips_shouldPreserveData() {
            // Arrange
            byte[] originalData = "Important PDF data".getBytes(StandardCharsets.UTF_8);

            // Act - first roundtrip
            byte[] encrypted1 = converter.convertToDatabaseColumn(originalData);
            byte[] decrypted1 = converter.convertToEntityAttribute(encrypted1);

            // Act - second roundtrip using decrypted data
            byte[] encrypted2 = converter.convertToDatabaseColumn(decrypted1);
            byte[] decrypted2 = converter.convertToEntityAttribute(encrypted2);

            // Assert
            assertThat(decrypted1).isEqualTo(originalData);
            assertThat(decrypted2).isEqualTo(originalData);
        }

        @Test
        @DisplayName("should handle all byte values in roundtrip")
        void roundtrip_allByteValues_shouldSucceed() {
            // Arrange - create byte array with all possible byte values
            byte[] originalData = new byte[256];
            for (int i = 0; i < 256; i++) {
                originalData[i] = (byte) i;
            }

            // Act
            byte[] encrypted = converter.convertToDatabaseColumn(originalData);
            byte[] decrypted = converter.convertToEntityAttribute(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalData);
        }

        @Test
        @DisplayName("should handle null in roundtrip")
        void roundtrip_null_shouldReturnNull() {
            // Act
            byte[] encrypted = converter.convertToDatabaseColumn(null);
            byte[] decrypted = converter.convertToEntityAttribute(null);

            // Assert
            assertThat(encrypted).isNull();
            assertThat(decrypted).isNull();
        }
    }
}

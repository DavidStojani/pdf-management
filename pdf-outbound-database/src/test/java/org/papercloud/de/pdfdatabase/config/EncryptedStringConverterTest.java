package org.papercloud.de.pdfdatabase.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for EncryptedStringConverter.
 * Tests JPA AttributeConverter for encrypting/decrypting strings at the persistence layer.
 */
@DisplayName("EncryptedStringConverter")
class EncryptedStringConverterTest {

    private EncryptedStringConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EncryptedStringConverter();
    }

    @Nested
    @DisplayName("convertToDatabaseColumn")
    class ConvertToDatabaseColumnTests {

        @Test
        @DisplayName("should encrypt string for database storage")
        void convertToDatabaseColumn_validString_shouldReturnEncrypted() {
            // Arrange
            String originalString = "Sensitive data to encrypt";

            // Act
            String encrypted = converter.convertToDatabaseColumn(originalString);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(originalString);
        }

        @Test
        @DisplayName("should return null when input is null")
        void convertToDatabaseColumn_nullInput_shouldReturnNull() {
            // Act
            String result = converter.convertToDatabaseColumn(null);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle empty string")
        void convertToDatabaseColumn_emptyString_shouldSucceed() {
            // Arrange
            String emptyString = "";

            // Act
            String encrypted = converter.convertToDatabaseColumn(emptyString);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEmpty(); // encrypted data will be base64
        }

        @Test
        @DisplayName("should handle Unicode characters")
        void convertToDatabaseColumn_unicodeString_shouldSucceed() {
            // Arrange
            String unicodeString = "Hello 世界 🌍 Привет";

            // Act
            String encrypted = converter.convertToDatabaseColumn(unicodeString);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(unicodeString);
        }

        @Test
        @DisplayName("should handle special characters")
        void convertToDatabaseColumn_specialCharacters_shouldSucceed() {
            // Arrange
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

            // Act
            String encrypted = converter.convertToDatabaseColumn(specialChars);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(specialChars);
        }

        @Test
        @DisplayName("should produce base64-encoded output")
        void convertToDatabaseColumn_shouldProduceBase64Output() {
            // Arrange
            String originalString = "Test data";

            // Act
            String encrypted = converter.convertToDatabaseColumn(originalString);

            // Assert - should be valid base64
            assertThat(encrypted).matches("^[A-Za-z0-9+/]+=*$");
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute")
    class ConvertToEntityAttributeTests {

        @Test
        @DisplayName("should decrypt string from database")
        void convertToEntityAttribute_validEncryptedData_shouldReturnDecrypted() {
            // Arrange
            String originalString = "Sensitive information";
            String encrypted = converter.convertToDatabaseColumn(originalString);

            // Act
            String decrypted = converter.convertToEntityAttribute(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
        }

        @Test
        @DisplayName("should return null when input is null")
        void convertToEntityAttribute_nullInput_shouldReturnNull() {
            // Act
            String result = converter.convertToEntityAttribute(null);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception when decrypting invalid base64")
        void convertToEntityAttribute_invalidBase64_shouldThrowException() {
            // Arrange
            String invalidBase64 = "Not valid base64!@#$%";

            // Act & Assert
            assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidBase64))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Decrypting text failed");
        }

        @Test
        @DisplayName("should throw exception when decrypting corrupted data")
        void convertToEntityAttribute_corruptedData_shouldThrowException() {
            // Arrange - valid base64 but invalid encrypted data
            String corruptedEncrypted = "YWJjZGVm"; // base64 for "abcdef" which is not valid AES encrypted data

            // Act & Assert
            assertThatThrownBy(() -> converter.convertToEntityAttribute(corruptedEncrypted))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Decrypting text failed");
        }
    }

    @Nested
    @DisplayName("Roundtrip conversion")
    class RoundtripTests {

        @Test
        @DisplayName("should successfully encrypt and decrypt string")
        void roundtrip_shouldPreserveOriginalString() {
            // Arrange
            String originalString = "This is a secret message";

            // Act
            String encrypted = converter.convertToDatabaseColumn(originalString);
            String decrypted = converter.convertToEntityAttribute(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
            assertThat(encrypted).isNotEqualTo(originalString);
        }

        @Test
        @DisplayName("should handle multiple roundtrips")
        void multipleRoundtrips_shouldPreserveData() {
            // Arrange
            String originalString = "Important data";

            // Act - first roundtrip
            String encrypted1 = converter.convertToDatabaseColumn(originalString);
            String decrypted1 = converter.convertToEntityAttribute(encrypted1);

            // Act - second roundtrip using decrypted data
            String encrypted2 = converter.convertToDatabaseColumn(decrypted1);
            String decrypted2 = converter.convertToEntityAttribute(encrypted2);

            // Assert
            assertThat(decrypted1).isEqualTo(originalString);
            assertThat(decrypted2).isEqualTo(originalString);
        }

        @Test
        @DisplayName("should handle whitespace characters in roundtrip")
        void roundtrip_whitespaceCharacters_shouldSucceed() {
            // Arrange
            String originalString = "Line 1\nLine 2\tTabbed\r\nWindows line";

            // Act
            String encrypted = converter.convertToDatabaseColumn(originalString);
            String decrypted = converter.convertToEntityAttribute(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
        }

        @Test
        @DisplayName("should handle Unicode in roundtrip")
        void roundtrip_unicode_shouldSucceed() {
            // Arrange
            String originalString = "Hello 世界 🌍 مرحبا";

            // Act
            String encrypted = converter.convertToDatabaseColumn(originalString);
            String decrypted = converter.convertToEntityAttribute(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
        }

        @Test
        @DisplayName("should handle long string in roundtrip")
        void roundtrip_longString_shouldSucceed() {
            // Arrange
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append("This is line ").append(i).append(" of a very long string. ");
            }
            String originalString = sb.toString();

            // Act
            String encrypted = converter.convertToDatabaseColumn(originalString);
            String decrypted = converter.convertToEntityAttribute(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
        }

        @Test
        @DisplayName("should handle null in roundtrip")
        void roundtrip_null_shouldReturnNull() {
            // Act
            String encrypted = converter.convertToDatabaseColumn(null);
            String decrypted = converter.convertToEntityAttribute(null);

            // Assert
            assertThat(encrypted).isNull();
            assertThat(decrypted).isNull();
        }

        @Test
        @DisplayName("should handle JSON-like string in roundtrip")
        void roundtrip_jsonString_shouldSucceed() {
            // Arrange
            String jsonString = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";

            // Act
            String encrypted = converter.convertToDatabaseColumn(jsonString);
            String decrypted = converter.convertToEntityAttribute(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(jsonString);
        }
    }
}

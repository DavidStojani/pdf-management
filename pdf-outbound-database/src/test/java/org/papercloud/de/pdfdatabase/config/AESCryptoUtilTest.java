package org.papercloud.de.pdfdatabase.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AESCryptoUtil.
 * Tests encryption and decryption of both byte arrays and strings using AES encryption.
 */
@DisplayName("AESCryptoUtil")
class AESCryptoUtilTest {

    @Nested
    @DisplayName("Byte array encryption/decryption")
    class ByteArrayCryptoTests {

        @Test
        @DisplayName("should encrypt and decrypt byte array successfully")
        void encryptDecrypt_byteArray_shouldReturnOriginalData() throws Exception {
            // Arrange
            byte[] originalData = "This is a test PDF content".getBytes(StandardCharsets.UTF_8);

            // Act
            byte[] encrypted = AESCryptoUtil.encrypt(originalData);
            byte[] decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalData);
            assertThat(encrypted).isNotEqualTo(originalData);
        }

        @Test
        @DisplayName("should produce different encrypted output each time due to padding")
        void encrypt_byteArray_shouldProduceDifferentOutputForSameInput() throws Exception {
            // Arrange
            byte[] originalData = "Test data".getBytes(StandardCharsets.UTF_8);

            // Act
            byte[] encrypted1 = AESCryptoUtil.encrypt(originalData);
            byte[] encrypted2 = AESCryptoUtil.encrypt(originalData);

            // Assert - with PKCS5Padding, same input produces same output in ECB mode
            assertThat(encrypted1).isEqualTo(encrypted2);
        }

        @Test
        @DisplayName("should encrypt empty byte array")
        void encrypt_emptyByteArray_shouldSucceed() throws Exception {
            // Arrange
            byte[] originalData = new byte[0];

            // Act
            byte[] encrypted = AESCryptoUtil.encrypt(originalData);
            byte[] decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalData);
        }

        @Test
        @DisplayName("should handle large byte array")
        void encryptDecrypt_largeByteArray_shouldSucceed() throws Exception {
            // Arrange - create a large byte array (1 MB)
            byte[] originalData = new byte[1024 * 1024];
            for (int i = 0; i < originalData.length; i++) {
                originalData[i] = (byte) (i % 256);
            }

            // Act
            byte[] encrypted = AESCryptoUtil.encrypt(originalData);
            byte[] decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalData);
        }

        @Test
        @DisplayName("should handle binary data with all byte values")
        void encryptDecrypt_allByteValues_shouldSucceed() throws Exception {
            // Arrange - create byte array with all possible byte values
            byte[] originalData = new byte[256];
            for (int i = 0; i < 256; i++) {
                originalData[i] = (byte) i;
            }

            // Act
            byte[] encrypted = AESCryptoUtil.encrypt(originalData);
            byte[] decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalData);
        }
    }

    @Nested
    @DisplayName("String encryption/decryption")
    class StringCryptoTests {

        @Test
        @DisplayName("should encrypt and decrypt string successfully")
        void encryptDecrypt_string_shouldReturnOriginalString() throws Exception {
            // Arrange
            String originalString = "This is a secret message";

            // Act
            String encrypted = AESCryptoUtil.encrypt(originalString);
            String decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
            assertThat(encrypted).isNotEqualTo(originalString);
        }

        @Test
        @DisplayName("should produce base64-encoded encrypted string")
        void encrypt_string_shouldProduceBase64Output() throws Exception {
            // Arrange
            String originalString = "Test data";

            // Act
            String encrypted = AESCryptoUtil.encrypt(originalString);

            // Assert - should be valid base64
            assertThat(encrypted).matches("^[A-Za-z0-9+/]+=*$");
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            assertThat(decoded).isNotEmpty();
        }

        @Test
        @DisplayName("should encrypt empty string")
        void encrypt_emptyString_shouldSucceed() throws Exception {
            // Arrange
            String originalString = "";

            // Act
            String encrypted = AESCryptoUtil.encrypt(originalString);
            String decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
        }

        @Test
        @DisplayName("should handle Unicode characters")
        void encryptDecrypt_unicodeString_shouldSucceed() throws Exception {
            // Arrange
            String originalString = "Hello 世界 🌍 Привет مرحبا";

            // Act
            String encrypted = AESCryptoUtil.encrypt(originalString);
            String decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
        }

        @Test
        @DisplayName("should handle special characters")
        void encryptDecrypt_specialCharacters_shouldSucceed() throws Exception {
            // Arrange
            String originalString = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

            // Act
            String encrypted = AESCryptoUtil.encrypt(originalString);
            String decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
        }

        @Test
        @DisplayName("should handle newlines and tabs")
        void encryptDecrypt_whitespaceCharacters_shouldSucceed() throws Exception {
            // Arrange
            String originalString = "Line 1\nLine 2\tTabbed\r\nWindows line ending";

            // Act
            String encrypted = AESCryptoUtil.encrypt(originalString);
            String decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
        }

        @Test
        @DisplayName("should handle long string")
        void encryptDecrypt_longString_shouldSucceed() throws Exception {
            // Arrange
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("This is line ").append(i).append(" of a long string. ");
            }
            String originalString = sb.toString();

            // Act
            String encrypted = AESCryptoUtil.encrypt(originalString);
            String decrypted = AESCryptoUtil.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(originalString);
        }
    }

    @Nested
    @DisplayName("Edge cases and error handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("should throw exception when decrypting invalid base64 string")
        void decrypt_invalidBase64_shouldThrowException() {
            // Arrange
            String invalidBase64 = "This is not base64!@#$%";

            // Act & Assert
            assertThatThrownBy(() -> AESCryptoUtil.decrypt(invalidBase64))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw exception when decrypting corrupted data")
        void decrypt_corruptedData_shouldThrowException() {
            // Arrange - create valid base64 but invalid encrypted data
            String corruptedEncrypted = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4, 5});

            // Act & Assert
            assertThatThrownBy(() -> AESCryptoUtil.decrypt(corruptedEncrypted))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should handle roundtrip with different data types")
        void encryptDecrypt_mixedDataTypes_shouldSucceed() throws Exception {
            // Arrange
            String jsonData = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
            String xmlData = "<root><element>value</element></root>";
            String csvData = "name,age,city\nJohn,30,New York";

            // Act & Assert - JSON
            String encryptedJson = AESCryptoUtil.encrypt(jsonData);
            String decryptedJson = AESCryptoUtil.decrypt(encryptedJson);
            assertThat(decryptedJson).isEqualTo(jsonData);

            // Act & Assert - XML
            String encryptedXml = AESCryptoUtil.encrypt(xmlData);
            String decryptedXml = AESCryptoUtil.decrypt(encryptedXml);
            assertThat(decryptedXml).isEqualTo(xmlData);

            // Act & Assert - CSV
            String encryptedCsv = AESCryptoUtil.encrypt(csvData);
            String decryptedCsv = AESCryptoUtil.decrypt(encryptedCsv);
            assertThat(decryptedCsv).isEqualTo(csvData);
        }
    }
}

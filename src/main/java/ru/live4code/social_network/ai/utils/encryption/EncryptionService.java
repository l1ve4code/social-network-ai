package ru.live4code.social_network.ai.utils.encryption;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Value;
import ru.live4code.social_network.ai.utils.annotation.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EncryptionService {

    @Value("${tenchat.token.cipher.transformation}")
    private String keyTransformation;

    private final SecretKey secretKey;

    public EncryptionService(
            @Value("${cipher.signing.key}") String cipherKey,
            @Value("${tenchat.token.cipher.algorithm}") String keyAlgorithm
    ) {
        byte[] key = cipherKey.getBytes();
        this.secretKey = new SecretKeySpec(key, keyAlgorithm);
    }

    public String decrypt(String message) {
        try {
            byte[] encryptedBytes = Base64.decode(message);
            byte[] iv = new byte[16];
            byte[] encryptedBytesWithoutIV = new byte[encryptedBytes.length - iv.length];
            System.arraycopy(encryptedBytes, 0, iv, 0, iv.length);
            System.arraycopy(encryptedBytes, iv.length, encryptedBytesWithoutIV, 0, encryptedBytesWithoutIV.length);
            var cipher = Cipher.getInstance(keyTransformation);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new String(cipher.doFinal(encryptedBytesWithoutIV));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new IllegalStateException("Something went wrong on decrypting message!");
        }
    }

    public Map<Long, String> decryptByClients(Map<Long, String> encryptedTokenByClient) {
        return encryptedTokenByClient.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, encryptedToken -> decrypt(encryptedToken.getValue())));
    }

    public String encrypt(String message) {
        try {
            var cipher = Cipher.getInstance(keyTransformation);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new SecureRandom());
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            byte[] iv = cipher.getIV();
            byte[] encryptedBytesWithIV = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, encryptedBytesWithIV, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, encryptedBytesWithIV, iv.length, encryptedBytes.length);
            return Base64.toBase64String(encryptedBytesWithIV);

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new IllegalStateException("Something went wrong on encrypting message!");
        }
    }

}

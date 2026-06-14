package com.babaai.core.security;

import com.babaai.core.config.AppProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.stereotype.Component;

@Component
public class RsaKeyProvider {

    private static final String PRIVATE_KEY_FILE = "jwt-private.pem";
    private static final String PUBLIC_KEY_FILE = "jwt-public.pem";

    private final AppProperties appProperties;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public RsaKeyProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    void init() throws Exception {
        Path keyDir = Path.of(appProperties.getJwt().getKeyPath()).toAbsolutePath().normalize();
        Files.createDirectories(keyDir);
        Path privatePath = keyDir.resolve(PRIVATE_KEY_FILE);
        Path publicPath = keyDir.resolve(PUBLIC_KEY_FILE);

        if (Files.exists(privatePath) && Files.exists(publicPath)) {
            privateKey = PemUtils.readPrivateKey(privatePath);
            publicKey = PemUtils.readPublicKey(publicPath);
            return;
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        PemUtils.writePrivateKey(privatePath, privateKey);
        PemUtils.writePublicKey(publicPath, publicKey);
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return "babaai-core-1";
    }

    static final class PemUtils {

        private PemUtils() {
        }

        static RSAPrivateKey readPrivateKey(Path path) throws Exception {
            String pem = Files.readString(path);
            byte[] decoded = decodePem(pem, "PRIVATE KEY");
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(decoded);
            return (RSAPrivateKey) java.security.KeyFactory.getInstance("RSA").generatePrivate(spec);
        }

        static RSAPublicKey readPublicKey(Path path) throws Exception {
            String pem = Files.readString(path);
            byte[] decoded = decodePem(pem, "PUBLIC KEY");
            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(decoded);
            return (RSAPublicKey) java.security.KeyFactory.getInstance("RSA").generatePublic(spec);
        }

        static void writePrivateKey(Path path, RSAPrivateKey key) throws IOException {
            String pem = "-----BEGIN PRIVATE KEY-----\n"
                    + java.util.Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded())
                    + "\n-----END PRIVATE KEY-----\n";
            Files.writeString(path, pem);
        }

        static void writePublicKey(Path path, RSAPublicKey key) throws IOException {
            String pem = "-----BEGIN PUBLIC KEY-----\n"
                    + java.util.Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded())
                    + "\n-----END PUBLIC KEY-----\n";
            Files.writeString(path, pem);
        }

        private static byte[] decodePem(String pem, String label) {
            String normalized = pem
                    .replace("-----BEGIN " + label + "-----", "")
                    .replace("-----END " + label + "-----", "")
                    .replaceAll("\\s", "");
            return java.util.Base64.getDecoder().decode(normalized);
        }
    }
}

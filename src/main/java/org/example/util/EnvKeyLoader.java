package org.example.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EnvKeyLoader {
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public EnvKeyLoader() {
        try {
            Dotenv dotenv = Dotenv.load();
            String privStr = dotenv.get("HOST_PRIVATE_KEY");
            String pubStr = dotenv.get("HOST_PUBLIC_KEY");

            if (privStr == null || pubStr == null) {
                throw new RuntimeException("Không tìm thấy khóa trong file .env!");
            }

            this.privateKey = parsePrivateKey(privStr);
            this.publicKey = parsePublicKey(pubStr);

            System.out.println("Đã load khóa từ .env thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi load key từ .env");
        }
    }

    private PrivateKey parsePrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private PublicKey parsePublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public PrivateKey getPrivateKey() { return privateKey; }
    public PublicKey getPublicKey() { return publicKey; }
}

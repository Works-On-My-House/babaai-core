package com.babaai.core.api;

import com.babaai.core.dto.Dtos;
import com.babaai.core.security.RsaKeyProvider;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import java.math.BigInteger;
import java.util.Base64;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

    private final RsaKeyProvider rsaKeyProvider;

    public JwksController(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    public ObjectNode jwks() {
        ObjectNode key = JsonNodeFactory.instance.objectNode();
        key.put("kty", "RSA");
        key.put("use", "sig");
        key.put("alg", "RS256");
        key.put("kid", rsaKeyProvider.getKeyId());
        key.put("n", base64Url(rsaKeyProvider.getPublicKey().getModulus()));
        key.put("e", base64Url(rsaKeyProvider.getPublicKey().getPublicExponent()));
        ArrayNode keys = JsonNodeFactory.instance.arrayNode();
        keys.add(key);
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.set("keys", keys);
        return response;
    }

    private String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

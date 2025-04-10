// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.scratch;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import java.util.Base64;
import javax.crypto.KeyGenerator;

public class CreateAESKey
{
    public static void main(final String[] args) throws Exception {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        final SecretKey key = keyGenerator.generateKey();
        String keyStr = Base64.getEncoder().withoutPadding().encodeToString(key.getEncoded());
        final FileOutputStream fos = new FileOutputStream("/tmp/aes.key");
        fos.write(keyStr.getBytes(StandardCharsets.UTF_8));
        fos.close();
        final FileInputStream fis = new FileInputStream("/tmp/aes.key");
        keyStr = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        final byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        final SecretKey secret = new SecretKeySpec(keyBytes, "AES");
        if (!secret.equals(key)) {
            throw new RuntimeException("Keys not equal!");
        }
    }
}

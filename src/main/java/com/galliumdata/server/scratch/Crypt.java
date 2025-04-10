// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.scratch;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Crypt
{
    public static void main(final String[] args) throws Exception {
        final String text = "These are the times";
        final String keyPath = "/Users/maxtardiveau/IdeaProjects/DataGammaEngine/src/test/data/MSSQL/AlwaysEncrypted/key.aes";
        final String keyStr = Files.readAllLines(Paths.get(keyPath, new String[0])).get(0);
        final byte[] key = Hex.decodeHex(keyStr);
        final SecretKey secretKeySpec = new SecretKeySpec(key, "AES");
        final Cipher encodingCipher = Cipher.getInstance("AES");
        encodingCipher.init(1, secretKeySpec);
        final byte[] encryptedBytes = encodingCipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        final String encodedText = Hex.encodeHexString(encryptedBytes);
        System.out.println("Encoded text: " + encodedText);
        final byte[] bytesToDecode = Hex.decodeHex(encodedText);
        final Cipher decodingCipher = Cipher.getInstance("AES");
        decodingCipher.init(2, secretKeySpec);
        final byte[] decodedBytes = decodingCipher.doFinal(bytesToDecode);
        final String decodedText = new String(decodedBytes, StandardCharsets.UTF_8);
        System.out.println("Decoded text: " + decodedText);
    }
}

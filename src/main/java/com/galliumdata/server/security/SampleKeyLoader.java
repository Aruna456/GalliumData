// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.security;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.spec.KeySpec;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import org.apache.commons.codec.binary.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManager;
import java.security.PrivateKey;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.KeyManagerFactory;
import java.security.cert.Certificate;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

public class SampleKeyLoader implements KeyLoader
{
    private static final String BASE_DIR;
    
    @Override
    public Map<String, Object> loadKeys(final Map<String, Object> params) {
        System.out.println(" ---- ---- SampleKeyLoader is loading keys");
        final Map<String, Object> res = new HashMap<String, Object>();
        try {
            final PrivateKey privateKey = this.readPrivateKey();
            final Certificate serverCert = this.readCertificate("server-cert.pem");
            final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null, null);
            keystore.setCertificateEntry("server-cert", serverCert);
            final Certificate[] keyCerts = { serverCert };
            final KeyStore.PrivateKeyEntry entry = new KeyStore.PrivateKeyEntry(privateKey, keyCerts);
            keystore.setEntry("server-key", entry, new KeyStore.PasswordProtection("GalliumData".toCharArray()));
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, "GalliumData".toCharArray());
            final KeyManager[] keyManagers = kmf.getKeyManagers();
            res.put("keyManagers", keyManagers);
            final Certificate caCert = this.readCertificate("ca.pem");
            final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca-cert", caCert);
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            final TrustManager[] trustManagers = tmf.getTrustManagers();
            res.put("trustManagers", trustManagers);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        return res;
    }
    
    private PrivateKey readPrivateKey() {
        try {
            String pemStr = new String(Files.readAllBytes(Paths.get(SampleKeyLoader.BASE_DIR + "/server-key.pem", new String[0])));
            pemStr = pemStr.replace("-----BEGIN PRIVATE KEY-----", "");
            pemStr = pemStr.replace("-----END PRIVATE KEY-----", "");
            final byte[] decoded = Base64.decodeBase64(pemStr.trim());
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
    private Certificate readCertificate(final String fileName) {
        try {
            String pemStr = new String(Files.readAllBytes(Paths.get(SampleKeyLoader.BASE_DIR + "/" + fileName, new String[0])));
            pemStr = pemStr.replace("-----BEGIN CERTIFICATE-----", "");
            pemStr = pemStr.replace("-----END CERTIFICATE-----", "");
            final byte[] decoded = Base64.decodeBase64(pemStr.trim());
            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCertificate(new ByteArrayInputStream(decoded));
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
    public static void main(final String[] args) {
        final SampleKeyLoader keyLoader = new SampleKeyLoader();
        final Map<String, Object> res = keyLoader.loadKeys(null);
        System.out.println("Result: " + String.valueOf(res));
    }
    
    static {
        BASE_DIR = System.getenv("SAMPLE_KEY_LOADER_BASE_DIR");
    }
}

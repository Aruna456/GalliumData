// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import org.apache.logging.log4j.LogManager;
import java.security.PrivateKey;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.ASN1Sequence;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.stream.Stream;
import java.io.IOException;
import java.util.function.Consumer;
import java.nio.file.FileVisitOption;
import com.galliumdata.server.log.Markers;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedWriter;
import java.nio.file.OpenOption;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.nio.file.Path;
import java.io.Reader;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import com.galliumdata.server.handler.ProtocolDataValue;
import com.galliumdata.server.handler.ProtocolDataArray;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.handler.ProtocolData;
import java.util.Iterator;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.ArrayList;
import java.util.TreeMap;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.adapters.Variables;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManager;
import java.security.KeyStore;
import java.util.Map;

public class Project extends RepositoryObject
{
    @Persisted(directoryName = "connections", memberClass = Connection.class)
    protected Map<String, Connection> connections;
    @Persisted(directoryName = "connection_filters", memberClass = FilterUse.class, fileName = "connection_filter.json")
    protected Map<String, FilterUse> connectionFilters;
    @Persisted(directoryName = "request_filters", memberClass = FilterUse.class, fileName = "request_filter.json")
    protected Map<String, FilterUse> requestFilters;
    @Persisted(directoryName = "response_filters", memberClass = FilterUse.class, fileName = "response_filter.json")
    protected Map<String, FilterUse> responseFilters;
    @Persisted(directoryName = "duplex_filters", memberClass = FilterUse.class, fileName = "duplex_filter.json")
    protected Map<String, FilterUse> duplexFilters;
    @Persisted(JSONName = "keyStore")
    protected KeyStore keyStore;
    protected char[] keyStorePassword;
    private boolean cryptoRead;
    protected String keyAlgorithm;
    private KeyManager[] keyManagers;
    private TrustManager[] trustManagers;
    private SSLContext sslContext;
    private int cryptoHash;
    private List<Breakpoint> breakpoints;
    private Variables projectContext;
    private static final Logger log;
    
    public Project(final Repository repo) {
        super(repo);
        this.cryptoRead = false;
        this.keyAlgorithm = "RSA";
        this.projectContext = new Variables();
    }
    
    @Override
    public void forgetEverything() {
        this.connectionFilters = null;
        this.requestFilters = null;
        this.responseFilters = null;
        this.duplexFilters = null;
        this.cryptoRead = false;
        this.keyManagers = null;
        this.trustManagers = null;
        this.breakpoints = null;
    }
    
    public Map<String, Connection> getConnections() {
        if (this.connections == null) {
            this.connections = new TreeMap<String, Connection>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("connections"));
            }
            catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return this.connections;
    }
    
    public Map<String, FilterUse> getFilters(final FilterStage filterStage) {
        switch (filterStage) {
            case CONNECTION: {
                return this.getConnectionFilters();
            }
            case REQUEST: {
                return this.getRequestFilters();
            }
            case RESPONSE: {
                return this.getResponseFilters();
            }
            default: {
                throw new RuntimeException("Invalid filter type: " + String.valueOf(filterStage));
            }
        }
    }
    
    private Map<String, FilterUse> orderFiltersByPriority(Map<String, FilterUse> filters) {
        final Set<Map.Entry<String, FilterUse>> entries = filters.entrySet();
        final List<Map.Entry<String, FilterUse>> entryList = new ArrayList<Map.Entry<String, FilterUse>>(entries);
        entryList.sort((o1, o2) -> {
            final int p2 = o1.getValue().getPriority();
            final int p3 = o2.getValue().getPriority();
            if (p2 == p3) {
                return 0;
            }
            else {
                return (p2 < p3) ? 1 : -1;
            }
        });
        filters = new LinkedHashMap<String, FilterUse>();
        for (final Map.Entry<String, FilterUse> entry : entryList) {
            filters.put(entry.getKey(), entry.getValue());
        }
        return filters;
    }
    
    private Map<String, FilterUse> getConnectionFilters() {
        if (this.connectionFilters == null) {
            this.connectionFilters = new TreeMap<String, FilterUse>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("connectionFilters"));
            }
            catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
            this.connectionFilters = this.orderFiltersByPriority(this.connectionFilters);
        }
        return this.connectionFilters;
    }
    
    private Map<String, FilterUse> getRequestFilters() {
        if (this.requestFilters == null) {
            this.requestFilters = new TreeMap<String, FilterUse>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("requestFilters"));
            }
            catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
            this.getDuplexFilters();
            this.requestFilters.putAll(this.duplexFilters);
            this.requestFilters = this.orderFiltersByPriority(this.requestFilters);
        }
        return this.requestFilters;
    }
    
    private Map<String, FilterUse> getResponseFilters() {
        if (this.responseFilters == null) {
            this.responseFilters = new TreeMap<String, FilterUse>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("responseFilters"));
            }
            catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
            this.getDuplexFilters();
            this.responseFilters.putAll(this.duplexFilters);
            this.responseFilters = this.orderFiltersByPriority(this.responseFilters);
        }
        return this.responseFilters;
    }
    
    private void getDuplexFilters() {
        if (this.duplexFilters == null) {
            this.duplexFilters = new TreeMap<String, FilterUse>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("duplexFilters"));
            }
            catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    public List<Breakpoint> getBreakpoints() {
        this.readBreakpoints();
        return this.breakpoints;
    }
    
    public ProtocolData getBreakpointsAsJson() {
        final ProtocolData topNode = new ProtocolDataObject();
        final ProtocolDataArray bpsNode = new ProtocolDataArray();
        topNode.put("breakpoints", bpsNode);
        for (final Breakpoint bp : this.getBreakpoints()) {
            final ProtocolData bpNode = new ProtocolDataObject();
            bpNode.put("file", new ProtocolDataValue(bp.filename));
            bpNode.put("line", new ProtocolDataValue(bp.linenum));
            bpsNode.add(bpNode);
        }
        return topNode;
    }
    
    public void addBreakpoint(final Breakpoint bp) {
        this.readBreakpoints();
        if (!this.breakpoints.contains(bp)) {
            this.breakpoints.add(bp);
            this.writeBreakpoints();
        }
    }
    
    public void removeBreakpoint(final Breakpoint bp) {
        this.readBreakpoints();
        this.breakpoints.remove(bp);
        this.writeBreakpoints();
    }
    
    private void readBreakpoints() {
        if (this.breakpoints != null) {
            return;
        }
        this.breakpoints = new ArrayList<Breakpoint>();
        final Path bpPath = this.path.resolveSibling("breakpoints.json");
        if (!Files.exists(bpPath, new LinkOption[0]) || !Files.isRegularFile(bpPath, new LinkOption[0]) || !Files.isReadable(bpPath)) {
            return;
        }
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode node;
        try (final BufferedReader reader = Files.newBufferedReader(bpPath)) {
            node = mapper.readTree((Reader)reader);
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.BadFile", new Object[] { bpPath.toString(), ex.getMessage() });
        }
        final JsonNode bps = node.get("breakpoints");
        if (bps == null || bps.isNull()) {
            return;
        }
        if (!bps.isArray()) {
            throw new RepositoryException("repo.BadFile", new Object[] { bpPath.toString(), "breakpoints is not an array" });
        }
        for (int i = 0; i < bps.size(); ++i) {
            final JsonNode bp = bps.get(i);
            if (!bp.isObject()) {
                throw new RepositoryException("repo.BadFile", new Object[] { bpPath.toString(), "breakpoint is not an object at index " + i });
            }
            if (!bp.hasNonNull("file")) {
                throw new RepositoryException("repo.BadFile", new Object[] { bpPath.toString(), "breakpoint has no file attribute at index " + i });
            }
            if (!bp.hasNonNull("line")) {
                throw new RepositoryException("repo.BadFile", new Object[] { bpPath.toString(), "breakpoint has no line attribute at index " + i });
            }
            final Breakpoint newBp = new Breakpoint(bp.get("file").asText(), bp.get("line").asInt());
            this.breakpoints.add(newBp);
        }
    }
    
    public void writeBreakpoints() {
        final Path bpPath = this.path.resolveSibling("breakpoints.json");
        try (final BufferedWriter writer = Files.newBufferedWriter(bpPath, new OpenOption[0])) {
            final String json = this.getBreakpointsAsJson().toPrettyJSON();
            writer.write(json);
            writer.flush();
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.BadFile", new Object[] { this.path.toString(), ex.getMessage() });
        }
    }
    
    public Variables getProjectContext() {
        return this.projectContext;
    }
    
    public void setProjectContext(final Variables v) {
        this.projectContext = v;
    }
    
    public boolean projectHasKey() {
        this.readCryptoFiles();
        return this.keyManagers != null;
    }
    
    public KeyManager[] getKeyManagers() {
        this.readCryptoFiles();
        return this.keyManagers;
    }
    
    public void addKeyManagers(final KeyManager[] mgrs) {
        int newSize = 0;
        if (this.keyManagers != null) {
            newSize = this.keyManagers.length;
        }
        newSize += mgrs.length;
        final KeyManager[] newMgrs = new KeyManager[newSize];
        System.arraycopy(mgrs, 0, newMgrs, 0, mgrs.length);
        if (this.keyManagers != null) {
            System.arraycopy(this.keyManagers, 0, newMgrs, mgrs.length, this.keyManagers.length);
        }
        this.keyManagers = newMgrs;
    }
    
    public TrustManager[] getTrustManagers() {
        this.readCryptoFiles();
        return this.trustManagers;
    }
    
    public void addTrustManagers(final TrustManager[] mgrs) {
        int newSize = 0;
        if (this.trustManagers != null) {
            newSize = this.trustManagers.length;
        }
        newSize += mgrs.length;
        final TrustManager[] newMgrs = new TrustManager[newSize];
        System.arraycopy(mgrs, 0, newMgrs, 0, mgrs.length);
        if (this.trustManagers != null) {
            System.arraycopy(this.trustManagers, 0, newMgrs, mgrs.length, this.trustManagers.length);
        }
        this.trustManagers = newMgrs;
    }
    
    public int getCryptoHash() {
        if (this.cryptoHash == 0) {
            this.readCryptoFiles();
        }
        return this.cryptoHash;
    }
    
    public static TrustManager[] getCredulousTrustManagers() {
        return new TrustManager[] { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                
                @Override
                public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                }
                
                @Override
                public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                }
            } };
    }
    
    private void readCryptoFiles() {
        if (this.cryptoRead) {
            return;
        }
        this.cryptoRead = true;
        final Path cryptoDir = this.path.resolveSibling("crypto");
        if (!Files.exists(cryptoDir, new LinkOption[0])) {
            return;
        }
        Project.log.trace(Markers.REPO, "Reading crypto files for project: {}", (Object)this.path);
        if (!Files.isDirectory(cryptoDir, new LinkOption[0])) {
            throw new RepositoryException("repo.crypto.NotDirectory", new Object[] { cryptoDir.toString() });
        }
        if (!Files.isReadable(cryptoDir)) {
            throw new RepositoryException("repo.crypto.NotReadable", new Object[] { cryptoDir.toString() });
        }
        final Path jsonPath = cryptoDir.resolve("crypto.json");
        if (Files.exists(jsonPath, new LinkOption[0])) {
            this.readCryptoJSON(jsonPath);
        }
        this.cryptoHash = 0;
        try (final Stream<Path> paths = Files.walk(cryptoDir, 1, new FileVisitOption[0])) {
            paths.forEach(this::addCryptoFile);
        }
        catch (final IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }
    
    private void readCryptoJSON(final Path path) {
        Project.log.trace(Markers.REPO, "Found JSON file for JKS: {}", (Object)path);
        final ObjectMapper objMapper = new ObjectMapper();
        JsonNode topNode;
        try {
            topNode = objMapper.readTree(path.toFile());
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.crypto.JSONReadError", new Object[] { path.toString(), ex.getMessage() });
        }
        final JsonNode pwNode = topNode.get("JKS password");
        if (pwNode != null) {
            this.keyStorePassword = pwNode.asText().toCharArray();
        }
        final JsonNode keyAlgo = topNode.get("Key algorithm");
        if (keyAlgo != null && !keyAlgo.isNull()) {
            this.keyAlgorithm = keyAlgo.asText();
        }
    }
    
    private void addCryptoFile(final Path path) {
        Project.log.trace(Markers.REPO, "Reading crypto file for project: {}", (Object)path);
        final String filename = path.getFileName().toString();
        final String lowerCaseFilename = filename.toLowerCase();
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            return;
        }
        final String s = lowerCaseFilename;
        switch (s) {
            case "keystore.jks": {
                this.readJKS(path);
                break;
            }
            case "key.pem": {
                this.readPrivateKey(path);
                break;
            }
            case "trust.pem": {
                this.readTrustFile(path);
                break;
            }
        }
    }
    
    private void readJKS(final Path path) {
        Project.log.debug(Markers.REPO, "Found JKS file for project {}: {}", (Object)this.name, (Object)path);
        try {
            (this.keyStore = KeyStore.getInstance("JKS")).load(new FileInputStream(path.toFile()), this.keyStorePassword);
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(this.keyStore, this.keyStorePassword);
            this.addKeyManagers(kmf.getKeyManagers());
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(this.keyStore);
            this.addTrustManagers(tmf.getTrustManagers());
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.crypto.JKSReadError", new Object[] { path, ex.getMessage() });
        }
    }
    
    private void readPrivateKey(final Path path) {
        final List<PEMFileReader.PEMEntry> entries = PEMFileReader.readPEMFile(path.toString());
        if (entries.size() < 2) {
            Project.log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " must contain a key and at least one certificate. SSL will not be available to clients.");
            return;
        }
        PrivateKey privateKey = null;
        final List<PEMFileReader.PEMEntry> certEntries = new ArrayList<PEMFileReader.PEMEntry>();
        for (PEMFileReader.PEMEntry entry : entries) {
            this.cryptoHash += entry.hashCode();
            if (this.cryptoHash == 0) {
                this.cryptoHash = 42;
            }
            final String label = entry.label;
            switch (label) {
                case "CERTIFICATE": {
                    certEntries.add(entry);
                    continue;
                }
                case "RSA PRIVATE KEY": {
                    if (privateKey != null) {
                        Project.log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " contains more than one key. Only the first one will be used.");
                        continue;
                    }
                    try {
                        final ASN1Sequence seq = ASN1Sequence.getInstance((Object)entry.bytes);
                        final RSAPrivateKey bcPrivateKey = RSAPrivateKey.getInstance((Object)seq);
                        final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                        final AlgorithmIdentifier algId = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, (ASN1Encodable)DERNull.INSTANCE);
                        privateKey = converter.getPrivateKey(new PrivateKeyInfo(algId, (ASN1Encodable)bcPrivateKey));
                    }
                    catch (final Exception ex) {
                        Project.log.error("Unable to read private key: " + ex.getMessage());
                    }
                    continue;
                }
                case "PRIVATE KEY": {
                    if (privateKey != null) {
                        Project.log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " contains more than one key. Only the first one will be used.");
                        continue;
                    }
                    try {
                        final KeyFactory kf = KeyFactory.getInstance(this.keyAlgorithm);
                        final PKCS8EncodedKeySpec skSpec = new PKCS8EncodedKeySpec(entry.bytes, this.keyAlgorithm);
                        privateKey = kf.generatePrivate(skSpec);
                    }
                    catch (final Exception ex) {
                        Project.log.error("Unable to read private key: " + ex.getMessage());
                    }
                    continue;
                }
                default: {
                    Project.log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " contains a section labelled " + entry.label + " which is not recognized and will be ignored");
                    continue;
                }
            }
        }
        if (privateKey == null) {
            Project.log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " does not contain a private key -- SSL will not be available to clients");
            return;
        }
        if (certEntries.size() == 0) {
            Project.log.warn(Markers.REPO, "Key file " + String.valueOf(path) + " contains a key but does not contain any certificates, which makes the key unusable. SSL will not be available to clients.");
        }
        final Certificate[] keyCerts = new Certificate[certEntries.size()];
        KeyStore keystore;
        try {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null, null);
            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            int certIdx = 0;
            for (PEMFileReader.PEMEntry certEntry : certEntries) {
                final Certificate serverCert = cf.generateCertificate(new ByteArrayInputStream(certEntry.bytes));
                keyCerts[certIdx] = serverCert;
                ++certIdx;
                keystore.setCertificateEntry("server-cert-" + certIdx, serverCert);
            }
        }
        catch (final Exception ex2) {
            throw new RepositoryException("repo.crypto.ErrorReadingCertificate", new Object[] { path.toString(), ex2.getMessage() });
        }
        try {
            final KeyStore.PrivateKeyEntry entry2 = new KeyStore.PrivateKeyEntry(privateKey, keyCerts);
            keystore.setEntry("key.pem", entry2, new KeyStore.PasswordProtection("GalliumData".toCharArray()));
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, "GalliumData".toCharArray());
            this.addKeyManagers(kmf.getKeyManagers());
        }
        catch (final Exception ex2) {
            throw new RepositoryException("repo.crypto.ErrorReadingKey", new Object[] { path.toString(), ex2.getMessage() });
        }
    }
    
    private void readTrustFile(final Path path) {
        try {
            final List<PEMFileReader.PEMEntry> entries = PEMFileReader.readPEMFile(path.toString());
            if (entries.size() == 0) {
                Project.log.warn(Markers.REPO, "Certificate file " + String.valueOf(path) + " must contain at least one certificate. No certificate has been added to the trust store.");
                return;
            }
            final KeyStore trustKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustKeyStore.load(null, null);
            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            int certIdx = 0;
            for (PEMFileReader.PEMEntry entry : entries) {
                this.cryptoHash += entry.hashCode();
                if (this.cryptoHash == 0) {
                    this.cryptoHash = 42;
                }
                ++certIdx;
                final Certificate caCert = cf.generateCertificate(new ByteArrayInputStream(entry.bytes));
                trustKeyStore.setCertificateEntry("ca-" + certIdx, caCert);
            }
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustKeyStore);
            this.addTrustManagers(tmf.getTrustManagers());
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.crypto.ErrorReadingPEM", new Object[] { path.toString(), ex.getMessage() });
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}

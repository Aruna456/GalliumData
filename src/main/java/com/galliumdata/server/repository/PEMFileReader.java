// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import java.util.Base64;
import com.galliumdata.server.log.Markers;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class PEMFileReader
{
    private static final String LABEL_BEGIN = "-----BEGIN ";
    private static final String LABEL_END = "-----";
    private static final String END_BEGIN = "-----END ";
    private static final Logger log;
    
    public static List<PEMEntry> readPEMFile(final String filename) {
        final List<PEMEntry> entries = new ArrayList<PEMEntry>();
        try {
            final byte[] pemBytes = Files.readAllBytes(Paths.get(filename, new String[0]));
            final String pem = new String(pemBytes);
            int idx = 0;
            for (PEMEntry entry = readPEMSection(pem, idx, filename); entry != null; entry = readPEMSection(pem, idx, filename)) {
                entries.add(entry);
                idx += entry.bytesRead;
            }
        }
        catch (final IllegalArgumentException iae) {
            throw new RepositoryException("repo.crypto.ErrorReadingPEM", new Object[] { filename, "most likely the certificate text contains invalid content" });
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.crypto.ErrorReadingPEM", new Object[] { filename, ex.getMessage() });
        }
        return entries;
    }
    
    private static PEMEntry readPEMSection(final String s, final int idx, final String filename) {
        final int beginIdx = s.indexOf("-----BEGIN ", idx);
        if (beginIdx == -1) {
            return null;
        }
        final int labelEndIdx = s.indexOf("-----", beginIdx + "-----BEGIN ".length());
        if (labelEndIdx == -1) {
            throw new RuntimeException("Unable to find label end, i.e. ----- after -----BEGIN");
        }
        final PEMEntry entry = new PEMEntry();
        entry.label = s.substring(beginIdx + "-----BEGIN ".length(), labelEndIdx).trim();
        final int endIdx = s.indexOf("-----END ", labelEndIdx + "-----".length());
        if (endIdx == -1) {
            throw new RuntimeException("Unable to find section end, i.e. -----END ");
        }
        final int endEndIdx = s.indexOf("-----", endIdx + "-----END ".length());
        if (endEndIdx == -1) {
            throw new RuntimeException("Unable to find label end, i.e. ----- after -----END");
        }
        final String endLabel = s.substring(endIdx + "-----END ".length(), endEndIdx);
        if (!entry.label.equals(endLabel)) {
            PEMFileReader.log.warn(Markers.REPO, "PEM entry has mismatched BEGIN and END label: " + entry.label + " vs " + endLabel + " in file " + filename + ". This is only advisory, the entry will be read anyway.");
        }
        String bytesStr = s.substring(labelEndIdx + "-----".length(), endIdx);
        if (bytesStr.charAt(0) == '\n' || bytesStr.charAt(0) == '\r') {
            bytesStr = bytesStr.substring(1);
        }
        final int lenIdx = bytesStr.length() - 1;
        if (bytesStr.charAt(lenIdx) == '\n' || bytesStr.charAt(lenIdx) == '\r') {
            bytesStr = bytesStr.substring(0, lenIdx);
        }
        bytesStr = bytesStr.replaceAll("([^\\r])\\n", "$1\r\n");
        bytesStr = bytesStr.replaceAll("\\r([^\\n])", "\r\n$1");
        entry.bytes = Base64.getMimeDecoder().decode(bytesStr);
        entry.bytesRead = endEndIdx + "-----".length() - idx;
        return entry;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
    
    public static class PEMEntry
    {
        public String label;
        public byte[] bytes;
        public int bytesRead;
        
        @Override
        public int hashCode() {
            return Arrays.hashCode(this.bytes);
        }
    }
}

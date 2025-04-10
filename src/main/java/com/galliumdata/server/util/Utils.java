// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

import org.apache.logging.log4j.LogManager;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import java.net.HttpURLConnection;
import java.net.URL;
import com.galliumdata.server.adapters.Variables;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.Logger;

public class Utils
{
    private static final Logger log;
    
    public byte[] getUTF8BytesForString(final String s) {
        return StringUtil.getUTF8BytesForString(s);
    }
    
    public String stringFromUTF8Bytes(final byte[] bytes) {
        return StringUtil.stringFromUTF8Bytes(bytes);
    }
    
    public String stringFromUTF8Bytes(final byte[] bytes, final int offset, final int numBytes) {
        return new String(bytes, offset, numBytes, StandardCharsets.UTF_8);
    }
    
    public Variables createObject() {
        return new Variables();
    }
    
    public byte[] allocateByteArray(final int size) {
        return new byte[size];
    }
    
    public String getBinaryDump(final byte[] bytes) {
        return BinaryDump.getBinaryDump(bytes, 0, bytes.length);
    }
    
    public String getBinaryDump(final byte[] bytes, final int offset, final int length) {
        return BinaryDump.getBinaryDump(bytes, offset, length);
    }
    
    public GeneralCache createCache(final int maxSize, final long maxIdleTimeInSecs) {
        return new GeneralCache(maxSize, maxIdleTimeInSecs);
    }
    
    public String doPost(final String restUrl, final String payload) {
        Utils.log.trace("doPost called for URL: {}", (Object)restUrl);
        try {
            final URL url = new URL(restUrl);
            final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            final OutputStream os = conn.getOutputStream();
            final byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            os.write(bytes, 0, bytes.length);
            final int respStatus = conn.getResponseCode();
            if (respStatus == 200) {
                Utils.log.trace("doPost successful");
                return IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
            }
            Utils.log.debug("doPost failed: {}", (Object)respStatus);
            return null;
        }
        catch (final Exception ex) {
            Utils.log.debug("doPost failed with exception", (Throwable)ex);
            return null;
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.uselog");
    }
}

// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class BinXMLDocument
{
    private byte[] bytes;
    private byte version;
    protected StringBuilder xml;
    private List<byte[]> extensions;
    private Map<Integer, String> names;
    private int namesIdx;
    private Map<Integer, int[]> qnames;
    private int qnamesIdx;
    private int idx;
    
    public BinXMLDocument(final byte[] bytes) {
        this.xml = new StringBuilder();
        this.extensions = new ArrayList<byte[]>();
        this.names = new HashMap<Integer, String>();
        this.namesIdx = 1;
        this.qnames = new HashMap<Integer, int[]>();
        this.qnamesIdx = 1;
        this.idx = 0;
        this.bytes = bytes;
    }
    
    public String decodeBinaryXML() {
        if (this.bytes == null) {
            return null;
        }
        if (this.bytes.length < 5) {
            throw new RuntimeException("Binary XML stream was less than 5 bytes");
        }
        if (this.bytes[0] != -33 || this.bytes[1] != -1) {
            throw new RuntimeException("Binary XML stream does not start with standard header");
        }
        this.version = this.bytes[2];
        if (this.bytes[3] != -80 || this.bytes[4] != 4) {
            throw new RuntimeException("Binary XML stream has unsupported character encoding");
        }
        this.idx = 5;
        while (this.idx < this.bytes.length) {
            this.readToken();
        }
        this.bytes = null;
        return this.xml.toString();
    }
    
    protected void write(final String s) {
        this.xml.append(s);
    }
    
    protected XMLToken readToken() {
        final byte tokenType = this.bytes[this.idx++];
        XMLToken token = null;
        switch (tokenType) {
            case -22: {
                token = new TokenExtn(this);
                break;
            }
            case -16: {
                token = new TokenNameDef(this);
                break;
            }
            case -17: {
                token = new TokenQNameDef(this);
                break;
            }
            case -8: {
                token = new TokenElement(this);
                break;
            }
            case -9: {
                token = new TokenEndElement(this);
                break;
            }
            case -10: {
                token = new TokenAttribute(this);
                break;
            }
            case -11: {
                token = new TokenEndAttribute(this);
                break;
            }
            case 17: {
                token = new TokenValueString(this);
                break;
            }
            case 126: {
                token = new TokenValueDateTime2(this);
                break;
            }
            case Byte.MAX_VALUE: {
                token = new TokenValueDate2(this);
                break;
            }
            default: {
                throw new RuntimeException("Unknown binary XML token: " + tokenType);
            }
        }
        token.read();
        return token;
    }
    
    protected XMLToken readValue() {
        final byte tokenType = this.bytes[this.idx];
        XMLToken token = null;
        switch (tokenType) {
            case -22: {
                token = new TokenExtn(this);
                break;
            }
            case -16: {
                token = new TokenNameDef(this);
                break;
            }
            case -17: {
                token = new TokenQNameDef(this);
                break;
            }
            case 17: {
                token = new TokenValueString(this);
                break;
            }
            case 126: {
                token = new TokenValueDateTime2(this);
                break;
            }
            case Byte.MAX_VALUE: {
                token = new TokenValueDate2(this);
                break;
            }
            default: {
                return null;
            }
        }
        ++this.idx;
        token.read();
        return token;
    }
    
    protected int readByte() {
        return Byte.toUnsignedInt(this.bytes[this.idx++]);
    }
    
    protected int readInt32() {
        int n = (this.readByte() << 24) + (this.readByte() << 16) + (this.readByte() << 8) + this.readByte();
        n += 4;
        return n;
    }
    
    protected int readInt24() {
        int n = this.readByte() + (this.readByte() << 8) + (this.readByte() << 16);
        n += 3;
        return n;
    }
    
    protected int readVarLengthInt() {
        final byte b1 = this.bytes[this.idx++];
        if (b1 >= 0) {
            return b1;
        }
        final byte b2 = this.bytes[this.idx++];
        if (b2 >= 0) {
            return (b1 & 0x7F) + ((b2 & 0x7F) << 7);
        }
        final byte b3 = this.bytes[this.idx++];
        if (b3 >= 0) {
            return (b1 & 0x7F) + ((b2 & 0x7F) << 7) + ((b3 & 0x7F) << 14);
        }
        final byte b4 = this.bytes[this.idx++];
        if (b4 >= 0) {
            return (b1 & 0x7F) + ((b2 & 0x7F) << 7) + ((b3 & 0x7F) << 14) + (b4 << 24);
        }
        final byte b5 = this.bytes[this.idx++];
        if (b5 < 0) {
            throw new RuntimeException("Invalid byte 5 in multi-byte integer");
        }
        return (b1 & 0x7F) + ((b2 & 0x7F) << 7) + ((b3 & 0x7F) << 14) + ((b4 & 0x7F) << 21);
    }
    
    protected void recordExtension(final byte[] ext) {
        this.extensions.add(ext);
    }
    
    protected void recordName(final String s) {
        this.names.put(this.namesIdx++, s);
    }
    
    protected String getName(final int n) {
        return this.names.get(n);
    }
    
    protected void recordQName(final int[] s) {
        if (s == null || s.length != 3) {
            throw new RuntimeException("Invalid qname: " + String.valueOf(s));
        }
        this.qnames.put(this.qnamesIdx++, s);
    }
    
    protected String getQName(final int num) {
        final int[] parts = this.qnames.get(num);
        if (parts == null) {
            throw new RuntimeException("Invalid qname reference");
        }
        if (parts[2] == 0) {
            return this.getName(parts[1]);
        }
        return this.getName(parts[2]);
    }
    
    protected String getFullQName(final int num) {
        final int[] parts = this.qnames.get(num);
        if (parts == null) {
            throw new RuntimeException("Invalid qname reference");
        }
        String fullName = "";
        if (parts[0] != 0) {
            fullName = this.names.get(parts[0]);
        }
        if (parts[1] != 0) {
            if (fullName.length() > 0) {
                fullName = fullName;
            }
            fullName += (String)this.names.get(parts[1]);
        }
        if (parts[0] != 0) {
            if (fullName.length() > 0) {
                fullName = fullName;
            }
            fullName += (String)this.names.get(parts[2]);
        }
        return fullName;
    }
    
    protected String readString(final int len) {
        final String s = new String(this.bytes, this.idx, len * 2, StandardCharsets.UTF_16LE);
        this.idx += len * 2;
        return s;
    }
    
    protected byte[] readBytes(final int len) {
        final byte[] b = new byte[len];
        System.arraycopy(this.bytes, this.idx, b, 0, len);
        this.idx += len;
        return b;
    }
}

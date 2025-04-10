// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RawPacketWriter
{
    private final ConnectionState connectionState;
    private final MSSQLForwarder forwarder;
    private final byte modelTypeCode;
    private final byte modelStatus;
    private final short modelSpid;
    private final byte modelPacketId;
    private final byte modelWindow;
    protected RawPacket currentPacket;
    private boolean closed;
    protected byte packetId;
    protected int smpSessionId;
    private boolean inDebugMode;
    private final List<RawPacket> debugPackets;
    
    public RawPacketWriter(final ConnectionState connectionState, final MSSQLPacket model, final MSSQLForwarder forwarder) {
        this.smpSessionId = -1;
        this.debugPackets = new ArrayList<RawPacket>();
        this.connectionState = connectionState;
        this.forwarder = forwarder;
        if (model.isWrappedInSMP()) {
            this.smpSessionId = model.getSMPSessionId();
        }
        this.modelTypeCode = model.getTypeCode();
        this.modelStatus = model.getStatus();
        this.modelSpid = model.getSpid();
        this.modelPacketId = model.getPacketId();
        this.modelWindow = model.getWindow();
        this.packetId = model.getPacketId();
    }
    
    public RawPacketWriter(final ConnectionState connectionState, final RawPacket model, final MSSQLForwarder forwarder) {
        this.smpSessionId = -1;
        this.debugPackets = new ArrayList<RawPacket>();
        this.connectionState = connectionState;
        this.forwarder = forwarder;
        if (model.isWrappedInSMP()) {
            this.smpSessionId = model.getSMPSessionId();
        }
        this.modelTypeCode = model.getTypeCode();
        this.modelStatus = model.getStatus();
        this.modelSpid = model.getSpid();
        this.modelPacketId = model.getPacketId();
        this.modelWindow = model.getWindow();
        this.packetId = model.getPacketId();
    }
    
    public RawPacket getPacket() {
        if (this.currentPacket == null) {
            this.addPacket();
        }
        if (this.currentPacket.getRemainingBytesToWrite() == 0 && !this.currentPacket.isFinalized()) {
            this.addPacket();
        }
        return this.currentPacket;
    }
    
    public void addPacket() {
        if (this.currentPacket != null) {
            this.finalizePacket();
            this.currentPacket.setEndOfMessage(false);
            this.sendPacket();
        }
        final byte[] buffer = new byte[this.connectionState.getPacketSize()];
        this.currentPacket = new RawPacket(buffer);
        this.writeModelHeader();
    }
    
    public void close() {
        if (this.closed) {
            return;
        }
        if (this.currentPacket != null) {
            this.finalizePacket();
            this.currentPacket.setEndOfMessage(true);
            this.sendPacket();
            this.closed = true;
        }
    }
    
    public void finalizePacket() {
        this.currentPacket.finalizeLength();
        this.currentPacket.setPacketId(this.packetId);
        ++this.packetId;
    }
    
    public void setDebug() {
        this.inDebugMode = true;
    }
    
    public List<RawPacket> getDebugPackets() {
        if (!this.inDebugMode) {
            throw new RuntimeException("Writer is not in debug mode");
        }
        return this.debugPackets;
    }
    
    protected void sendPacket() {
        final byte[] bytes = this.currentPacket.getWrittenBuffer();
        if (this.inDebugMode) {
            this.debugPackets.add(this.currentPacket);
        }
        try {
            if (this.smpSessionId != -1) {
                this.forwarder.writeOutWithSMP(bytes, 0, bytes.length, this.smpSessionId);
            }
            else {
                this.forwarder.writeOut(bytes, 0, bytes.length);
            }
        }
        catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void writeModelHeader() {
        final byte[] buffer = this.currentPacket.getBuffer();
        buffer[0] = this.modelTypeCode;
        buffer[1] = this.modelStatus;
        DataTypeWriter.encodeTwoByteInteger(buffer, 4, this.modelSpid);
        buffer[6] = this.modelPacketId;
        buffer[7] = this.modelWindow;
        this.currentPacket.setWriteIndex(8);
    }
    
    public int getPacketSize() {
        return this.connectionState.getPacketSize();
    }
    
    public void writeBytes(final byte[] bytes, final int idx, final int numBytes) {
        RawPacket pkt = this.getPacket();
        if (numBytes <= pkt.getRemainingBytesToWrite()) {
            pkt.writeBytes(bytes, idx, numBytes);
            return;
        }
        int numToWrite;
        for (int numWritten = 0; numWritten < numBytes; numWritten += numToWrite) {
            pkt = this.getPacket();
            numToWrite = pkt.getRemainingBytesToWrite();
            if (numBytes - numWritten < numToWrite) {
                numToWrite = numBytes - numWritten;
            }
            pkt.writeBytes(bytes, idx + numWritten, numToWrite);
        }
    }
    
    public int writeBytesUpToSplit(final byte[] bytes, final int idx, final int numBytes) {
        final RawPacket pkt = this.getPacket();
        if (numBytes <= pkt.getRemainingBytesToWrite()) {
            pkt.writeBytes(bytes, idx, numBytes);
            return numBytes;
        }
        final int numToWrite = pkt.getRemainingBytesToWrite();
        pkt.writeBytes(bytes, idx, numToWrite);
        return numToWrite;
    }
    
    public void writeByte(final byte b) {
        this.writeBytes(new byte[] { b }, 0, 1);
    }
    
    public void writeTwoByteInteger(final int i) {
        final byte[] bytes = { (byte)(i >> 8 & 0xFF), (byte)(i & 0xFF) };
        this.writeBytes(bytes, 0, 2);
    }
    
    public void writeTwoByteIntegerLow(final int i) {
        final byte[] bytes = { (byte)(i & 0xFF), (byte)(i >> 8 & 0xFF) };
        this.writeBytes(bytes, 0, 2);
    }
    
    public void writeFourByteInteger(final int i) {
        final byte[] bytes = { (byte)(i >> 24 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i & 0xFF) };
        this.writeBytes(bytes, 0, 4);
    }
    
    public void writeFourByteIntegerLow(final int i) {
        final byte[] bytes = { (byte)(i & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 24 & 0xFF) };
        this.writeBytes(bytes, 0, 4);
    }
    
    public void writeFourByteIntegerLowNoBreak(final int i) {
        if (this.getPacket().getRemainingBytesToWrite() < 4) {
            this.addPacket();
        }
        final byte[] bytes = { (byte)(i & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 24 & 0xFF) };
        this.writeBytes(bytes, 0, 4);
    }
    
    public void writeEightByteIntegerLow(final long i) {
        final byte[] bytes = { (byte)(i & 0xFFL), (byte)(i >> 8 & 0xFFL), (byte)(i >> 16 & 0xFFL), (byte)(i >> 24 & 0xFFL), (byte)(i >> 32 & 0xFFL), (byte)(i >> 40 & 0xFFL), (byte)(i >> 48 & 0xFFL), (byte)(i >> 56 & 0xFFL) };
        this.writeBytes(bytes, 0, 8);
    }
    
    public void writeEightByteNumber(final long num) {
        final byte[] buffer = { (byte)(num >> 0 & 0xFFL), (byte)(num >> 8 & 0xFFL), (byte)(num >> 16 & 0xFFL), (byte)(num >> 24 & 0xFFL), (byte)(num >> 32 & 0xFFL), (byte)(num >> 40 & 0xFFL), (byte)(num >> 48 & 0xFFL), (byte)(num >> 56 & 0xFFL) };
        this.writeBytes(buffer, 0, 8);
    }
    
    public void writeEightByteDecimal(final long num) {
        final byte[] buffer = { (byte)(num >> 32 & 0xFFL), (byte)(num >> 40 & 0xFFL), (byte)(num >> 48 & 0xFFL), (byte)(num >> 56 & 0xFFL), (byte)(num >> 0 & 0xFFL), (byte)(num >> 8 & 0xFFL), (byte)(num >> 16 & 0xFFL), (byte)(num >> 24 & 0xFFL) };
        this.writeBytes(buffer, 0, 8);
    }
    
    public void writeStringWithEncoding(final String s, final int collation) {
        Charset charset = null;
        switch (collation) {
            case 1033: {
                charset = StandardCharsets.US_ASCII;
                break;
            }
            default: {
                charset = StandardCharsets.ISO_8859_1;
                break;
            }
        }
        final byte[] bytes = s.getBytes(charset);
        this.writeBytes(bytes, 0, bytes.length);
    }
}

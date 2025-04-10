// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.ConnectionState;
import java.util.List;

public class TokenSessionState extends MessageToken
{
    private int seqNo;
    private boolean fRecoverable;
    private List<SessionStateData> sessionStateDatas;
    
    public TokenSessionState(final ConnectionState connectionState) {
        super(connectionState);
        this.sessionStateDatas = new ArrayList<SessionStateData>();
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readFourByteIntLow();
        reader.resetMarker();
        this.seqNo = reader.readFourByteIntLow();
        final byte status = reader.readByte();
        this.fRecoverable = ((status & 0x1) != 0x0);
        while (reader.getMarker() < length) {
            final SessionStateData ssd = new SessionStateData();
            ssd.stateId = reader.readByte();
            final byte len1 = reader.readByte();
            if (len1 == -1) {
                final int len2 = reader.readTwoByteIntLow();
                ssd.stateValue = reader.readBytes(len2);
            }
            else {
                ssd.stateValue = reader.readBytes(len1);
            }
            this.sessionStateDatas.add(ssd);
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 4;
        size += 4;
        ++size;
        for (final SessionStateData ssd : this.sessionStateDatas) {
            ++size;
            if (ssd.stateValue.length < 255) {
                ++size;
            }
            else {
                size += 3;
            }
            size += ssd.stateValue.length;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        final int len = this.getSerializedSize() - 5;
        writer.writeFourByteIntegerLow(len);
        writer.writeFourByteIntegerLow(this.seqNo);
        byte flags = 0;
        if (this.fRecoverable) {
            flags |= 0x1;
        }
        writer.writeByte(flags);
        for (final SessionStateData ssd : this.sessionStateDatas) {
            writer.writeByte(ssd.stateId);
            if (ssd.stateValue.length < 255) {
                writer.writeByte((byte)ssd.stateValue.length);
            }
            else {
                writer.writeByte((byte)(-1));
                writer.writeTwoByteIntegerLow(ssd.stateValue.length);
            }
            writer.writeBytes(ssd.stateValue, 0, ssd.stateValue.length);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -28;
    }
    
    @Override
    public String getTokenTypeName() {
        return "SessionState";
    }
    
    public static class SessionStateData
    {
        private byte stateId;
        private byte[] stateValue;
    }
}

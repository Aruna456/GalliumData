// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public abstract class DataStreamPacket extends MSSQLPacket
{
    private List<DataStreamHeader> dataStreamHeaders;
    
    public DataStreamPacket(final ConnectionState connectionState) {
        super(connectionState);
        this.dataStreamHeaders = new ArrayList<DataStreamHeader>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        if (this.connectionState.tdsVersion72andHigher()) {
            final int headersSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
            while (idx - offset < headersSize) {
                final DataStreamHeader hdr = DataStreamHeader.createDataStreamHeader(bytes, idx);
                idx += hdr.readFromBytes(bytes, idx);
                this.dataStreamHeaders.add(hdr);
            }
        }
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        if (this.connectionState.tdsVersion72andHigher()) {
            size += 4;
            for (final DataStreamHeader hdr : this.dataStreamHeaders) {
                size += hdr.getSerializedSize();
            }
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.connectionState.tdsVersion72andHigher()) {
            int hdrSize = 0;
            for (final DataStreamHeader hdr : this.dataStreamHeaders) {
                hdrSize += hdr.getSerializedSize();
            }
            writer.writeFourByteIntegerLow(hdrSize + 4);
            for (final DataStreamHeader hdr : this.dataStreamHeaders) {
                hdr.write(writer);
            }
        }
    }
    
    @Override
    public String getPacketType() {
        return "DataStream";
    }
    
    public List<DataStreamHeader> getDataStreamHeaders() {
        return this.dataStreamHeaders;
    }
}

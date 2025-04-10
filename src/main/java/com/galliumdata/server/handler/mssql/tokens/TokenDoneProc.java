// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class TokenDoneProc extends MessageToken
{
    private boolean statusMore;
    private boolean statusError;
    private boolean statusInXact;
    private boolean statusCount;
    private boolean statusRpcInBatch;
    private boolean statusSrvError;
    private int curCmd;
    private long rowCount;
    
    public TokenDoneProc(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final short status = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.statusMore = ((status & 0x1) > 0);
        this.statusError = ((status & 0x2) > 0);
        this.statusInXact = ((status & 0x4) > 0);
        this.statusCount = ((status & 0x10) > 0);
        this.statusRpcInBatch = ((status & 0x80) > 0);
        this.statusSrvError = ((status & 0x100) > 0);
        this.curCmd = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (this.connectionState.tdsVersion72andHigher()) {
            this.rowCount = DataTypeReader.readEightByteIntegerLow(bytes, idx);
            idx += 8;
        }
        else {
            this.rowCount = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final short status = reader.readTwoByteIntLow();
        this.statusMore = ((status & 0x1) > 0);
        this.statusError = ((status & 0x2) > 0);
        this.statusInXact = ((status & 0x4) > 0);
        this.statusCount = ((status & 0x10) > 0);
        this.statusRpcInBatch = ((status & 0x80) > 0);
        this.statusSrvError = ((status & 0x100) > 0);
        this.curCmd = reader.readTwoByteIntLow();
        if (this.connectionState.tdsVersion72andHigher()) {
            this.rowCount = reader.readEightByteIntLow();
        }
        else {
            this.rowCount = reader.readFourByteIntLow();
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        size += 2;
        if (this.connectionState.tdsVersion72andHigher()) {
            size += 8;
        }
        else {
            size += 4;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        short status = 0;
        if (this.statusMore) {
            status |= 0x1;
        }
        if (this.statusError) {
            status |= 0x2;
        }
        if (this.statusInXact) {
            status |= 0x4;
        }
        if (this.statusCount) {
            status |= 0x10;
        }
        if (this.statusSrvError) {
            status |= 0x20;
        }
        writer.writeTwoByteIntegerLow(status);
        writer.writeTwoByteIntegerLow((short)this.curCmd);
        if (this.connectionState.tdsVersion72andHigher()) {
            writer.writeEightByteIntegerLow(this.rowCount);
        }
        else {
            writer.writeFourByteIntegerLow((int)this.rowCount);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -2;
    }
    
    @Override
    public String getTokenTypeName() {
        return "DoneProc";
    }
    
    public boolean isStatusFinal() {
        return !this.statusMore && !this.statusError && !this.statusInXact && !this.statusCount && !this.statusSrvError;
    }
    
    public void setStatusFinal(final boolean statusFinal) {
        this.statusMore = false;
        this.statusError = false;
        this.statusInXact = false;
        this.statusCount = false;
        this.statusSrvError = false;
    }
    
    public boolean isStatusMore() {
        return this.statusMore;
    }
    
    public void setStatusMore(final boolean statusMore) {
        this.statusMore = statusMore;
    }
    
    public boolean isStatusError() {
        return this.statusError;
    }
    
    public void setStatusError(final boolean statusError) {
        this.statusError = statusError;
    }
    
    public boolean isStatusInXact() {
        return this.statusInXact;
    }
    
    public void setStatusInXact(final boolean statusInXact) {
        this.statusInXact = statusInXact;
    }
    
    public boolean isStatusCount() {
        return this.statusCount;
    }
    
    public void setStatusCount(final boolean statusCount) {
        this.statusCount = statusCount;
    }
    
    public boolean isStatusRpcInBatch() {
        return this.statusRpcInBatch;
    }
    
    public void setStatusRpcInBatch(final boolean statusRpcInBatch) {
        this.statusRpcInBatch = statusRpcInBatch;
    }
    
    public boolean isStatusSrvError() {
        return this.statusSrvError;
    }
    
    public void setStatusSrvError(final boolean statusSrvError) {
        this.statusSrvError = statusSrvError;
    }
    
    public int getCurCmd() {
        return this.curCmd;
    }
    
    public void setCurCmd(final int curCmd) {
        this.curCmd = curCmd;
    }
    
    public long getRowCount() {
        return this.rowCount;
    }
    
    public void setRowCount(final long rowCount) {
        this.rowCount = rowCount;
    }
}

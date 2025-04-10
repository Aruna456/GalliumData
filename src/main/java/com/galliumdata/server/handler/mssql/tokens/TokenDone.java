// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.util.List;
import java.util.Arrays;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class TokenDone extends MessageToken
{
    private boolean doneFinal;
    private boolean doneMore;
    private boolean doneError;
    private boolean doneInXact;
    private boolean doneCount;
    private boolean doneAttn;
    private boolean doneSrvError;
    private int curCmd;
    private long rowCount;
    private boolean useShortRowCount;
    
    public TokenDone(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final short status = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.doneFinal = (status == 0);
        this.doneMore = ((status & 0x1) > 0);
        this.doneError = ((status & 0x2) > 0);
        this.doneInXact = ((status & 0x4) > 0);
        this.doneCount = ((status & 0x10) > 0);
        this.doneAttn = ((status & 0x20) > 0);
        this.doneSrvError = ((status & 0x100) > 0);
        this.curCmd = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (this.connectionState.tdsVersion72andHigher() && !this.useShortRowCount) {
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
        this.doneFinal = (status == 0);
        this.doneMore = ((status & 0x1) > 0);
        this.doneError = ((status & 0x2) > 0);
        this.doneInXact = ((status & 0x4) > 0);
        this.doneCount = ((status & 0x10) > 0);
        this.doneAttn = ((status & 0x20) > 0);
        this.doneSrvError = ((status & 0x100) > 0);
        this.curCmd = reader.readTwoByteIntLow();
        if (this.connectionState.tdsVersion72andHigher() && !this.useShortRowCount) {
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
        if (this.connectionState.tdsVersion72andHigher() && !this.useShortRowCount) {
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
        if (this.doneMore) {
            status |= 0x1;
        }
        if (this.doneError) {
            status |= 0x2;
        }
        if (this.doneInXact) {
            status |= 0x4;
        }
        if (this.doneCount) {
            status |= 0x10;
        }
        if (this.doneAttn) {
            status |= 0x20;
        }
        if (this.doneSrvError) {
            status |= 0x100;
        }
        if (this.doneFinal) {
            status = 0;
        }
        writer.writeTwoByteIntegerLow(status);
        writer.writeTwoByteIntegerLow((short)this.curCmd);
        if (this.connectionState.tdsVersion72andHigher() && !this.useShortRowCount) {
            writer.writeEightByteIntegerLow(this.rowCount);
        }
        else {
            writer.writeFourByteIntegerLow((int)this.rowCount);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -3;
    }
    
    @Override
    public String getTokenTypeName() {
        return "Done";
    }
    
    @Override
    public String toString() {
        return "Done: #rows " + this.rowCount + (this.doneError ? " - error" : "");
    }
    
    public boolean isDoneFinal() {
        return this.doneFinal;
    }
    
    public void setDoneFinal(final boolean doneFinal) {
        this.doneFinal = doneFinal;
    }
    
    public boolean isDoneMore() {
        return this.doneMore;
    }
    
    public void setDoneMore(final boolean doneMore) {
        this.doneMore = doneMore;
    }
    
    public boolean isDoneError() {
        return this.doneError;
    }
    
    public void setDoneError(final boolean doneError) {
        this.doneError = doneError;
    }
    
    public boolean isDoneInXact() {
        return this.doneInXact;
    }
    
    public void setDoneInXact(final boolean doneInXact) {
        this.doneInXact = doneInXact;
    }
    
    public boolean isDoneCount() {
        return this.doneCount;
    }
    
    public void setDoneCount(final boolean doneCount) {
        this.doneCount = doneCount;
    }
    
    public boolean isDoneAttn() {
        return this.doneAttn;
    }
    
    public void setDoneAttn(final boolean doneAttn) {
        this.doneAttn = doneAttn;
    }
    
    public boolean isDoneSrvError() {
        return this.doneSrvError;
    }
    
    public void setDoneSrvError(final boolean doneSrvError) {
        this.doneSrvError = doneSrvError;
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
    
    public void setUseShortRowCount(final boolean useShortRowCount) {
        this.useShortRowCount = useShortRowCount;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "doneFinal": {
                return this.doneFinal;
            }
            case "doneMore": {
                return this.doneMore;
            }
            case "doneError": {
                return this.doneError;
            }
            case "doneInXact": {
                return this.doneInXact;
            }
            case "doneCount": {
                return this.doneCount;
            }
            case "doneAttn": {
                return this.doneAttn;
            }
            case "doneSrvError": {
                return this.doneSrvError;
            }
            case "curCmd": {
                return this.curCmd;
            }
            case "rowCount": {
                return this.rowCount;
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        final String[] parentKeys = (String[])super.getMemberKeys();
        final List<String> keys = Arrays.asList(parentKeys);
        keys.add("doneFinal");
        keys.add("doneMore");
        keys.add("doneError");
        keys.add("doneInXact");
        keys.add("doneCount");
        keys.add("doneAttn");
        keys.add("doneSrvError");
        keys.add("curCmd");
        keys.add("rowCount");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "doneFinal":
            case "doneMore":
            case "doneError":
            case "doneInXact":
            case "doneCount":
            case "doneAttn":
            case "doneSrvError":
            case "curCmd":
            case "rowCount": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "doneFinal": {
                this.setDoneFinal(value.asBoolean());
                break;
            }
            case "doneMore": {
                this.setDoneMore(value.asBoolean());
                break;
            }
            case "doneError": {
                this.setDoneError(value.asBoolean());
                break;
            }
            case "doneInXact": {
                this.setDoneInXact(value.asBoolean());
                break;
            }
            case "doneCount": {
                this.setDoneCount(value.asBoolean());
                break;
            }
            case "doneAttn": {
                this.setDoneAttn(value.asBoolean());
                break;
            }
            case "doneSrvError": {
                this.setDoneSrvError(value.asBoolean());
                break;
            }
            case "curCmd": {
                this.setCurCmd(value.asInt());
                break;
            }
            case "rowCount": {
                this.setRowCount(value.asLong());
                break;
            }
            default: {
                super.putMember(key, value);
                break;
            }
        }
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "Done token" });
    }
}

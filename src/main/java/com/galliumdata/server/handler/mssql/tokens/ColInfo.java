// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import org.graalvm.polyglot.proxy.ProxyObject;

public class ColInfo implements ProxyObject
{
    private byte colNum;
    private byte tableNum;
    private boolean statusExpression;
    private boolean statusKey;
    private boolean statusHidden;
    private boolean statusDifferentName;
    private String colName;
    
    public void read(final RawPacketReader reader) {
        this.colNum = reader.readByte();
        this.tableNum = reader.readByte();
        final byte status = reader.readByte();
        this.statusExpression = ((status & 0x4) != 0x0);
        this.statusKey = ((status & 0x8) != 0x0);
        this.statusHidden = ((status & 0x10) != 0x0);
        this.statusDifferentName = ((status & 0x20) != 0x0);
        if (this.statusDifferentName) {
            final byte nameLen = reader.readByte();
            this.colName = reader.readString(nameLen * 2);
        }
    }
    
    public int getSerializedSize() {
        int size = 3;
        if (this.statusDifferentName) {
            size = ++size + this.colName.length() * 2;
        }
        return size;
    }
    
    public void write(final RawPacketWriter writer) {
        writer.writeByte(this.colNum);
        writer.writeByte(this.tableNum);
        byte status = 0;
        if (this.statusExpression) {
            status |= 0x4;
        }
        if (this.statusKey) {
            status |= 0x8;
        }
        if (this.statusHidden) {
            status |= 0x10;
        }
        if (this.statusDifferentName) {
            status |= 0x20;
        }
        writer.writeByte(status);
        if (this.statusDifferentName) {
            final byte[] nameBytes = this.colName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(nameBytes.length / 2));
            writer.writeBytes(nameBytes, 0, nameBytes.length);
        }
    }
    
    @Override
    public String toString() {
        final String s = "Column info for #" + this.colNum;
        return s;
    }
    
    public byte getColNum() {
        return this.colNum;
    }
    
    public void setColNum(final byte colNum) {
        this.colNum = colNum;
    }
    
    public byte getTableNum() {
        return this.tableNum;
    }
    
    public void setTableNum(final byte tableNum) {
        this.tableNum = tableNum;
    }
    
    public boolean isStatusExpression() {
        return this.statusExpression;
    }
    
    public void setStatusExpression(final boolean statusExpression) {
        this.statusExpression = statusExpression;
    }
    
    public boolean isStatusKey() {
        return this.statusKey;
    }
    
    public void setStatusKey(final boolean statusKey) {
        this.statusKey = statusKey;
    }
    
    public boolean isStatusHidden() {
        return this.statusHidden;
    }
    
    public void setStatusHidden(final boolean statusHidden) {
        this.statusHidden = statusHidden;
    }
    
    public boolean isStatusDifferentName() {
        return this.statusDifferentName;
    }
    
    public void setStatusDifferentName(final boolean statusDifferentName) {
        this.statusDifferentName = statusDifferentName;
    }
    
    public String getColName() {
        return this.colName;
    }
    
    public void setColName(final String colName) {
        this.colName = colName;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "colNum": {
                return this.colNum;
            }
            case "tableNum": {
                return this.tableNum;
            }
            case "statusExpression": {
                return this.statusExpression;
            }
            case "statusKey": {
                return this.statusKey;
            }
            case "statusHidden": {
                return this.statusHidden;
            }
            case "statusDifferentName": {
                return this.statusDifferentName;
            }
            case "colName": {
                return this.colName;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "colNum", "tableNum", "statusExpression", "statusKey", "statusHidden", "statusDifferentName", "colName" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "colNum":
            case "tableNum":
            case "statusExpression":
            case "statusKey":
            case "statusHidden":
            case "statusDifferentName":
            case "colName": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "colNum": {
                this.setColNum(value.asByte());
                return;
            }
            case "tableNum": {
                this.setTableNum(value.asByte());
                return;
            }
            case "statusExpression": {
                this.setStatusExpression(value.asBoolean());
                return;
            }
            case "statusKey": {
                this.setStatusKey(value.asBoolean());
                return;
            }
            case "statusHidden": {
                this.setStatusHidden(value.asBoolean());
                return;
            }
            case "statusDifferentName": {
                this.setStatusDifferentName(value.asBoolean());
                return;
            }
            case "colName": {
                this.setColName(value.asString());
                break;
            }
        }
        throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "ColInfo token" });
    }
}

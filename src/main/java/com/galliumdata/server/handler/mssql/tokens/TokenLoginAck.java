// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class TokenLoginAck extends MessageToken
{
    private byte interfaceType;
    private int tdsVersion;
    private String progName;
    private byte majorVersion;
    private byte minorVersion;
    private short buildNumber;
    
    public TokenLoginAck(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int length = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.interfaceType = bytes[idx];
        ++idx;
        this.tdsVersion = DataTypeReader.readFourByteInteger(bytes, idx);
        idx += 4;
        switch (this.tdsVersion) {
            case 117440512:
            case 117506048: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(0);
                break;
            }
            case 1895825409: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(1);
                break;
            }
            case 1913192450: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(2);
                break;
            }
            case 1930035203:
            case 1930100739: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(3);
                break;
            }
            case 1946157060: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(4);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.TDSVersionNotSupported", new Object[] { this.tdsVersion });
            }
        }
        final byte progNameSize = bytes[idx];
        ++idx;
        this.progName = new String(bytes, idx, progNameSize * 2, StandardCharsets.UTF_16LE);
        idx += progNameSize * 2;
        this.majorVersion = bytes[idx];
        ++idx;
        this.minorVersion = bytes[idx];
        ++idx;
        this.buildNumber = DataTypeReader.readTwoByteInteger(bytes, idx);
        idx += 2;
        if (idx - offset - 3 != length) {
            throw new ServerException("db.mssql.protocol.UnexpectedTokenSize", new Object[] { this.getTokenTypeName(), length, idx - offset - 3 });
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readTwoByteIntLow();
        this.interfaceType = reader.readByte();
        switch (this.tdsVersion = reader.readFourByteInt()) {
            case 117440512:
            case 117506048: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(0);
                break;
            }
            case 1895825409: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(1);
                break;
            }
            case 1913192450: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(2);
                break;
            }
            case 1930035203:
            case 1930100739: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(3);
                break;
            }
            case 1946157060: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(4);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.TDSVersionNotSupported", new Object[] { this.tdsVersion });
            }
        }
        final byte progNameSize = reader.readByte();
        this.progName = reader.readString(progNameSize * 2);
        this.majorVersion = reader.readByte();
        this.minorVersion = reader.readByte();
        this.buildNumber = reader.readTwoByteInt();
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        ++size;
        size += 4;
        size = ++size + this.progName.length() * 2;
        ++size;
        ++size;
        size += 2;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeTwoByteIntegerLow((short)(this.getSerializedSize() - 3));
        writer.writeByte(this.interfaceType);
        writer.writeFourByteInteger(this.tdsVersion);
        final byte[] strBytes = this.progName.getBytes(StandardCharsets.UTF_16LE);
        writer.writeByte((byte)(strBytes.length / 2));
        writer.writeBytes(strBytes, 0, strBytes.length);
        writer.writeByte(this.majorVersion);
        writer.writeByte(this.minorVersion);
        writer.writeTwoByteInteger(this.buildNumber);
    }
    
    @Override
    public byte getTokenType() {
        return -83;
    }
    
    @Override
    public String getTokenTypeName() {
        return "LoginAck";
    }
    
    @Override
    public String toString() {
        return "LoginAck, TDS v." + this.getTdsVersionName() + ", program " + this.progName + " v." + this.majorVersion + "." + this.minorVersion;
    }
    
    public byte getInterfaceType() {
        return this.interfaceType;
    }
    
    public void setInterfaceType(final byte interfaceType) {
        this.interfaceType = interfaceType;
    }
    
    public int getTdsVersion() {
        return this.tdsVersion;
    }
    
    public String getTdsVersionName() {
        switch (this.tdsVersion) {
            case 117440512: {
                return "SQL Server 7.0";
            }
            case 117506048: {
                return "SQL Server 2000";
            }
            case 1895825409: {
                return "SQL Server 2000 SP1";
            }
            case 1913192450: {
                return "SQL Server 2005";
            }
            case 1930035203: {
                return "SQL Server 2008";
            }
            case 1930100739: {
                return "SQL Server 2008 R2";
            }
            case 1946157060: {
                return "SQL Server 2012, 2014, 2016, 2017 or 2019";
            }
            default: {
                return "Unknown: " + this.tdsVersion;
            }
        }
    }
    
    public void setTdsVersion(final int tdsVersion) {
        this.tdsVersion = tdsVersion;
    }
    
    public String getProgName() {
        return this.progName;
    }
    
    public void setProgName(final String progName) {
        this.progName = progName;
    }
    
    public byte getMajorVersion() {
        return this.majorVersion;
    }
    
    public void setMajorVersion(final byte majorVersion) {
        this.majorVersion = majorVersion;
    }
    
    public byte getMinorVersion() {
        return this.minorVersion;
    }
    
    public void setMinorVersion(final byte minorVersion) {
        this.minorVersion = minorVersion;
    }
    
    public short getBuildNumber() {
        return this.buildNumber;
    }
    
    public void setBuildNumber(final short buildNumber) {
        this.buildNumber = buildNumber;
    }
}

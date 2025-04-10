// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;

public class TransactionManagerRequest extends MSSQLPacket
{
    private long transactionDescriptor;
    private int outstandingRequestCount;
    private short requestType;
    public static final int TM_GET_DTC_ADDRESS = 0;
    public static final int TM_PROPAGATE_XACT = 1;
    public static final int TM_BEGIN_XACT = 5;
    public static final int TM_PROMOTE_XACT = 6;
    public static final int TM_COMMIT_XACT = 7;
    public static final int TM_ROLLBACK_XACT = 8;
    public static final int TM_SAVE_XACT = 9;
    private byte[] propagateBuffer;
    private byte isolationLevel;
    private byte[] beginTransactionName;
    private byte[] commitTransactionName;
    private boolean commitTransactionBeginTransaction;
    private Byte commitTransactionIsolationLevel;
    private byte[] saveTransactionName;
    private byte[] rawBytes;
    
    public TransactionManagerRequest(final ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = 14;
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        System.arraycopy(bytes, idx, this.rawBytes = new byte[this.length - 8], 0, this.length - 8);
        idx += this.length - 8;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        if (this.rawBytes != null) {
            return size + this.rawBytes.length;
        }
        size += 22;
        size += 2;
        switch (this.requestType) {
            case 0: {
                size += 2;
                break;
            }
            case 1: {
                size += 2;
                if (this.propagateBuffer != null && this.propagateBuffer.length > 0) {
                    size += this.propagateBuffer.length;
                    break;
                }
                break;
            }
            case 5: {
                ++size;
                ++size;
                if (this.beginTransactionName != null) {
                    size += this.beginTransactionName.length;
                    break;
                }
                break;
            }
            case 7:
            case 8: {
                ++size;
                if (this.commitTransactionName != null && this.commitTransactionName.length > 0) {
                    size += this.commitTransactionName.length;
                }
                ++size;
                if (this.commitTransactionIsolationLevel == null) {
                    break;
                }
                ++size;
                ++size;
                if (this.beginTransactionName != null && this.beginTransactionName.length > 0) {
                    size += this.beginTransactionName.length;
                    break;
                }
                break;
            }
            case 9: {
                ++size;
                if (this.saveTransactionName != null && this.saveTransactionName.length > 0) {
                    size += this.saveTransactionName.length;
                    break;
                }
                break;
            }
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.rawBytes != null) {
            writer.writeBytes(this.rawBytes, 0, this.rawBytes.length);
            return;
        }
        writer.writeFourByteIntegerLow(22);
        writer.writeFourByteIntegerLow(18);
        writer.writeTwoByteIntegerLow(2);
        writer.writeEightByteNumber(this.transactionDescriptor);
        writer.writeFourByteIntegerLow(this.outstandingRequestCount);
        writer.writeTwoByteIntegerLow(this.requestType);
        switch (this.requestType) {
            case 0: {
                writer.writeTwoByteIntegerLow(0);
                break;
            }
            case 1: {
                if (this.propagateBuffer == null || this.propagateBuffer.length == 0) {
                    writer.writeTwoByteIntegerLow(0);
                    break;
                }
                writer.writeTwoByteIntegerLow(this.propagateBuffer.length);
                writer.writeBytes(this.propagateBuffer, 0, this.propagateBuffer.length);
                break;
            }
            case 5: {
                writer.writeByte(this.isolationLevel);
                if (this.beginTransactionName == null || this.beginTransactionName.length == 0) {
                    writer.writeByte((byte)0);
                    break;
                }
                writer.writeByte((byte)this.beginTransactionName.length);
                writer.writeBytes(this.beginTransactionName, 0, this.beginTransactionName.length);
                break;
            }
            case 6: {
                break;
            }
            case 7:
            case 8: {
                if (this.commitTransactionName == null || this.commitTransactionName.length == 0) {
                    writer.writeByte((byte)0);
                }
                else {
                    writer.writeByte((byte)this.commitTransactionName.length);
                    writer.writeBytes(this.commitTransactionName, 0, this.commitTransactionName.length);
                }
                byte flags = 0;
                if (this.commitTransactionBeginTransaction) {
                    flags |= 0x1;
                }
                writer.writeByte(flags);
                if (this.commitTransactionIsolationLevel == null) {
                    break;
                }
                writer.writeByte(this.commitTransactionIsolationLevel);
                if (this.beginTransactionName == null || this.beginTransactionName.length == 0) {
                    writer.writeByte((byte)0);
                    break;
                }
                writer.writeByte((byte)this.beginTransactionName.length);
                writer.writeBytes(this.beginTransactionName, 0, this.beginTransactionName.length);
                break;
            }
            case 9: {
                if (this.saveTransactionName == null || this.saveTransactionName.length == 0) {
                    writer.writeByte((byte)0);
                    break;
                }
                writer.writeByte((byte)this.saveTransactionName.length);
                writer.writeBytes(this.saveTransactionName, 0, this.saveTransactionName.length);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownTxReqType", new Object[] { this.requestType });
            }
        }
    }
    
    @Override
    public String getPacketType() {
        return "TransactionManagerRequest";
    }
    
    @Override
    public String toString() {
        String s = "Tx request";
        if (this.rawBytes != null) {
            return s + " - raw";
        }
        switch (this.requestType) {
            case 0: {
                s += " - GET_DTC_ADDRESS";
                break;
            }
            case 1: {
                s += " - PROPAGATE_XACT";
                break;
            }
            case 5: {
                s += " - BEGIN_XACT";
                break;
            }
            case 6: {
                s += " - PROMOTE_XACT";
                break;
            }
            case 7: {
                s += " - COMMIT_XACT";
                break;
            }
            case 8: {
                s += " - ROLLBACK_XACT";
                break;
            }
            case 9: {
                s += " - SAVE_XACT";
                break;
            }
            default: {
                s = s + " - unknown type " + this.requestType;
                break;
            }
        }
        return s;
    }
}

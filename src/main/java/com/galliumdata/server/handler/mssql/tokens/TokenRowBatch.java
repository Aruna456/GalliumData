// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import java.util.NoSuchElementException;
import org.graalvm.polyglot.proxy.ProxyIterator;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.js.JSListWrapper;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.ConnectionState;
import java.util.List;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;

public class TokenRowBatch extends MessageToken implements ProxyObject, ProxyArray
{
    private final List<TokenRow> packets;
    private final int maxRows;
    private final int maxBytes;
    private int totalBytes;
    
    public TokenRowBatch(final int maxRows, final int maxBytes) {
        super(null);
        this.packets = new ArrayList<TokenRow>();
        this.maxRows = maxRows;
        this.maxBytes = maxBytes;
    }
    
    public TokenRowBatch(final ConnectionState connectionState) {
        super(connectionState);
        this.packets = new ArrayList<TokenRow>();
        throw new RuntimeException("");
    }
    
    public void addRow(final TokenRow pkt) {
        this.packets.add(pkt);
        this.totalBytes += pkt.getSerializedSize();
    }
    
    public void insertRow(final TokenRow pkt, final int idx) {
        this.packets.add(idx, pkt);
        this.totalBytes += pkt.getSerializedSize();
    }
    
    public void insertRowBefore(final TokenRow pkt, final TokenRow pkt2) {
        final int idx = this.packets.indexOf(pkt2);
        if (idx == -1) {
            return;
        }
        this.packets.add(idx, pkt);
        this.totalBytes += pkt.getSerializedSize();
    }
    
    public void insertRowAfter(final TokenRow pkt, final TokenRow pkt2) {
        final int idx = this.packets.indexOf(pkt2);
        if (idx == -1) {
            return;
        }
        this.packets.add(idx + 1, pkt);
        this.totalBytes += pkt.getSerializedSize();
    }
    
    @Override
    public byte getTokenType() {
        return -1;
    }
    
    @Override
    public String getTokenTypeName() {
        return "RowBatch";
    }
    
    public List<TokenRow> getRows() {
        return this.packets;
    }
    
    public void removeRow(final TokenRow pkt) {
        final int idx = this.packets.indexOf(pkt);
        if (this.packets.remove(pkt)) {
            this.totalBytes -= pkt.getSerializedSize();
        }
    }
    
    public void removeRow(final int idx) {
        final TokenRow pkt = this.packets.get(idx);
        if (pkt != null) {
            this.packets.remove(idx);
            this.totalBytes -= pkt.getSerializedSize();
        }
    }
    
    public void clear() {
        this.packets.clear();
        this.totalBytes = 0;
    }
    
    public int getNumberOfRows() {
        return this.packets.size();
    }
    
    public boolean batchIsFull() {
        return (this.maxRows > 0 && this.packets.size() >= this.maxRows) || (this.maxBytes > 0 && this.totalBytes >= this.maxBytes);
    }
    
    public void writeOut(final RawPacketWriter rawWriter) {
        if (this.packets.isEmpty()) {
            return;
        }
        this.packets.forEach(p -> {
            if (!p.isRemoved()) {
                p.write(rawWriter);
            }
            return;
        });
        this.packets.clear();
        this.totalBytes = 0;
    }
    
    private void recomputeByteTotal() {
        this.totalBytes = 0;
        this.packets.forEach(p -> this.totalBytes += p.getSerializedSize());
    }
    
    @Override
    public String toString() {
        String desc = "";
        if (!this.packets.isEmpty()) {
            final TokenRow row = this.packets.get(0);
            int colNum = 0;
            for (ColumnMetadata d : row.getColumnMetadata()) {
                if (!desc.isBlank()) {
                    desc = desc;
                }
                else {
                    desc = "[";
                }
                if (!d.getTableName().isEmpty()) {
                    desc = desc + "[" + String.valueOf(d.getTableName()) + "].";
                }
                final String colName = d.getColumnName();
                if (colName == null || colName.trim().isBlank()) {
                    desc += "<no name>";
                }
                else {
                    desc += colName;
                }
                if (++colNum > 10) {
                    break;
                }
                if (desc.length() > 60) {
                    break;
                }
            }
            if (row.getColumnMetadata().size() > colNum) {
                desc = desc + "," + (row.getColumnMetadata().size() - colNum) + " more";
            }
            desc += "] - ";
        }
        return "DataRowBatch - " + desc + this.packets.size() + " row" + ((this.packets.size() > 1) ? "s" : "") + ", " + this.totalBytes + " bytes";
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "rows": {
                return new JSListWrapper(this.packets, this::recomputeByteTotal);
            }
            case "numberOfRows": {
                return this.getNumberOfRows();
            }
            case "addRow": {
                return (Function<Value[],Object>) arguments -> {
                    this.addRow((TokenRow)arguments[0].asProxyObject());
                    return null;
                };
            }
            case "insertRow": {
                return (Function<Value[],Object>) arguments -> {
                    if (arguments.length != 2) {
                        throw new ServerException("db.mssql.logic.InvalidNumParams", new Object[] { "insertRow", 2 });
                    }
                    this.insertRow((TokenRow)arguments[0].asProxyObject(), arguments[1].asInt());
                    return null;
                };
            }
            case "insertRowBefore": {
                return (Function<Value[],Object>) arguments -> {
                    if (arguments.length != 2) {
                        throw new ServerException("db.mssql.logic.InvalidNumParams", new Object[] { "insertRowBefore", 2 });
                    }
                    this.insertRowBefore((TokenRow)arguments[0].asProxyObject(), (TokenRow)arguments[1].asProxyObject());
                    return null;
                };
            }
            case "insertRowAfter": {
                return (Function<Value[],Object>) arguments -> {
                    if (arguments.length != 2) {
                        throw new ServerException("db.mssql.logic.InvalidNumParams", new Object[] { "insertRowAfter", 2 });
                    }
                    this.insertRowAfter((TokenRow)arguments[0].asProxyObject(), (TokenRow)arguments[1].asProxyObject());
                    return null;
                };
            }
            case "removeRow": {
                return (Function<Value[],Object>) arguments -> {
                    if (arguments[0].isNumber()) {
                        this.removeRow(arguments[0].asInt());
                    }
                    else {
                        this.removeRow((TokenRow)arguments[0].asProxyObject());
                    }
                    return null;
                };
            }
            case "clear": {
                return (Function<Value[],Object>) arguments -> {
                    this.clear();
                    return null;
                };
            }
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key, this.getPacketType() });
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return new String[] { "rows", "numberOfRows", "addRow", "insertRow", "insertRowBefore", "insertRowAfter", "removeRow", "clear", "toString" };
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "rows":
            case "numberOfRows":
            case "addRow":
            case "insertRow":
            case "insertRowBefore":
            case "insertRowAfter":
            case "removeRow":
            case "clear":
            case "toString": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key, "TokenRowBatch" });
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "TokenRowBatch" });
    }
    
    public Object get(final long index) {
        return this.packets.get((int)index);
    }
    
    public void set(final long index, final Value value) {
        this.packets.set((int)index, (TokenRow)value.asHostObject());
    }
    
    public boolean remove(final long index) {
        return this.packets.remove((int)index) != null;
    }
    
    public long getSize() {
        return this.packets.size();
    }
    
    public Object getIterator() {
        return new BatchIterator();
    }
    
    private class BatchIterator implements ProxyIterator
    {
        private int curPos;
        
        private BatchIterator() {
            this.curPos = -1;
        }
        
        public boolean hasNext() {
            return TokenRowBatch.this.packets.size() > this.curPos;
        }
        
        public Object getNext() throws NoSuchElementException, UnsupportedOperationException {
            ++this.curPos;
            if (TokenRowBatch.this.packets.size() <= this.curPos) {
                throw new NoSuchElementException();
            }
            return TokenRowBatch.this.packets.get(this.curPos);
        }
    }
}

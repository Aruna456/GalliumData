// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import java.util.Iterator;
import com.galliumdata.server.util.StringUtil;
import java.util.Arrays;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenError;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import java.util.List;
import java.util.function.Function;

import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;
import org.graalvm.polyglot.proxy.ProxyObject;

public class MSSQLResultSet implements ProxyObject
{
    private TokenColMetadata meta;
    private List<TokenRow> rows;
    private TokenError error;
    private static final int MAX_COL_WIDTH = 40;
    
    public MSSQLResultSet(final TokenColMetadata meta, final List<TokenRow> rows) {
        this.meta = meta;
        this.rows = rows;
    }
    
    public MSSQLResultSet(final TokenError error) {
        this.error = error;
    }
    
    public TokenColMetadata getMetaData() {
        return this.meta;
    }
    
    public List<TokenRow> getRows() {
        return this.rows;
    }
    
    public TokenError getError() {
        return this.error;
    }
    
    @Override
    public String toString() {
        if (this.error != null) {
            return "Error: " + this.error.getMessage();
        }
        final StringBuilder sb = new StringBuilder();
        final List<ColumnMetadata> cols = this.meta.getColumns();
        final int[] colWidths = new int[cols.size()];
        for (int i = 0; i < cols.size(); ++i) {
            String colName = cols.get(i).getColumnName();
            if (colName == null || colName.isBlank()) {
                colName = "<empty>";
            }
            if (colName.length() > 40) {
                colWidths[i] = 40;
            }
            else {
                colWidths[i] = colName.length();
            }
        }
        for (final TokenRow row : this.rows) {
            for (int j = 0; j < cols.size(); ++j) {
                final ColumnMetadata colMeta = this.meta.getColumns().get(j);
                final Object value = row.getValue(colMeta.getColumnName());
                int valueWidth;
                if (value == null) {
                    valueWidth = "<null>".length();
                }
                else {
                    valueWidth = value.toString().length();
                    if (valueWidth > 40) {
                        valueWidth = 40;
                    }
                }
                if (colWidths[j] < valueWidth) {
                    colWidths[j] = valueWidth;
                }
            }
        }
        final int tableWidth = 1 + Arrays.stream(colWidths).sum() + cols.size() * 3;
        final String dashRow = "-".repeat(tableWidth);
        sb.append(dashRow);
        sb.append("\n");
        sb.append("|");
        for (int j = 0; j < cols.size(); ++j) {
            String colName2 = cols.get(j).getColumnName();
            if (colName2 == null || colName2.isBlank()) {
                colName2 = "<empty>";
            }
            if (colName2.length() > 40) {
                colName2 = StringUtil.getShortenedString(colName2, 40);
            }
            sb.append(" ");
            sb.append(colName2);
            final int padLen = colWidths[j] - colName2.length();
            if (padLen > 0) {
                sb.append(" ".repeat(padLen));
            }
            sb.append(" |");
        }
        sb.append("\n");
        sb.append(dashRow);
        sb.append("\n");
        for (final TokenRow row2 : this.rows) {
            sb.append("|");
            for (int k = 0; k < cols.size(); ++k) {
                final String colName3 = cols.get(k).getColumnName();
                final Object value2 = row2.getValue(colName3);
                String valueStr;
                if (value2 == null) {
                    valueStr = "<null>";
                }
                else {
                    valueStr = value2.toString();
                    if (valueStr.length() > 40) {
                        valueStr = StringUtil.getShortenedString(valueStr, 40);
                    }
                }
                sb.append(" ");
                sb.append(valueStr);
                final int padLen2 = colWidths[k] - valueStr.length();
                if (padLen2 > 0) {
                    sb.append(" ".repeat(padLen2));
                }
                sb.append(" |");
            }
            sb.append("\n");
        }
        sb.append(dashRow);
        sb.append("\n");
        return sb.toString();
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "metadata": {
                return this.meta;
            }
            case "rows": {
                return this.rows;
            }
            case "error": {
                return this.error;
            }
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "metadata", "rows", "error", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "metadata":
            case "rows":
            case "error":
            case "toString": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public void putMember(final String key, final Value value) {
        throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "Token token" });
    }
}

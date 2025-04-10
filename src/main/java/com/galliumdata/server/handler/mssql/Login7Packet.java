// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureUnknown;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureAzureSQLDNSCaching;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureUTF8Support;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureDataClassification;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureGlobalTransactions;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureColumnEncryption;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureSessionRecovery;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.ServerException;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7Feature;
import java.util.List;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyObject;

public class Login7Packet extends MSSQLPacket implements ProxyObject
{
    private int tdsVersion;
    private int tdsRevision;
    private int packetSize;
    private byte[] clientProgramVersion;
    private long clientPid;
    private long connectionPid;
    private boolean fByteOrder;
    private boolean fChar;
    private byte fFloat;
    private boolean fDumpLoad;
    private boolean fUseDB;
    private boolean fDatabase;
    private boolean fSetLang;
    private boolean fLanguage;
    private boolean fODBC;
    private boolean fTranBoundary;
    private boolean fCacheConnect;
    private byte fUserType;
    private boolean fIntSecurity;
    private byte fSQLType;
    private boolean fOLEDB;
    private boolean fReadOnlyIntent;
    private boolean fChangePassword;
    private boolean fUserInstance;
    private boolean fSendYukonBinaryXML;
    private boolean fUnknownCollationHandling;
    private boolean fExtension;
    private int clientTimeZone;
    private int clientLcid;
    private String hostname;
    private String username;
    private String password;
    private String appName;
    private String serverName;
    private int extensionOffset;
    private boolean extensionsInline;
    private String clientName;
    private String language;
    private String database;
    private byte[] clientId;
    private byte[] sspi;
    private String attachDbFile;
    private String changePassword;
    private List<Login7Feature> features;
    
    public Login7Packet(final ConnectionState connectionState) {
        super(connectionState);
        this.clientProgramVersion = new byte[4];
        this.features = new ArrayList<Login7Feature>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        final int loginLength = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.tdsVersion = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        switch (this.tdsVersion) {
            case 117440512:
            case 117506048:
            case 1879048192: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(0);
                break;
            }
            case 1895825408: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(1);
                break;
            }
            case 1895825409: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(1);
                this.connectionState.setTdsRevision(1);
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
        this.packetSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        System.arraycopy(bytes, idx, this.clientProgramVersion, 0, 4);
        idx += 4;
        this.clientPid = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.connectionPid = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        final byte options1 = bytes[idx];
        ++idx;
        this.fByteOrder = ((options1 & 0x1) != 0x0);
        this.fChar = ((options1 & 0x2) != 0x0);
        this.fFloat = (byte)((options1 & 0xC) >> 2);
        this.fDumpLoad = ((options1 & 0x10) != 0x0);
        this.fUseDB = ((options1 & 0x20) != 0x0);
        this.fDatabase = ((options1 & 0x40) != 0x0);
        this.fSetLang = ((options1 & 0xFFFFFF80) != 0x0);
        final byte options2 = bytes[idx];
        ++idx;
        this.fLanguage = ((options2 & 0x1) != 0x0);
        this.fODBC = ((options2 & 0x2) != 0x0);
        this.fTranBoundary = ((options2 & 0x4) != 0x0);
        this.fCacheConnect = ((options2 & 0x8) != 0x0);
        this.fUserType = (byte)((options2 & 0x70) >> 4);
        this.fIntSecurity = ((options2 & 0x80) != 0x0);
        final byte typeFlags = bytes[idx];
        ++idx;
        this.fSQLType = (byte)(typeFlags & 0xF);
        this.fOLEDB = ((typeFlags & 0x10) != 0x0);
        this.fReadOnlyIntent = ((typeFlags & 0x20) != 0x0);
        final byte options3 = bytes[idx];
        ++idx;
        this.fChangePassword = ((options3 & 0x1) != 0x0);
        this.fSendYukonBinaryXML = ((options3 & 0x2) != 0x0);
        if (this.fSendYukonBinaryXML) {
            this.connectionState.setBinaryXml(true);
            Login7Packet.log.trace(Markers.MSSQL, "Login packet specifies that XML will be binary-encoded");
        }
        this.fUserInstance = ((options3 & 0x4) != 0x0);
        this.fUnknownCollationHandling = ((options3 & 0x8) != 0x0);
        this.fExtension = ((options3 & 0x10) != 0x0);
        this.clientTimeZone = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.clientLcid = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.hostname = this.readString(bytes, offset, idx);
        idx += 4;
        this.username = this.readString(bytes, offset, idx);
        idx += 4;
        int off = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        int len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (len > 0) {
            final byte[] passwordBytes = new byte[len * 2];
            System.arraycopy(bytes, offset + 8 + off, passwordBytes, 0, len * 2);
            for (int i = 0; i < passwordBytes.length; ++i) {
                final byte[] array = passwordBytes;
                final int n = i;
                array[n] ^= 0xFFFFFFA5;
                final byte lowBits = (byte)((passwordBytes[i] & 0xF) << 4);
                final byte highBits = (byte)((passwordBytes[i] & 0xF0) >> 4);
                passwordBytes[i] = (byte)(highBits | lowBits);
            }
            this.password = new String(passwordBytes, StandardCharsets.UTF_16LE);
        }
        this.appName = this.readString(bytes, offset, idx);
        idx += 4;
        this.serverName = this.readString(bytes, offset, idx);
        idx += 4;
        if (this.isfExtension()) {
            off = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
            len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
            this.extensionOffset = DataTypeReader.readFourByteIntegerLow(bytes, offset + 8 + off);
        }
        else {
            idx += 4;
        }
        final int clientNameIdx = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        if (this.extensionOffset <= clientNameIdx) {
            this.extensionsInline = true;
        }
        this.clientName = this.readString(bytes, offset, idx);
        idx += 4;
        this.language = this.readString(bytes, offset, idx);
        idx += 4;
        this.database = this.readString(bytes, offset, idx);
        idx += 4;
        System.arraycopy(bytes, idx, this.clientId = new byte[6], 0, 6);
        idx += 6;
        final int sspiOffset = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        final int sspiLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        final int attachDbFileIdx = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        this.attachDbFile = this.readString(bytes, offset, idx);
        idx += 4;
        if (this.fChangePassword) {
            final int changePasswordIdx = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            this.changePassword = this.readString(bytes, offset, changePasswordIdx);
        }
        idx += 4;
        int sspiLenLong = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (sspiLen != 0) {
            if (sspiLen < 32767) {
                System.arraycopy(bytes, offset + 8 + sspiOffset, this.sspi = new byte[sspiLen], 0, sspiLen);
            }
            else {
                if (sspiLenLong == 0) {
                    sspiLenLong = sspiLen;
                }
                System.arraycopy(bytes, offset + 8 + sspiOffset, this.sspi = new byte[sspiLenLong], 0, sspiLenLong);
            }
        }
        if (this.extensionOffset > 0) {
            for (idx = offset + 8 + this.extensionOffset; idx - offset - 8 < loginLength; idx += this.readFeature(bytes, idx)) {
                if (bytes[idx] == -1) {
                    ++idx;
                    break;
                }
            }
        }
        if (Login7Packet.log.isTraceEnabled()) {
            Login7Packet.log.trace(Markers.MSSQL, "Login as " + this.username + " - column-level encryption: " + this.connectionState.isColumnEncryptionInUse());
        }
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += this.hostname.length() * 2;
        size += 4;
        size += this.username.length() * 2;
        size += 4;
        if (this.password != null) {
            size += this.password.length() * 2;
        }
        size += 4;
        size += this.appName.length() * 2;
        size += 4;
        size += this.serverName.length() * 2;
        size += 4;
        size += 4;
        size += this.clientName.length() * 2;
        size += 4;
        size += this.language.length() * 2;
        size += 4;
        size += this.database.length() * 2;
        size += 6;
        size += 4;
        size += 4;
        size += this.attachDbFile.length() * 2;
        size += 4;
        if (this.changePassword != null) {
            size += this.changePassword.length() * 2;
        }
        size += 4;
        if (this.sspi != null) {
            size += this.sspi.length;
        }
        if (this.isfExtension()) {
            size += 4;
        }
        for (final Login7Feature feature : this.getFeatures()) {
            size += feature.getSerializedSize();
        }
        if (this.getFeatures().size() > 0 || this.isfExtension()) {
            ++size;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeFourByteIntegerLow(this.getSerializedSize() - 8);
        writer.writeFourByteIntegerLow(this.tdsVersion);
        writer.writeFourByteIntegerLow(this.packetSize);
        writer.writeBytes(this.clientProgramVersion, 0, 4);
        writer.writeFourByteIntegerLow((int)this.clientPid);
        writer.writeFourByteIntegerLow((int)this.connectionPid);
        byte options1 = 0;
        if (this.fByteOrder) {
            options1 |= 0x1;
        }
        if (this.fChar) {
            options1 |= 0x2;
        }
        options1 |= (byte)((this.fFloat & 0x3) << 2);
        if (this.fDumpLoad) {
            options1 |= 0x10;
        }
        if (this.fUseDB) {
            options1 |= 0x20;
        }
        if (this.fDatabase) {
            options1 |= 0x40;
        }
        if (this.fSetLang) {
            options1 |= 0xFFFFFF80;
        }
        writer.writeByte(options1);
        byte options2 = 0;
        if (this.fLanguage) {
            options2 |= 0x1;
        }
        if (this.fODBC) {
            options2 |= 0x2;
        }
        if (this.fTranBoundary) {
            options2 |= 0x4;
        }
        if (this.fCacheConnect) {
            options2 |= 0x8;
        }
        options2 |= (byte)((this.fUserType & 0x7) << 4);
        if (this.fIntSecurity) {
            options2 |= (byte)128;
        }
        writer.writeByte(options2);
        byte typeFlags = this.fSQLType;
        if (this.fOLEDB) {
            typeFlags |= 0x10;
        }
        if (this.fReadOnlyIntent) {
            typeFlags |= 0x20;
        }
        writer.writeByte(typeFlags);
        byte options3 = 0;
        if (this.fChangePassword) {
            options3 |= 0x1;
        }
        if (this.fSendYukonBinaryXML) {
            options3 |= 0x2;
        }
        if (this.fUserInstance) {
            options3 |= 0x4;
        }
        if (this.fUnknownCollationHandling) {
            options3 |= 0x8;
        }
        if (this.fExtension) {
            options3 |= 0x10;
        }
        writer.writeByte(options3);
        writer.writeFourByteIntegerLow(this.clientTimeZone);
        writer.writeFourByteIntegerLow(this.clientLcid);
        final int bufferOffset = writer.getPacket().getWriteIndex() - 8;
        final byte[] buffer = new byte[8000];
        final int hostnameIdx;
        int idx = hostnameIdx = 0;
        idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.hostname.length());
        idx += 2;
        int usernameIdx = idx;
        idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.username.length());
        idx += 2;
        int passwordIdx = idx;
        idx += 2;
        if (this.password != null) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.password.length());
            idx += 2;
        }
        else {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)0);
            idx += 2;
        }
        int appNameIdx = idx;
        idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.appName.length());
        idx += 2;
        int serverNameIdx = idx;
        idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.serverName.length());
        idx += 2;
        int extensionOffsetIdx = 0;
        if (this.isfExtension()) {
            extensionOffsetIdx = idx;
            idx += 2;
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)4);
            idx += 2;
        }
        else {
            buffer[idx + 1] = (buffer[idx] = 0);
            buffer[idx + 3] = (buffer[idx + 2] = 0);
            idx += 4;
        }
        int clientNameIdx = idx;
        idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.clientName.length());
        idx += 2;
        int languageIdx = idx;
        idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.language.length());
        idx += 2;
        int databaseIdx = idx;
        idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.database.length());
        idx += 2;
        System.arraycopy(this.clientId, 0, buffer, idx, 6);
        idx += 6;
        final int sspiIdx = idx;
        if (this.sspi == null) {
            buffer[idx + 1] = (buffer[idx] = 0);
            buffer[idx + 3] = (buffer[idx + 2] = 0);
            idx += 4;
        }
        else if (this.sspi.length < 32767) {
            idx += 2;
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.sspi.length);
            idx += 2;
        }
        else {
            idx += 2;
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)32767);
            idx += 2;
        }
        int attachDbFileIdx = idx;
        idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.attachDbFile.length());
        idx += 2;
        int changePasswordIdx = idx;
        idx += 2;
        if (this.changePassword != null) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.changePassword.length());
        }
        idx += 2;
        if (this.sspi == null || this.sspi.length < 32767) {
            DataTypeWriter.encodeFourByteIntegerLow(buffer, idx, 0);
            idx += 4;
        }
        else {
            DataTypeWriter.encodeFourByteIntegerLow(buffer, idx, this.sspi.length);
            idx += 4;
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, hostnameIdx, (short)(idx + bufferOffset));
        byte[] strBytes = this.hostname.getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
        idx += strBytes.length;
        if (!this.username.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, usernameIdx, (short)(idx + bufferOffset));
            usernameIdx = 0;
            strBytes = this.username.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        if (this.password != null && !this.password.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, passwordIdx, (short)(idx + bufferOffset));
            passwordIdx = 0;
            strBytes = this.password.getBytes(StandardCharsets.UTF_16LE);
            for (int i = 0; i < strBytes.length; ++i) {
                final byte lowBits = (byte)((strBytes[i] & 0xF) << 4);
                final byte highBits = (byte)((strBytes[i] & 0xF0) >> 4);
                strBytes[i] = (byte)(highBits | lowBits);
                final byte[] array = strBytes;
                final int n = i;
                array[n] ^= 0xFFFFFFA5;
            }
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        if (!this.appName.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, appNameIdx, (short)(idx + bufferOffset));
            appNameIdx = 0;
            strBytes = this.appName.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        if (!this.serverName.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, serverNameIdx, (short)(idx + bufferOffset));
            serverNameIdx = 0;
            strBytes = this.serverName.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        int extensionsIdx = 0;
        if (extensionOffsetIdx > 0) {
            final int extensionStart = idx + bufferOffset;
            if (this.extensionsInline) {
                DataTypeWriter.encodeTwoByteIntegerLow(buffer, extensionOffsetIdx, (short)(idx + bufferOffset));
                DataTypeWriter.encodeFourByteIntegerLow(buffer, idx, extensionStart + 4);
                idx += 4;
                extensionsIdx = 0;
                if (this.features.size() > 0) {
                    final RawPacketWriter extensionsWriter = new RawPacketWriter(this.connectionState, this, null);
                    for (final Login7Feature feature : this.features) {
                        feature.write(extensionsWriter);
                    }
                    final byte[] extensionsBytes = extensionsWriter.getPacket().getWrittenBuffer();
                    System.arraycopy(extensionsBytes, 8, buffer, idx, extensionsBytes.length - 8);
                    idx += extensionsBytes.length - 8;
                }
                buffer[idx] = -1;
                ++idx;
            }
            else {
                DataTypeWriter.encodeTwoByteIntegerLow(buffer, extensionOffsetIdx, (short)extensionStart);
                extensionsIdx = idx;
                idx += 4;
            }
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, clientNameIdx, (short)(idx + bufferOffset));
        if (!this.clientName.isEmpty()) {
            clientNameIdx = 0;
            strBytes = this.clientName.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, languageIdx, (short)(idx + bufferOffset));
        if (!this.language.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, languageIdx, (short)(idx + bufferOffset));
            languageIdx = 0;
            strBytes = this.language.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        if (!this.database.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, databaseIdx, (short)(idx + bufferOffset));
            databaseIdx = 0;
            strBytes = this.database.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        else {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, databaseIdx, (short)(idx + bufferOffset));
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, attachDbFileIdx, (short)(idx + bufferOffset));
        attachDbFileIdx = 0;
        if (!this.attachDbFile.isEmpty()) {
            strBytes = this.attachDbFile.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, changePasswordIdx, (short)(idx + bufferOffset));
        changePasswordIdx = 0;
        if (this.changePassword != null && !this.changePassword.isEmpty()) {
            strBytes = this.changePassword.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, sspiIdx, (short)(idx + bufferOffset));
        if (this.sspi != null) {
            if (this.sspi.length != 0) {
                if (this.sspi.length < 32767) {
                    System.arraycopy(this.sspi, 0, buffer, idx, this.sspi.length);
                    idx += this.sspi.length;
                }
                else {
                    DataTypeWriter.encodeTwoByteIntegerLow(buffer, sspiIdx, (short)0);
                    System.arraycopy(this.sspi, 0, buffer, idx, this.sspi.length);
                    idx += this.sspi.length;
                }
            }
        }
        final short dataEnd = (short)(idx + bufferOffset);
        if (usernameIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, usernameIdx, dataEnd);
        }
        if (passwordIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, passwordIdx, dataEnd);
        }
        if (appNameIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, appNameIdx, dataEnd);
        }
        if (serverNameIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, serverNameIdx, dataEnd);
        }
        if (clientNameIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, clientNameIdx, dataEnd);
        }
        if (languageIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, languageIdx, dataEnd);
        }
        if (databaseIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, databaseIdx, dataEnd);
        }
        if (attachDbFileIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, attachDbFileIdx, dataEnd);
        }
        if (changePasswordIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, changePasswordIdx, dataEnd);
        }
        if (extensionsIdx > 0) {
            DataTypeWriter.encodeFourByteIntegerLow(buffer, extensionsIdx, idx + bufferOffset);
        }
        writer.writeBytes(buffer, 0, idx);
        if (extensionsIdx > 0) {
            for (final Login7Feature feature2 : this.features) {
                feature2.write(writer);
            }
            writer.writeByte((byte)(-1));
        }
    }
    
    public String getPacketType() {
        return "Login7";
    }
    
    @Override
    public String toString() {
        return "Login7: user: " + this.username + ", app: " + this.appName + ", client: " + this.clientName;
    }
    
    private String readString(final byte[] bytes, final int offset, int idx) {
        final int off = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        final int len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (len == 0) {
            return "";
        }
        return new String(bytes, offset + 8 + off, len * 2, StandardCharsets.UTF_16LE);
    }
    
    private int readFeature(final byte[] bytes, final int offset) {
        int idx = offset;
        final int featureType = bytes[idx];
        ++idx;
        Login7Feature feature = null;
        switch (featureType) {
            case 1: {
                feature = new Login7FeatureSessionRecovery();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 4: {
                feature = new Login7FeatureColumnEncryption();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 5: {
                feature = new Login7FeatureGlobalTransactions();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 9: {
                feature = new Login7FeatureDataClassification();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 10: {
                feature = new Login7FeatureUTF8Support();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 11: {
                feature = new Login7FeatureAzureSQLDNSCaching();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            default: {
                feature = new Login7FeatureUnknown();
                idx += feature.readFromBytes(bytes, idx - 1);
                break;
            }
        }
        this.features.add(feature);
        return idx - offset;
    }
    
    public int getTdsVersion() {
        return this.tdsVersion;
    }
    
    public void setTdsVersion(final int tdsVersion) {
        this.tdsVersion = tdsVersion;
    }
    
    public int getPacketSize() {
        return this.packetSize;
    }
    
    public void setPacketSize(final int packetSize) {
        this.packetSize = packetSize;
    }
    
    public byte[] getClientProgramVersion() {
        return this.clientProgramVersion;
    }
    
    public void setClientProgramVersion(final byte[] clientProgramVersion) {
        this.clientProgramVersion = clientProgramVersion;
    }
    
    public long getClientPid() {
        return this.clientPid;
    }
    
    public void setClientPid(final long clientPid) {
        this.clientPid = clientPid;
    }
    
    public long getConnectionPid() {
        return this.connectionPid;
    }
    
    public void setConnectionPid(final long connectionPid) {
        this.connectionPid = connectionPid;
    }
    
    public boolean isfByteOrder() {
        return this.fByteOrder;
    }
    
    public void setfByteOrder(final boolean fByteOrder) {
        this.fByteOrder = fByteOrder;
    }
    
    public boolean isfChar() {
        return this.fChar;
    }
    
    public void setfChar(final boolean fChar) {
        this.fChar = fChar;
    }
    
    public byte getfFloat() {
        return this.fFloat;
    }
    
    public void setfFloat(final byte fFloat) {
        this.fFloat = fFloat;
    }
    
    public boolean isfDumpLoad() {
        return this.fDumpLoad;
    }
    
    public void setfDumpLoad(final boolean fDumpLoad) {
        this.fDumpLoad = fDumpLoad;
    }
    
    public boolean isfUseDB() {
        return this.fUseDB;
    }
    
    public void setfUseDB(final boolean fUseDB) {
        this.fUseDB = fUseDB;
    }
    
    public boolean isfDatabase() {
        return this.fDatabase;
    }
    
    public void setfDatabase(final boolean fDatabase) {
        this.fDatabase = fDatabase;
    }
    
    public boolean isfSetLang() {
        return this.fSetLang;
    }
    
    public void setfSetLang(final boolean fSetLang) {
        this.fSetLang = fSetLang;
    }
    
    public boolean isfLanguage() {
        return this.fLanguage;
    }
    
    public void setfLanguage(final boolean fLanguage) {
        this.fLanguage = fLanguage;
    }
    
    public boolean isfODBC() {
        return this.fODBC;
    }
    
    public void setfODBC(final boolean fODBC) {
        this.fODBC = fODBC;
    }
    
    public boolean isfTranBoundary() {
        return this.fTranBoundary;
    }
    
    public void setfTranBoundary(final boolean fTranBoundary) {
        this.fTranBoundary = fTranBoundary;
    }
    
    public boolean isfCacheConnect() {
        return this.fCacheConnect;
    }
    
    public void setfCacheConnect(final boolean fCacheConnect) {
        this.fCacheConnect = fCacheConnect;
    }
    
    public byte getfUserType() {
        return this.fUserType;
    }
    
    public void setfUserType(final byte fUserType) {
        this.fUserType = fUserType;
    }
    
    public boolean isfIntSecurity() {
        return this.fIntSecurity;
    }
    
    public void setfIntSecurity(final boolean fIntSecurity) {
        this.fIntSecurity = fIntSecurity;
    }
    
    public byte getfSQLType() {
        return this.fSQLType;
    }
    
    public void setfSQLType(final byte fSQLType) {
        this.fSQLType = fSQLType;
    }
    
    public boolean isfOLEDB() {
        return this.fOLEDB;
    }
    
    public void setfOLEDB(final boolean fOLEDB) {
        this.fOLEDB = fOLEDB;
    }
    
    public boolean isfReadOnlyIntent() {
        return this.fReadOnlyIntent;
    }
    
    public void setfReadOnlyIntent(final boolean fReadOnlyIntent) {
        this.fReadOnlyIntent = fReadOnlyIntent;
    }
    
    public boolean isfChangePassword() {
        return this.fChangePassword;
    }
    
    public void setfChangePassword(final boolean fChangePassword) {
        this.fChangePassword = fChangePassword;
    }
    
    public boolean isfUserInstance() {
        return this.fUserInstance;
    }
    
    public void setfUserInstance(final boolean fUserInstance) {
        this.fUserInstance = fUserInstance;
    }
    
    public boolean isfSendYukonBinaryXML() {
        return this.fSendYukonBinaryXML;
    }
    
    public void setfSendYukonBinaryXML(final boolean fSendYukonBinaryXML) {
        this.fSendYukonBinaryXML = fSendYukonBinaryXML;
    }
    
    public boolean isfUnknownCollationHandling() {
        return this.fUnknownCollationHandling;
    }
    
    public void setfUnknownCollationHandling(final boolean fUnknownCollationHandling) {
        this.fUnknownCollationHandling = fUnknownCollationHandling;
    }
    
    public boolean isfExtension() {
        return this.fExtension;
    }
    
    public void setfExtension(final boolean fExtension) {
        this.fExtension = fExtension;
    }
    
    public int getClientTimeZone() {
        return this.clientTimeZone;
    }
    
    public void setClientTimeZone(final int clientTimeZone) {
        this.clientTimeZone = clientTimeZone;
    }
    
    public int getClientLcid() {
        return this.clientLcid;
    }
    
    public void setClientLcid(final int clientLcid) {
        this.clientLcid = clientLcid;
    }
    
    public String getHostname() {
        return this.hostname;
    }
    
    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }
    
    public String getUsername() {
        return this.username;
    }
    
    public void setUsername(final String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return this.password;
    }
    
    public void setPassword(final String password) {
        this.password = password;
    }
    
    public String getAppName() {
        return this.appName;
    }
    
    public void setAppName(final String appName) {
        this.appName = appName;
    }
    
    public String getServerName() {
        return this.serverName;
    }
    
    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }
    
    public String getClientName() {
        return this.clientName;
    }
    
    public void setClientName(final String clientName) {
        this.clientName = clientName;
    }
    
    public String getLanguage() {
        return this.language;
    }
    
    public void setLanguage(final String language) {
        this.language = language;
    }
    
    public String getDatabase() {
        return this.database;
    }
    
    public void setDatabase(final String database) {
        this.database = database;
    }
    
    public byte[] getClientId() {
        return this.clientId;
    }
    
    public void setClientId(final byte[] clientId) {
        this.clientId = clientId;
    }
    
    public byte[] getSspi() {
        return this.sspi;
    }
    
    public void setSspi(final byte[] sspi) {
        this.sspi = sspi;
    }
    
    public String getAttachDbFile() {
        return this.attachDbFile;
    }
    
    public void setAttachDbFile(final String attachDbFile) {
        this.attachDbFile = attachDbFile;
    }
    
    public String getChangePassword() {
        return this.changePassword;
    }
    
    public void setChangePassword(final String changePassword) {
        this.changePassword = changePassword;
    }
    
    public List<Login7Feature> getFeatures() {
        return this.features;
    }
    
    public void setFeatures(final List<Login7Feature> features) {
        this.features = features;
    }
    
    public Login7Feature getFeature(final String featureName) {
        if (featureName == null || featureName.trim().isEmpty()) {
            return null;
        }
        for (final Login7Feature feature : this.features) {
            if (featureName.equalsIgnoreCase(feature.getFeatureType())) {
                return feature;
            }
        }
        return null;
    }
    
    public Login7Feature removeFeature(final String featureName) {
        if (featureName == null || featureName.trim().isEmpty()) {
            return null;
        }
        Login7Feature foundFeature = null;
        for (final Login7Feature feature : this.features) {
            if (featureName.equalsIgnoreCase(feature.getFeatureType())) {
                foundFeature = feature;
                break;
            }
        }
        if (foundFeature == null) {
            Login7Packet.log.debug(Markers.MSSQL, "Login feature could not be removed because it was not found in the login packet: " + featureName);
        }
        else {
            this.features.remove(foundFeature);
        }
        return foundFeature;
    }
    
    public Login7Feature addFeature(final String featureName) {
        Login7Feature newFeature = null;
        switch (featureName) {
            case "AzureSQLDNSCaching": {
                newFeature = new Login7FeatureAzureSQLDNSCaching();
                break;
            }
            case "ColumnEncryption": {
                newFeature = new Login7FeatureColumnEncryption();
                break;
            }
            case "DataClassification": {
                newFeature = new Login7FeatureDataClassification();
                break;
            }
            case "GlobalTransactions": {
                newFeature = new Login7FeatureGlobalTransactions();
                break;
            }
            case "SessionRecovery": {
                newFeature = new Login7FeatureSessionRecovery();
                break;
            }
            case "UTF8Support": {
                newFeature = new Login7FeatureUTF8Support();
                break;
            }
            default: {
                Login7Packet.log.debug(Markers.MSSQL, "Login feature could not be added because its name is not valid: " + featureName);
                return null;
            }
        }
        this.features.add(newFeature);
        return newFeature;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "tdsVersion": {
                return this.tdsVersion;
            }
            case "packetSize": {
                return this.packetSize;
            }
            case "clientProgramVersion": {
                return this.clientProgramVersion;
            }
            case "clientPid": {
                return this.clientPid;
            }
            case "connectionPid": {
                return this.connectionPid;
            }
            case "fByteOrder": {
                return this.fByteOrder;
            }
            case "fChar": {
                return this.fChar;
            }
            case "fFloat": {
                return this.fFloat;
            }
            case "fDumpLoad": {
                return this.fDumpLoad;
            }
            case "fUseDB": {
                return this.fUseDB;
            }
            case "fDatabase": {
                return this.fDatabase;
            }
            case "fSetLang": {
                return this.fSetLang;
            }
            case "fLanguage": {
                return this.fLanguage;
            }
            case "fODBC": {
                return this.fODBC;
            }
            case "fTranBoundary": {
                return this.fTranBoundary;
            }
            case "fCacheConnect": {
                return this.fCacheConnect;
            }
            case "fUserType": {
                return this.fUserType;
            }
            case "fIntSecurity": {
                return this.fIntSecurity;
            }
            case "fSQLType": {
                return this.fSQLType;
            }
            case "fOLEDB": {
                return this.fOLEDB;
            }
            case "fReadOnlyIntent": {
                return this.fReadOnlyIntent;
            }
            case "fChangePassword": {
                return this.fChangePassword;
            }
            case "fUserInstance": {
                return this.fUserInstance;
            }
            case "fSendYukonBinaryXML": {
                return this.fSendYukonBinaryXML;
            }
            case "fUnknownCollationHandling": {
                return this.fUnknownCollationHandling;
            }
            case "clientTimeZone": {
                return this.clientTimeZone;
            }
            case "clientLcid": {
                return this.clientLcid;
            }
            case "hostname": {
                return this.hostname;
            }
            case "username": {
                return this.username;
            }
            case "password": {
                return this.password;
            }
            case "appName": {
                return this.appName;
            }
            case "serverName": {
                return this.serverName;
            }
            case "clientName": {
                return this.clientName;
            }
            case "language": {
                return this.language;
            }
            case "database": {
                return this.database;
            }
            case "clientId": {
                return this.clientId;
            }
            case "sspi": {
                return this.sspi;
            }
            case "attachDbFile": {
                return this.attachDbFile;
            }
            case "changePassword": {
                return this.changePassword;
            }
            case "features": {
                return this.features;
            }
            case "getFeature": {
                return (Function<Value[],Object>) arg -> this.getFeature(arg[0].asString());
            }
            case "addFeature": {
                return (Function<Value[],Object>) arg -> this.addFeature(arg[0].asString());
            }
            case "removeFeature": {
                return (Function<Value[],Object>) arg -> this.removeFeature(arg[0].asString());
            }
            case "remove": {
                return (Function<Value[],Object>) arguments -> {
                    this.remove();
                    return null;
                };
            }
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return new String[] { "tdsVersion", "packetSize", "clientProgramVersion", "clientPid", "connectionPid", "fByteOrder", "fChar", "fFloat", "fDumpLoad", "fUseDB", "fDatabase", "fSetLang", "fLanguage", "fODBC", "fTranBoundary", "fCacheConnect", "fUserType", "fIntSecurity", "fSQLType", "fOLEDB", "fReadOnlyIntent", "fChangePassword", "fUserInstance", "fSendYukonBinaryXML", "fUnknownCollationHandling", "clientTimeZone", "clientLcid", "hostname", "username", "password", "appName", "serverName", "clientName", "language", "database", "clientId", "sspi", "attachDbFile", "changePassword", "features", "getFeature", "addFeature", "removeFeature", "remove", "toString" };
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "tdsVersion":
            case "packetSize":
            case "clientProgramVersion":
            case "clientPid":
            case "connectionPid":
            case "fByteOrder":
            case "fChar":
            case "fFloat":
            case "fDumpLoad":
            case "fUseDB":
            case "fDatabase":
            case "fSetLang":
            case "fLanguage":
            case "fODBC":
            case "fTranBoundary":
            case "fCacheConnect":
            case "fUserType":
            case "fIntSecurity":
            case "fSQLType":
            case "fOLEDB":
            case "fReadOnlyIntent":
            case "fChangePassword":
            case "fUserInstance":
            case "fSendYukonBinaryXML":
            case "fUnknownCollationHandling":
            case "clientTimeZone":
            case "clientLcid":
            case "hostname":
            case "username":
            case "password":
            case "appName":
            case "serverName":
            case "clientName":
            case "language":
            case "database":
            case "clientId":
            case "sspi":
            case "attachDbFile":
            case "changePassword":
            case "features":
            case "getFeature":
            case "addFeature":
            case "removeFeature":
            case "remove":
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
        switch (key) {
            case "tdsVersion": {
                this.setTdsVersion(value.asInt());
                break;
            }
            case "packetSize": {
                this.setPacketSize(value.asInt());
                break;
            }
            case "clientProgramVersion": {
                this.setClientProgramVersion((byte[])value.asHostObject());
                break;
            }
            case "clientPid": {
                this.setClientPid(value.asLong());
                break;
            }
            case "connectionPid": {
                this.setConnectionPid(value.asLong());
                break;
            }
            case "fByteOrder": {
                this.setfByteOrder(value.asBoolean());
                break;
            }
            case "fChar": {
                this.setfChar(value.asBoolean());
                break;
            }
            case "fFloat": {
                this.setfFloat(value.asByte());
                break;
            }
            case "fDumpLoad": {
                this.setfDumpLoad(value.asBoolean());
                break;
            }
            case "fUseDB": {
                this.setfUseDB(value.asBoolean());
                break;
            }
            case "fDatabase": {
                this.setfDatabase(value.asBoolean());
                break;
            }
            case "fSetLang": {
                this.setfSetLang(value.asBoolean());
                break;
            }
            case "fLanguage": {
                this.setfLanguage(value.asBoolean());
                break;
            }
            case "fODBC": {
                this.setfODBC(value.asBoolean());
                break;
            }
            case "fTranBoundary": {
                this.setfTranBoundary(value.asBoolean());
                break;
            }
            case "fCacheConnect": {
                this.setfCacheConnect(value.asBoolean());
                break;
            }
            case "fUserType": {
                this.setfUserType(value.asByte());
                break;
            }
            case "fIntSecurity": {
                this.setfIntSecurity(value.asBoolean());
                break;
            }
            case "fSQLType": {
                this.setfSQLType(value.asByte());
                break;
            }
            case "fOLEDB": {
                this.setfOLEDB(value.asBoolean());
                break;
            }
            case "fReadOnlyIntent": {
                this.setfReadOnlyIntent(value.asBoolean());
                break;
            }
            case "fChangePassword": {
                this.setfChangePassword(value.asBoolean());
                break;
            }
            case "fUserInstance": {
                this.setfUserInstance(value.asBoolean());
                break;
            }
            case "fSendYukonBinaryXML": {
                this.setfSendYukonBinaryXML(value.asBoolean());
                break;
            }
            case "fUnknownCollationHandling": {
                this.setfUnknownCollationHandling(value.asBoolean());
                break;
            }
            case "clientTimeZone": {
                this.setClientTimeZone(value.asInt());
                break;
            }
            case "clientLcid": {
                this.setClientLcid(value.asInt());
                break;
            }
            case "hostname": {
                this.setHostname(value.asString());
                break;
            }
            case "username": {
                this.setUsername(value.asString());
                break;
            }
            case "password": {
                this.setPassword(value.asString());
                break;
            }
            case "appName": {
                this.setAppName(value.asString());
                break;
            }
            case "serverName": {
                this.setServerName(value.asString());
                break;
            }
            case "clientName": {
                this.setClientName(value.asString());
                break;
            }
            case "language": {
                this.setLanguage(value.asString());
                break;
            }
            case "database": {
                this.setDatabase(value.asString());
                break;
            }
            case "clientId": {
                this.setClientId((byte[])value.asHostObject());
                break;
            }
            case "sspi": {
                this.setSspi((byte[])value.asHostObject());
                break;
            }
            case "attachDbFile": {
                this.setAttachDbFile(value.asString());
                break;
            }
            case "changePassword": {
                this.setChangePassword(value.asString());
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "Login7 packet" });
    }
}

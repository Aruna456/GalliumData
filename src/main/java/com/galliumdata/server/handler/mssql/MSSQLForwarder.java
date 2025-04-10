// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.handler.mssql.tokens.TokenRowBatch;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.PacketGroup;
import com.galliumdata.server.repository.FilterStage;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import javax.net.ssl.SSLEngineResult;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.util.BinaryDump;
import java.util.Iterator;
import com.galliumdata.server.log.Markers;
import java.io.IOException;
import java.io.BufferedOutputStream;
import org.apache.logging.log4j.Logger;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import com.galliumdata.server.adapters.Variables;
import java.io.OutputStream;
import java.net.Socket;

public abstract class MSSQLForwarder implements Runnable
{
    private MSSQLForwarder otherForwarder;
    protected PacketReader reader;
    protected Socket inSock;
    protected Socket outSock;
    protected OutputStream out;
    protected MSSQLAdapter adapter;
    protected MSSQLPacket lastPacket;
    protected boolean stopRequested;
    protected Variables connectionContext;
    protected static ThreadLocal<Variables> threadContext;
    protected Thread thread;
    protected ConnectionState connectionState;
    public static final String CONN_CTXT_PRELOGIN_REQUEST = "CONN_CTXT_PRELOGIN_REQUEST";
    public static final String CONN_CTXT_PRELOGIN_RESPONSE = "CONN_CTXT_PRELOGIN_RESPONSE";
    public static final String CONN_CTXT_LOGIN_PACKET = "CONN_CTXT_LOGIN_PACKET";
    public static final String CONN_CTXT_VAR_USERNAME = "userName";
    public static final String CONN_CTXT_VAR_USER_IP = "userIP";
    public static final String CONN_CTXT_VAR_LAST_SQL = "lastSQL";
    public static final String CONN_CTXT_VAR_CLIENT_FORWARDER = "clientForwarder";
    public static final String CONN_CTXT_VAR_SERVER_FORWARDER = "serverForwarder";
    protected SSLEngine sslEngine;
    protected byte[] networkReadBuffer;
    protected ByteBuffer networkReadBytes;
    protected byte[] networkWriteBuffer;
    protected ByteBuffer networkWriteBytes;
    protected ByteBuffer sslAppBytes;
    protected boolean sslAvailable;
    protected boolean inSslMode;
    protected PreLoginPacket preLoginPacket;
    private static final Logger log;
    protected static final Logger sslLog;
    protected static final Logger logNet;
    public static final boolean SAVE_PACKETS = false;
    
    public MSSQLForwarder(final Socket inSock, final Socket outSock, final Variables connectionContext, final ConnectionState connectionState) {
        this.stopRequested = false;
        this.connectionContext = connectionContext;
        this.connectionState = connectionState;
        this.inSock = inSock;
        try {
            this.reader = new PacketReader(inSock, this.toString(), this);
            if (outSock != null) {
                this.out = new BufferedOutputStream(outSock.getOutputStream());
                this.outSock = outSock;
            }
        }
        catch (final IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }
    
    @Override
    public void run() {
        while (!this.stopRequested) {
            RawPacket rawPacket = null;
            try {
                rawPacket = this.reader.readNextPacket();
            }
            catch (final Exception ex) {
                MSSQLForwarder.log.debug(Markers.MSSQL, "Exception while reading packet: {}", (Object)ex.getMessage());
            }
            if (rawPacket == null) {
                try {
                    this.out.close();
                }
                catch (final Exception ex) {
                    ex.printStackTrace();
                }
                this.otherForwarder.requestStop();
                return;
            }
            final RawPacketGroup pktGrp = new RawPacketGroup(rawPacket);
            if (rawPacket.getPacketId() == 1 && !rawPacket.isEndOfMessage() && (rawPacket.getBuffer()[0] == 1 || rawPacket.getBuffer()[0] == 3)) {
                RawPacket moreRawPkt = rawPacket;
                while (!moreRawPkt.isEndOfMessage()) {
                    moreRawPkt = this.reader.readNextPacket();
                    rawPacket.addBytes(moreRawPkt.getBuffer(), 8);
                }
            }
            try {
                this.processPacket(pktGrp);
            }
            catch (final UnableToParseException utpex) {
                MSSQLForwarder.log.debug(Markers.MSSQL, "Unable to parse packet, forwarding it blindly, cause: {}", (Object)utpex.getMessage());
            }
            catch (final Exception ex2) {
                MSSQLForwarder.log.debug(Markers.MSSQL, "Exception while processing packet, closing connection, error was: {}", (Object)ex2.getMessage());
                try {
                    this.out.close();
                }
                catch (final Exception ex3) {
                    ex3.printStackTrace();
                }
                this.getOtherForwarder().requestStop();
                return;
            }
            this.writePacketGroup(pktGrp);
        }
        this.cleanup();
    }
    
    protected abstract void processPacket(final RawPacketGroup p0);
    
    public MSSQLForwarder getOtherForwarder() {
        return this.otherForwarder;
    }
    
    public void setOtherForwarder(final MSSQLForwarder otherForwarder) {
        this.otherForwarder = otherForwarder;
    }
    
    public void requestStop() {
        this.stopRequested = true;
        try {
            this.inSock.close();
        }
        catch (final Exception ex) {
            MSSQLForwarder.log.trace(Markers.MSSQL, "Exception caught when closing inSock in requestStop for {}: {}", (Object)this, (Object)ex.getMessage());
        }
        this.getOtherForwarder().reader.close();
    }
    
    protected void writePacketGroup(final RawPacketGroup pktGroup) {
        if (pktGroup.getSize() == 0) {
            return;
        }
        int totalSize = 0;
        for (final RawPacket pkt : pktGroup.getPackets()) {
            totalSize += pkt.getBuffer().length;
        }
        final byte[] buffer = new byte[totalSize];
        int idx = 0;
        for (final RawPacket pkt2 : pktGroup.getPackets()) {
            System.arraycopy(pkt2.getBuffer(), 0, buffer, idx, pkt2.getBuffer().length);
            idx += pkt2.getBuffer().length;
        }
        try {
            this.writeOut(buffer, 0, buffer.length);
        }
        catch (final IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }
    
    protected void writeOutWithSMP(final byte[] buffer, final int offset, final int length, final int smpSessionId) throws IOException {
        buffer[offset] = 83;
        buffer[offset + 1] = 8;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, offset + 2, (short)smpSessionId);
        DataTypeWriter.encodeFourByteIntegerLow(buffer, offset + 4, length);
        this.writeOut(buffer, 0, buffer.length);
    }
    
    protected void writeOut(final byte[] buffer, final int offset, final int length) throws IOException {
        this.logNetwork(buffer, offset, length);
        if (this.inSslMode) {
            if (MSSQLForwarder.sslLog.isTraceEnabled()) {
                MSSQLForwarder.sslLog.trace(Markers.MSSQL, this.toString() + " is about to encrypt and send:\n" + BinaryDump.getBinaryDump(buffer, offset, length));
            }
            final SSLEngine engine = this.getOtherForwarder().sslEngine;
            final byte[] clearBytes = new byte[length];
            System.arraycopy(buffer, offset, clearBytes, 0, length);
            final ByteBuffer clearBuffer = ByteBuffer.wrap(clearBytes);
            final byte[] encryptedBytes = new byte[engine.getSession().getPacketBufferSize()];
            final ByteBuffer encryptedBuffer = ByteBuffer.wrap(encryptedBytes);
            encryptedBuffer.clear();
            try {
                final SSLEngineResult sslResult = engine.wrap(clearBuffer, encryptedBuffer);
                if (sslResult.bytesProduced() == 0) {
                    throw new ServerException("db.mssql.ssl.DecryptionError", new Object[] { "No bytes produced" });
                }
                if (sslResult.getStatus() != SSLEngineResult.Status.OK) {
                    throw new ServerException("db.mssql.ssl.DecryptionError", new Object[] { "Status: " + String.valueOf(sslResult.getStatus()) });
                }
                this.out.write(encryptedBytes, 0, sslResult.bytesProduced());
                this.out.flush();
            }
            catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
            return;
        }
        if (MSSQLForwarder.log.isTraceEnabled()) {
            MSSQLForwarder.log.trace(Markers.MSSQL, String.valueOf(this) + " is about to send:\n" + BinaryDump.getBinaryDump(buffer, offset, length));
        }
        try {
            this.out.write(buffer, offset, length);
        }
        catch (final IOException ioex) {
            throw ioex;
        }
        this.out.flush();
    }
    
    protected RawPacket encryptPacket(final MSSQLPacket pkt) {
        final RawPacketWriter writer = new RawPacketWriter(this.connectionState, pkt, this);
        pkt.write(writer);
        writer.finalizePacket();
        final RawPacket rawPkt = writer.getPacket();
        final ByteBuffer pktBuf = ByteBuffer.wrap(rawPkt.getWrittenBuffer());
        final byte[] writeBuf = new byte[32768];
        final ByteBuffer byteBuf = ByteBuffer.wrap(writeBuf);
        try {
            final SSLEngineResult result = this.getOtherForwarder().sslEngine.wrap(pktBuf, byteBuf);
            final byte[] finalBuf = new byte[result.bytesProduced()];
            System.arraycopy(writeBuf, 0, finalBuf, 0, result.bytesProduced());
            return new RawPacket(finalBuf);
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.ssl.EncryptionException", new Object[] { String.valueOf(this.getClass()) + ".encryptPacket", ex });
        }
    }
    
    protected void cleanup() {
    }
    
    public ConnectionState getConnectionState() {
        return this.connectionState;
    }
    
    public abstract String getReceiveFromName();
    
    public abstract String getForwardToName();
    
    public static Variables getThreadContext() {
        Variables var = MSSQLForwarder.threadContext.get();
        if (var == null) {
            var = new Variables();
            MSSQLForwarder.threadContext.set(var);
        }
        return var;
    }
    
    protected AdapterCallbackResponse callRequestFilters(final MSSQLPacket packet, final RawPacketGroup rawPackets) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.REQUEST, packet.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        final Variables context = new Variables();
        context.put("packet", packet);
        context.put("packets", rawPackets);
        context.put("clientAddress", this.inSock.getInetAddress());
        context.put("mssqlutils", new MSSQLUtils((ClientForwarder)this));
        if (packet.isWrappedInSMP()) {
            final int sid = packet.getSMPSessionId();
            final SMPSession smpSession = this.connectionState.getSMPSession((short)sid);
            if (smpSession == null) {
                throw new ServerException("db.mssql.protocol.InvalidSMPSession", new Object[] { sid });
            }
            context.put("connectionContext", smpSession.getSMPConnectionContext());
        }
        else {
            context.put("connectionContext", this.connectionContext);
        }
        context.put("threadContext", getThreadContext());
        final PacketGroup<MSSQLPacket> responsePackets = new PacketGroup<MSSQLPacket>();
        context.put("responsePackets", responsePackets);
        final AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeRequestFilters(packet.getPacketType(), context);
        if (response.reject) {
            MSSQLForwarder.log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        if (responsePackets.isModified()) {
            MSSQLForwarder.log.trace(Markers.MSSQL, "User logic has provided the response");
            final RawPacketWriter writer = new RawPacketWriter(this.connectionState, packet, this);
            for (int i = 0; i < responsePackets.getSize(); ++i) {
                responsePackets.get(i).write(writer);
            }
            writer.close();
        }
        return response;
    }
    
    protected AdapterCallbackResponse callRequestFilters(final MSSQLPacket packet, final PacketGroup<MessageToken> tokens) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.REQUEST, packet.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        final Variables context = new Variables();
        context.put("packet", packet);
        context.put("packets", tokens);
        context.put("clientAddress", this.inSock.getInetAddress());
        if (packet.isWrappedInSMP()) {
            final int sid = packet.getSMPSessionId();
            final SMPSession smpSession = this.connectionState.getSMPSession((short)sid);
            if (smpSession == null) {
                throw new ServerException("db.mssql.protocol.InvalidSMPSession", new Object[] { sid });
            }
            context.put("connectionContext", smpSession.getSMPConnectionContext());
        }
        else {
            context.put("connectionContext", this.connectionContext);
        }
        context.put("threadContext", getThreadContext());
        final PacketGroup<MessageToken> responseTokens = new PacketGroup<MessageToken>();
        context.put("responsePackets", responseTokens);
        final AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeRequestFilters(packet.getPacketType(), context);
        if (response.reject) {
            MSSQLForwarder.log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        if (responseTokens.isModified()) {
            MSSQLForwarder.log.trace(Markers.MSSQL, "User logic has provided the response");
            final RawPacketWriter writer = new RawPacketWriter(this.connectionState, packet, this);
            for (int i = 0; i < responseTokens.getSize(); ++i) {
                responseTokens.get(i).write(writer);
            }
            writer.close();
        }
        return response;
    }
    
    protected AdapterCallbackResponse callResponseFilters(final MSSQLPacket packet, final RawPacketGroup pktGrp) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.RESPONSE, packet.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        final Variables context = new Variables();
        context.put("packet", packet);
        final PacketGroup<MSSQLPacket> packets = new PacketGroup<MSSQLPacket>();
        packets.addPacketNoModify(packet);
        context.put("packets", packets);
        context.put("connectionContext", this.connectionContext);
        context.put("threadContext", getThreadContext());
        final AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeResponseFilters(packet.getPacketType(), context);
        if (response.reject) {
            MSSQLForwarder.log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        if (packet.isRemoved()) {
            packets.removePacket(packet);
        }
        if (packets.isModified()) {
            final RawPacketWriter writer = new RawPacketWriter(this.connectionState, packet, this);
            for (int i = 0; i < packets.getSize(); ++i) {
                final MSSQLPacket pkt = packets.get(i);
                if (!pkt.isRemoved()) {
                    pkt.write(writer);
                }
            }
        }
        return response;
    }
    
    protected AdapterCallbackResponse callResponseFilters(final PacketGroup<MessageToken> tokens) {
        final MessageToken token = tokens.get(0);
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.RESPONSE, token.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        final Variables context = new Variables();
        context.put("packet", token);
        context.put("packets", tokens);
        context.put("connectionContext", this.connectionContext);
        context.put("threadContext", getThreadContext());
        final AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeResponseFilters(token.getPacketType(), context);
        if (response.reject) {
            MSSQLForwarder.log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        if (token.isRemoved()) {
            tokens.removePacket(token);
        }
        return response;
    }
    
    protected AdapterCallbackResponse callResponseFiltersForBatch(final TokenRowBatch batch) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.RESPONSE, batch.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        final Variables context = new Variables();
        context.put("packet", batch);
        context.put("packets", batch);
        context.put("connectionContext", this.connectionContext);
        context.put("threadContext", getThreadContext());
        final AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeResponseFilters(batch.getPacketType(), context);
        if (response.reject) {
            MSSQLForwarder.log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        return response;
    }
    
    protected MSSQLAdapter getAdapter() {
        return this.adapter;
    }
    
    protected void setAdapter(final MSSQLAdapter adapter) {
        this.adapter = adapter;
    }
    
    public PreLoginPacket getFirstPreLoginPacket() {
        return this.preLoginPacket;
    }
    
    protected abstract void incrementBytesReceived(final long p0);
    
    protected void logNetwork(final byte[] bytes, final int idx, final int len) {
        if (!MSSQLForwarder.logNet.isDebugEnabled()) {
            return;
        }
        if (MSSQLForwarder.logNet.isTraceEnabled()) {
            final String bytesStr = BinaryDump.getBinaryDump(bytes, idx, len);
            MSSQLForwarder.logNet.trace(Markers.MSSQL, "Connection " + this.adapter.getConnection().getName() + " to " + this.getForwardToName() + ":\n" + bytesStr);
        }
    }
    
    @Override
    public String toString() {
        return "Generic MSSQL forwarder";
    }
    
    static {
        MSSQLForwarder.threadContext = new ThreadLocal<Variables>();
        log = LogManager.getLogger("galliumdata.dbproto");
        sslLog = LogManager.getLogger("galliumdata.ssl");
        logNet = LogManager.getLogger("galliumdata.network");
    }
}

// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.apache.logging.log4j.LogManager;
import javax.net.ssl.SSLSession;
import org.apache.logging.log4j.Level;
import javax.net.ssl.SSLException;
import com.galliumdata.server.handler.mssql.tokens.TokenError;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import com.galliumdata.server.handler.mssql.tokens.TokenReturnValue;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.PacketGroup;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.handler.ProtocolException;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.util.BinaryDump;
import com.galliumdata.server.log.Markers;
import java.nio.ByteBuffer;
import com.galliumdata.server.adapters.Variables;
import java.net.Socket;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.handler.mssql.tokens.TokenRowBatch;

public class ServerForwarder extends MSSQLForwarder
{
    private RawPacket firstPacketOfGroup;
    protected TokenRowBatch currentRowBatch;
    private final RawPacketGroup currentPacketGroup;
    public static final boolean DEBUG_MODE = false;
    private static final Logger log;
    
    public ServerForwarder(final Socket inSock, final Socket outSock, final Variables connectionContext, final ConnectionState connectionState) {
        super(inSock, outSock, connectionContext, connectionState);
        this.currentPacketGroup = new RawPacketGroup();
    }
    
    @Override
    protected void processPacket(final RawPacketGroup pktGrp) {
        final RawPacket rawPacket = pktGrp.getPackets().get(0);
        this.adapter.getStatus().incrementNumResponses(1L);
        MSSQLPacket pkt = null;
        switch (rawPacket.getTypeCode()) {
            case 4: {
                if (this.preLoginPacket == null) {
                    pkt = new PreLoginPacket(this.connectionState);
                    break;
                }
                pkt = new MessagePacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 18: {
                pkt = new PreLoginPacket(this.connectionState, true);
                break;
            }
            case 3: {
                pkt = new RPCPacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 23: {
                final ByteBuffer byteBuffer = ByteBuffer.wrap(rawPacket.getBuffer());
                final ByteBuffer decryptOut = ByteBuffer.allocate(rawPacket.getBuffer().length * 2);
                SSLEngineResult sslResult = null;
                try {
                    sslResult = this.sslEngine.unwrap(byteBuffer, decryptOut);
                }
                catch (final Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
                final byte[] decryptedBytes = new byte[sslResult.bytesProduced()];
                System.arraycopy(decryptOut.array(), 0, decryptedBytes, 0, sslResult.bytesProduced());
                final RawPacket decryptedRawPacket = new RawPacket(decryptedBytes);
                pktGrp.clear();
                pktGrp.addRawPacket(decryptedRawPacket);
                if (ServerForwarder.log.isTraceEnabled()) {
                    ServerForwarder.log.trace(Markers.MSSQL, "Decrypted:\n" + BinaryDump.getBinaryDump(decryptedBytes, 0, decryptedBytes.length));
                }
                this.inSslMode = true;
                this.processPacket(pktGrp);
                return;
            }
            case 83: {
                final SMPPacket smpPacket = new SMPPacket(this.connectionState);
                smpPacket.readFromBytes(rawPacket.getBuffer(), 0, rawPacket.getBuffer().length);
                if (smpPacket.getSMPPacketType() == SMPPacket.SMPPacketType.DATA) {
                    final SMPSession smpSession = this.connectionState.getSMPSession((short)smpPacket.getSMPSessionId());
                    if (smpSession == null) {
                        throw new ServerException("db.mssql.protocol.InvalidSMPSession", new Object[] { smpPacket.getSMPSessionId() });
                    }
                    if (smpSession.getServerWindow() == 0) {
                        smpSession.setServerWindow(smpPacket.getSMPWindow());
                    }
                }
                switch (smpPacket.getSMPPacketType()) {
                    case SYN: {
                        this.connectionState.startSMPSession(smpPacket, this);
                        break;
                    }
                    case ACK: {
                        final SMPSession smpSession = this.connectionState.getSMPSession((short)smpPacket.getSMPSessionId());
                        smpSession.setServerWindow(smpPacket.getSMPWindow());
                        break;
                    }
                    case FIN: {
                        this.connectionState.closeSMPSession(smpPacket);
                        break;
                    }
                    case DATA: {
                        final RawPacket unwrappedPacket = new RawPacket(smpPacket.getPayload());
                        unwrappedPacket.setSMPSessionId(smpPacket.getSMPSessionId());
                        final SMPSession smpSession = this.connectionState.getSMPSession((short)smpPacket.getSMPSessionId());
                        if (smpSession == null) {
                            throw new ServerException("db.mssql.protocol.InvalidSMPSession", new Object[] { smpPacket.getSMPSessionId() });
                        }
                        smpSession.setServerWindow(smpSession.getServerSeqNum() + (smpPacket.getSMPWindow() - smpPacket.getSeqNum()));
                        pktGrp.clear();
                        pktGrp.addRawPacket(unwrappedPacket);
                        this.processPacket(pktGrp);
                        return;
                    }
                }
                final byte[] smpBytes = smpPacket.serialize();
                if (ServerForwarder.log.isTraceEnabled()) {
                    ServerForwarder.log.trace(Markers.MSSQL, "Forwarding SMP packet from server: " + String.valueOf(smpPacket));
                }
                try {
                    this.writeOut(smpBytes, 0, smpBytes.length);
                }
                catch (final Exception ex2) {
                    throw new ServerException("db.mssql.protocol.ErrorSendingPacket", new Object[] { ex2.getMessage() });
                }
                pktGrp.clear();
                return;
            }
            default: {
                throw new ProtocolException("db.mssql.protocol.UnknownPacketType", new Object[] { rawPacket.getTypeCode() });
            }
        }
        if (pkt instanceof MessagePacket) {
            final MessagePacket msgPkt = (MessagePacket)pkt;
            final RawPacketReader rawReader = new RawPacketReader(this.connectionState, this.reader);
            rawReader.setCurrentPacket(rawPacket, 0);
            pkt.read(rawReader);
            final RawPacketWriter rawWriter = new RawPacketWriter(this.connectionState, msgPkt, this);
            MessageToken token = msgPkt.readNextToken(rawReader);
            while (token != null) {
                if ((token.getTokenType() == -47 || token.getTokenType() == -46) && this.rowsShouldBeBatched()) {
                    final TokenRowBatch batch = this.getRowBatch();
                    batch.addRow((TokenRow)token);
                    if (batch.batchIsFull()) {
                        final AdapterCallbackResponse response = this.callResponseFiltersForBatch(batch);
                        if (response.reject) {
                            pktGrp.clear();
                            batch.clear();
                            continue;
                        }
                        pktGrp.clear();
                        batch.writeOut(rawWriter);
                    }
                    token = msgPkt.readNextToken(rawReader);
                    if (token != null) {
                        continue;
                    }
                    final PacketGroup<MessageToken> tokens = new PacketGroup<MessageToken>();
                    final AdapterCallbackResponse response2 = this.callResponseFiltersForBatch(batch);
                    if (response2.reject) {
                        pktGrp.clear();
                        batch.clear();
                    }
                    else {
                        pktGrp.clear();
                        batch.writeOut(rawWriter);
                        rawWriter.close();
                    }
                }
                else {
                    if (this.currentRowBatch != null && this.currentRowBatch.getRows().size() > 0) {
                        final AdapterCallbackResponse response3 = this.callResponseFiltersForBatch(this.currentRowBatch);
                        if (response3.closeConnection) {
                            ServerForwarder.log.debug(Markers.MSSQL, "Connection " + this.getAdapter().getConnection().getName() + " is being closed by filter, message: " + response3.errorMessage);
                            this.requestStop();
                            return;
                        }
                        if (response3.reject) {
                            pktGrp.clear();
                            this.currentRowBatch.clear();
                            continue;
                        }
                        pktGrp.clear();
                        this.currentRowBatch.writeOut(rawWriter);
                    }
                    final PacketGroup<MessageToken> tokens2 = new PacketGroup<MessageToken>();
                    tokens2.addPacket(token);
                    if (token.getTokenType() == -84) {
                        final String lastSp = this.connectionState.getLastRPC();
                        if ("Sp_Prepare".equalsIgnoreCase(lastSp) || "Sp_PrepExec".equalsIgnoreCase(lastSp)) {
                            final TokenReturnValue retVal = (TokenReturnValue)token;
                            if (retVal.getParamOrdinal() == 0) {
                                final int id = (int)retVal.getValue().getValue();
                                final String currentSql = (String)this.connectionContext.get("lastSQL");
                                this.getConnectionState().openPreparedStatement(currentSql, id);
                            }
                        }
                    }
                    final AdapterCallbackResponse response = this.callResponseFilters(tokens2);
                    if (response.reject) {
                        this.sendErrorMessage(response.errorMessage, (int)response.errorCode);
                        if (response.closeConnection) {
                            this.requestStop();
                        }
                        return;
                    }
                    for (int i = 0; i < tokens2.getSize(); ++i) {
                        final MessageToken tok = tokens2.get(i);
                        if (!tok.isRemoved()) {
                            tok.write(rawWriter);
                        }
                    }
                    token = msgPkt.readNextToken(rawReader);
                    if (token == null) {
                        break;
                    }
                    continue;
                }
            }
            rawWriter.close();
            pktGrp.clear();
            return;
        }
        pkt.readFromRawPacket(rawPacket);
        this.currentPacketGroup.addRawPacket(rawPacket);
        if (this.firstPacketOfGroup == null) {
            this.firstPacketOfGroup = rawPacket;
        }
        if (!pkt.isStatusEndOfMessage()) {
            throw new ServerException("db.mssql.protocol.LastPacketNotEndOfMessage", new Object[0]);
        }
        if (this.preLoginPacket == null && pkt instanceof PreLoginPacket) {
            this.preLoginPacket = (PreLoginPacket)pkt;
            this.connectionState.setServerMajorVersion(this.preLoginPacket.getMajorVersion());
            this.connectionContext.put("CONN_CTXT_PRELOGIN_RESPONSE", pkt);
            final int encryptionMode = this.preLoginPacket.getEncryption();
            if (encryptionMode != 2) {
                this.startSSL();
                this.sslAvailable = true;
                this.getOtherForwarder().sslAvailable = true;
            }
        }
        final AdapterCallbackResponse resp = this.callResponseFilters(pkt, pktGrp);
        if (resp.reject) {
            ServerForwarder.log.debug(Markers.MSSQL, "Response filter '" + resp.logicName + "' has rejected response: " + resp.errorMessage);
            if (resp.errorResponse != null) {
                ServerForwarder.log.debug(Markers.MSSQL, "Sending custom response provided by request filter '" + resp.logicName);
                try {
                    this.writeOut(resp.errorResponse, 0, resp.errorResponse.length);
                }
                catch (final IOException ex3) {
                    ServerForwarder.log.error(Markers.MSSQL, "Exception while sending back rejection packet: " + ex3.getMessage());
                }
            }
            pktGrp.clear();
            if (resp.closeConnection) {
                ServerForwarder.log.debug(Markers.MSSQL, "Closing connection, as requested by filter '" + resp.logicName);
                this.requestStop();
            }
        }
        final RawPacketWriter writer = new RawPacketWriter(this.connectionState, this.firstPacketOfGroup, this);
        pkt.write(writer);
        writer.close();
        pktGrp.clear();
        this.currentPacketGroup.clear();
        this.firstPacketOfGroup = null;
    }
    
    private boolean rowsShouldBeBatched() {
        return this.adapter.batchSizeBytes > 0 || this.adapter.batchSizeRows > 0;
    }
    
    private TokenRowBatch getRowBatch() {
        if (this.currentRowBatch == null) {
            this.currentRowBatch = new TokenRowBatch(this.adapter.batchSizeRows, this.adapter.batchSizeBytes);
        }
        return this.currentRowBatch;
    }
    
    @Override
    protected void writeOutWithSMP(final byte[] buffer, final int offset, final int length, final int smpSessionId) throws IOException {
        final byte[] smpBuffer = new byte[length + 16];
        final SMPSession smpSession = this.connectionState.getSMPSession((short)smpSessionId);
        final int seqNum = smpSession.getAndIncrementServerSeqNum();
        DataTypeWriter.encodeFourByteIntegerLow(smpBuffer, 8, seqNum);
        DataTypeWriter.encodeFourByteIntegerLow(smpBuffer, 12, smpSession.getServerWindow());
        System.arraycopy(buffer, offset, smpBuffer, 16, length);
        super.writeOutWithSMP(smpBuffer, 0, smpBuffer.length, smpSessionId);
    }
    
    public void sendErrorMessage(String errMsg, final int errNo) {
        final MessagePacket pkt = new MessagePacket(this.connectionState);
        pkt.setPacketId((byte)1);
        pkt.setStatusEndOfMessage(true);
        final TokenError errToken = new TokenError(this.connectionState);
        if (errMsg == null) {
            errMsg = "Unspecified error";
        }
        errToken.setMessage(errMsg);
        errToken.setErrorNumber(errNo);
        final RawPacketWriter writer = new RawPacketWriter(this.connectionState, pkt, this);
        errToken.write(writer);
        writer.close();
    }
    
    @Override
    protected void incrementBytesReceived(final long num) {
        this.adapter.getStatus().incrementNumResponseBytes(num);
    }
    
    @Override
    public String getReceiveFromName() {
        return "server";
    }
    
    @Override
    public String getForwardToName() {
        return "client";
    }
    
    @Override
    public String toString() {
        return "MSSQL server forwarder";
    }
    
    protected void startSSL() {
        ServerForwarder.log.trace(Markers.MSSQL, "Beginning SSL handshake for server");
        this.adapter.initializeServerSSLContext(this.connectionState.getServerMajorVersion());
        SSLEngineResult sslResult = null;
        SSLSession sslSession;
        SSLEngineResult.HandshakeStatus handshakeStatus;
        try {
            (this.sslEngine = this.adapter.serverSSLContext.createSSLEngine()).setUseClientMode(true);
            if (this.connectionState.getServerMajorVersion() == 11) {
                this.sslEngine.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1" });
            }
            else {
                this.sslEngine.setEnabledProtocols(new String[] { "TLSv1.2" });
            }
            sslSession = this.sslEngine.getSession();
            this.sslEngine.beginHandshake();
            handshakeStatus = this.sslEngine.getHandshakeStatus();
        }
        catch (final SSLException sex) {
            throw new RuntimeException(sex);
        }
        this.networkReadBuffer = new byte[sslSession.getPacketBufferSize()];
        (this.networkReadBytes = ByteBuffer.wrap(this.networkReadBuffer)).flip();
        this.networkWriteBuffer = new byte[sslSession.getPacketBufferSize() * 5];
        this.networkWriteBytes = ByteBuffer.wrap(this.networkWriteBuffer);
        final byte[] sslAppBuffer = new byte[sslSession.getApplicationBufferSize()];
        this.sslAppBytes = ByteBuffer.wrap(sslAppBuffer);
        final ByteBuffer emptyBytes = ByteBuffer.allocate(0);
    Label_1035:
        while (handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
            ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side SSL handshake: handshakeStatus={}", (Object)handshakeStatus);
            switch (handshakeStatus) {
                case NEED_TASK: {
                    ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side SSL handshake: need to run some tasks...");
                    Runnable task;
                    while ((task = this.sslEngine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side SSL handshake: tasks have been run");
                    sslResult = null;
                    break;
                }
                case NEED_WRAP: {
                    this.networkWriteBytes.clear();
                    try {
                        sslResult = this.sslEngine.wrap(emptyBytes, this.networkWriteBytes);
                    }
                    catch (final SSLException sex2) {
                        throw new RuntimeException(sex2);
                    }
                    if (sslResult.bytesConsumed() == 0 && sslResult.bytesProduced() == 0) {
                        throw new RuntimeException("Server-side NEED_WRAP is stuck in a loop");
                    }
                    while (this.sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                        final byte[] buf = new byte[sslSession.getPacketBufferSize()];
                        final ByteBuffer bbuf = ByteBuffer.wrap(buf);
                        try {
                            sslResult = this.sslEngine.wrap(emptyBytes, bbuf);
                        }
                        catch (final SSLException sex3) {
                            throw new RuntimeException(sex3);
                        }
                        bbuf.flip();
                        this.networkWriteBytes.put(bbuf);
                        if (sslResult.bytesConsumed() == 0 && sslResult.bytesProduced() == 0) {
                            throw new RuntimeException("Server-side NEED_WRAP is stuck in a loop");
                        }
                    }
                    this.writePreLoginSSLPacket();
                    break;
                }
                case NEED_UNWRAP: {
                    if (this.networkReadBytes.position() >= this.networkReadBytes.limit()) {
                        if (ServerForwarder.sslLog.isEnabled(Level.TRACE)) {
                            ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side UNWRAP: position is: " + this.networkReadBytes.position() + ",  limit is: " + this.networkReadBytes.limit());
                        }
                        if (!this.readNextPreLoginSSLPacket()) {
                            ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side Nothing more SSL to read from the network");
                            break Label_1035;
                        }
                    }
                    else if (ServerForwarder.sslLog.isTraceEnabled()) {
                        ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side UNWRAP: still some bytes in the buffer, position is: {},  limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                    }
                    if (ServerForwarder.sslLog.isEnabled(Level.TRACE)) {
                        ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side We have " + (this.networkReadBytes.limit() - this.networkReadBytes.position()) + " bytes in the buffer, about to unwrap");
                    }
                    int numBytesConsumed = 0;
                    while (numBytesConsumed < this.networkReadBytes.limit() && (sslResult == null || sslResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)) {
                        if (ServerForwarder.sslLog.isTraceEnabled()) {
                            ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side About to UNWRAP, position is: {}, limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                        }
                        try {
                            sslResult = this.sslEngine.unwrap(this.networkReadBytes, emptyBytes);
                        }
                        catch (final SSLException sex4) {
                            throw new RuntimeException(sex4);
                        }
                        if (sslResult.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                            this.networkReadBytes.compact();
                            if (!this.readNextPreLoginSSLPacket()) {
                                ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side No data received after an SSL handshake underflow??");
                                break;
                            }
                            continue;
                        }
                        else {
                            if (sslResult.bytesConsumed() == 0) {
                                ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side UNWRAP has consumed zero, so done for now");
                                break;
                            }
                            numBytesConsumed += sslResult.bytesConsumed();
                            if (ServerForwarder.sslLog.isEnabled(Level.TRACE)) {
                                ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side unwrapping result: {}, numBytesConsumed: {}", (Object)sslResult.getHandshakeStatus(), (Object)numBytesConsumed);
                            }
                            this.networkReadBytes.compact();
                            this.networkReadBytes.flip();
                            if (!ServerForwarder.sslLog.isTraceEnabled()) {
                                continue;
                            }
                            ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side After compact, position is: {}, limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                        }
                    }
                    break;
                }
                default: {
                    throw new RuntimeException("Server-side: unexpected handshake status: " + String.valueOf(handshakeStatus));
                }
            }
            handshakeStatus = this.sslEngine.getHandshakeStatus();
            if (ServerForwarder.sslLog.isTraceEnabled()) {
                ServerForwarder.sslLog.trace(Markers.MSSQL, "Server-side SSL status is: {}", (Object)((sslResult == null) ? "null" : sslResult.toString()));
            }
        }
        this.reader.setSslEngine(this.sslEngine);
        final int numLeftOver = this.networkReadBytes.limit() - this.networkReadBytes.position();
        ServerForwarder.sslLog.debug(Markers.MSSQL, "Server-side SSL handshake is done, there are {} bytes left over", (Object)numLeftOver);
    }
    
    private boolean readNextPreLoginSSLPacket() {
        ServerForwarder.sslLog.trace(Markers.MSSQL, "Reading next prelogin SSL packet (server)");
        RawPacket rawPkt = this.reader.readNextPacketPlain(true);
        if (rawPkt == null) {
            return false;
        }
        this.networkReadBytes.clear();
        if (rawPkt.getTypeCode() != 18) {
            this.networkReadBytes.put(rawPkt.getBuffer());
        }
        else {
            final PreLoginPacket pkt = new PreLoginPacket(this.connectionState, true);
            pkt.readFromRawPacket(rawPkt);
            this.networkReadBytes.put(pkt.getSslData());
        }
        while (!rawPkt.isEndOfMessage()) {
            ServerForwarder.sslLog.trace("Incomplete pre-login TLS package, reading the rest");
            rawPkt = this.reader.readNextPacketPlain(true);
            if (rawPkt == null) {
                return false;
            }
            final PreLoginPacket pkt = new PreLoginPacket(this.connectionState, true);
            pkt.readFromRawPacket(rawPkt);
            this.networkReadBytes.put(pkt.getSslData());
        }
        this.networkReadBytes.flip();
        return true;
    }
    
    private void writePreLoginSSLPacket() {
        this.networkWriteBytes.flip();
        final byte[] buf = new byte[this.networkWriteBytes.limit()];
        this.networkWriteBytes.get(buf);
        final PreLoginPacket pkt = new PreLoginPacket(this.connectionState, true);
        pkt.setTypeCode((byte)18);
        pkt.setStatusEndOfMessage(true);
        pkt.setSslData(buf);
        final RawPacketWriter writer = new RawPacketWriter(this.connectionState, pkt, this);
        pkt.write(writer);
        writer.finalizePacket();
        final RawPacket rawPkt = writer.getPacket();
        ServerForwarder.sslLog.trace(Markers.MSSQL, "Writing prelogin SSL packet (server)");
        try {
            this.inSock.getOutputStream().write(rawPkt.getBuffer());
            this.inSock.getOutputStream().flush();
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}

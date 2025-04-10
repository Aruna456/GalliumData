// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.apache.logging.log4j.LogManager;
import javax.net.ssl.SSLSession;
import org.apache.logging.log4j.Level;
import javax.net.ssl.SSLException;
import java.util.List;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import java.util.regex.Matcher;
import javax.net.ssl.SSLEngineResult;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.PacketGroup;
import com.galliumdata.server.handler.mssql.tokens.TokenDone;
import com.galliumdata.server.handler.mssql.tokens.TokenError;
import java.io.IOException;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.util.BinaryDump;
import com.galliumdata.server.log.Markers;
import java.nio.ByteBuffer;
import com.galliumdata.server.adapters.Variables;
import java.net.Socket;
import org.apache.logging.log4j.Logger;
import java.util.regex.Pattern;

public class ClientForwarder extends MSSQLForwarder
{
    private static final Pattern SP_UNPREPARE_PATTERN;
    private static final Logger log;
    
    public ClientForwarder(final Socket inSock, final Socket outSock, final Variables connectionContext, final ConnectionState connectionState) {
        super(inSock, outSock, connectionContext, connectionState);
    }
    
    @Override
    protected void processPacket(final RawPacketGroup pktGrp) {
        final RawPacket rawPacket = pktGrp.getPackets().get(0);
        this.adapter.getStatus().incrementNumRequests(1L);
        MSSQLPacket pkt = null;
        switch (rawPacket.getTypeCode()) {
            case 1: {
                pkt = new SQLBatchPacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 3: {
                pkt = new RPCPacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 6: {
                pkt = new AttentionPacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 7: {
                pkt = new BulkLoadBCPPacket(this.connectionState);
                break;
            }
            case 8: {
                pkt = new FederatedAuthenticationPacket(this.connectionState);
                break;
            }
            case 14: {
                pkt = new TransactionManagerRequest(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 16: {
                pkt = new Login7Packet(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                if (this.preLoginPacket != null) {
                    break;
                }
                this.preLoginPacket = new PreLoginPacket(this.connectionState);
                if (this.getOtherForwarder().preLoginPacket == null) {
                    this.getOtherForwarder().preLoginPacket = this.preLoginPacket;
                    break;
                }
                break;
            }
            case 17: {
                pkt = new SSPIMessagePacket(this.connectionState);
                break;
            }
            case 18: {
                pkt = new PreLoginPacket(this.connectionState, this.preLoginPacket != null);
                break;
            }
            case 23: {
                ByteBuffer byteBuffer = ByteBuffer.wrap(rawPacket.getBuffer());
                ByteBuffer decryptOut = ByteBuffer.allocate(rawPacket.getBuffer().length);
                SSLEngineResult sslResult = null;
                try {
                    sslResult = this.sslEngine.unwrap(byteBuffer, decryptOut);
                }
                catch (final Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
                byte[] decryptedBytes = new byte[sslResult.bytesProduced()];
                System.arraycopy(decryptOut.array(), 0, decryptedBytes, 0, sslResult.bytesProduced());
                final RawPacket decryptedRawPacket = new RawPacket(decryptedBytes);
                pktGrp.clear();
                pktGrp.addRawPacket(decryptedRawPacket);
                if (ClientForwarder.log.isTraceEnabled()) {
                    ClientForwarder.log.trace(Markers.MSSQL, "Received from client, decrypted:\n" + BinaryDump.getBinaryDump(decryptedBytes, 0, decryptedBytes.length));
                }
                this.inSslMode = true;
                for (boolean isLastPacket = decryptedRawPacket.isEndOfMessage(); !isLastPacket; isLastPacket = ((decryptedBytes[1] & 0x1) != 0x0)) {
                    final RawPacket moreRawPkt = this.reader.readNextPacket();
                    byteBuffer = ByteBuffer.wrap(moreRawPkt.getBuffer());
                    decryptOut = ByteBuffer.allocate(moreRawPkt.getBuffer().length);
                    try {
                        sslResult = this.sslEngine.unwrap(byteBuffer, decryptOut);
                    }
                    catch (final Exception ex2) {
                        ex2.printStackTrace();
                        throw new RuntimeException(ex2);
                    }
                    decryptedBytes = new byte[sslResult.bytesProduced()];
                    System.arraycopy(decryptOut.array(), 0, decryptedBytes, 0, sslResult.bytesProduced());
                    decryptedRawPacket.addBytes(decryptedBytes, 8);
                    pktGrp.clear();
                    pktGrp.addRawPacket(decryptedRawPacket);
                }
                decryptedRawPacket.setEndOfMessage(true);
                this.processPacket(pktGrp);
                return;
            }
            case 83: {
                final SMPPacket smpPacket = new SMPPacket(this.connectionState);
                smpPacket.readFromBytes(rawPacket.getBuffer(), 0, rawPacket.getBuffer().length);
                switch (smpPacket.getSMPPacketType()) {
                    case SYN: {
                        this.connectionState.startSMPSession(smpPacket, this);
                        break;
                    }
                    case ACK: {
                        final SMPSession smpSession = this.connectionState.getSMPSession((short)smpPacket.getSMPSessionId());
                        if (smpSession == null) {
                            throw new ServerException("db.mssql.protocol.InvalidSMPSession", new Object[] { smpPacket.getSMPSessionId() });
                        }
                        smpSession.setClientWindow(smpPacket.getSMPWindow());
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
                        smpSession.setClientWindow(smpSession.getClientSeqNum() + (smpPacket.getSMPWindow() - smpPacket.getSeqNum()));
                        pktGrp.clear();
                        pktGrp.addRawPacket(unwrappedPacket);
                        this.processPacket(pktGrp);
                        return;
                    }
                }
                final byte[] smpBytes = smpPacket.serialize();
                if (ClientForwarder.log.isTraceEnabled()) {
                    ClientForwarder.log.trace(Markers.MSSQL, "Forwarding SMP packet from client: " + String.valueOf(smpPacket));
                }
                try {
                    this.writeOut(smpBytes, 0, smpBytes.length);
                }
                catch (final Exception ex3) {
                    throw new ServerException("db.mssql.protocol.ErrorSendingPacket", new Object[] { ex3.getMessage() });
                }
                pktGrp.clear();
                return;
            }
            default: {
                pkt = new UnknownPacket(this.connectionState);
                break;
            }
        }
        pkt.readFromRawPacket(rawPacket);
        if (this.preLoginPacket == null && pkt instanceof PreLoginPacket) {
            this.preLoginPacket = (PreLoginPacket)pkt;
            this.connectionContext.put("CONN_CTXT_PRELOGIN_REQUEST", this.preLoginPacket);
        }
        else if (pkt.getTypeCode() == 18) {
            this.adapter.intializeClientSSLContext();
            this.startSSL(((PreLoginPacket)pkt).getSslData());
            final RawPacket loginReqRaw = this.reader.readNextPacketPlain(true);
            if (loginReqRaw == null) {
                ClientForwarder.log.debug(Markers.MSSQL, "Login request not received because socket was closed");
                return;
            }
            final ByteBuffer buf = ByteBuffer.wrap(loginReqRaw.getBuffer());
            final byte[] outBufArray = new byte[32768];
            final ByteBuffer outBuf = ByteBuffer.wrap(outBufArray);
            try {
                this.sslEngine.unwrap(buf, outBuf);
            }
            catch (final Exception ex4) {
                ex4.printStackTrace();
                return;
            }
            pkt = new Login7Packet(this.connectionState);
            pkt.readFromBytes(outBufArray, 0, outBuf.position());
            ((Login7Packet)pkt).setServerName(this.connectionState.getServerName());
            pktGrp.clear();
            pktGrp.addPacket(pkt);
            this.connectionContext.put("CONN_CTXT_LOGIN_PACKET", pkt);
            final String username = ((Login7Packet)pkt).getUsername();
            if (username != null && username.length() > 0) {
                this.connectionContext.put("userName", ((Login7Packet)pkt).getUsername());
            }
        }
        if (pkt instanceof SQLBatchPacket || pkt instanceof RPCPacket) {
            this.getUserName(pkt);
        }
        if (pkt instanceof SQLBatchPacket) {
            this.connectionContext.remove("lastSQL");
            final SQLBatchPacket batch = (SQLBatchPacket)pkt;
            if (pkt.isWrappedInSMP()) {
                final int sid = pkt.getSMPSessionId();
                final SMPSession smpSession2 = this.connectionState.getSMPSession((short)sid);
                if (smpSession2 == null) {
                    throw new ServerException("db.mssql.protocol.InvalidSMPSession", new Object[] { sid });
                }
                smpSession2.getSMPConnectionContext().put("lastSQL", batch.getSql());
            }
            else {
                this.connectionContext.put("lastSQL", batch.getSql());
            }
            final String sql = batch.getSql();
            final Matcher matcher = ClientForwarder.SP_UNPREPARE_PATTERN.matcher(sql);
            while (matcher.find()) {
                final int id = Integer.parseInt(matcher.group(1));
                this.connectionState.closePreparedStatement(id);
            }
        }
        this.connectionState.setLastPacketFromClient(pkt);
        final AdapterCallbackResponse resp = this.callRequestFilters(pkt, pktGrp);
        if (resp.reject) {
            ClientForwarder.log.debug(Markers.MSSQL, "Request filter '" + resp.logicName + "' has rejected request: " + resp.errorMessage);
            if (resp.errorResponse != null) {
                ClientForwarder.log.debug(Markers.MSSQL, "Sending custom response provided by request filter '" + resp.logicName);
                try {
                    this.getOtherForwarder().writeOut(resp.errorResponse, 0, resp.errorResponse.length);
                }
                catch (final IOException ex5) {
                    ClientForwarder.log.error(Markers.MSSQL, "Exception while sending back rejection packet: " + ex5.getMessage());
                }
            }
            pktGrp.clear();
            if (resp.errorMessage != null) {
                final MessagePacket errMsgPkt = new MessagePacket(this.connectionState);
                errMsgPkt.setSMPSessionId(rawPacket.getSMPSessionId());
                final TokenError errorToken = new TokenError(this.connectionState);
                errorToken.setMessage(resp.errorMessage);
                if (resp.errorCode != 0L) {
                    errorToken.setErrorNumber((int)resp.errorCode);
                }
                final RawPacketWriter writer = new RawPacketWriter(this.connectionState, errMsgPkt, this.getOtherForwarder());
                errorToken.write(writer);
                final TokenDone doneToken = new TokenDone(this.connectionState);
                doneToken.setDoneError(true);
                doneToken.setDoneFinal(true);
                doneToken.write(writer);
                writer.close();
                if (resp.closeConnection) {
                    ClientForwarder.log.debug(Markers.MSSQL, "Closing connection, as requested by filter '" + resp.logicName);
                    this.requestStop();
                }
                return;
            }
            if (resp.closeConnection) {
                ClientForwarder.log.debug(Markers.MSSQL, "Closing connection, as requested by filter '" + resp.logicName);
                this.requestStop();
            }
        }
        if (pkt instanceof SQLBatchPacket) {
            final SQLBatchPacket batch2 = (SQLBatchPacket)pkt;
            if (pkt.isWrappedInSMP()) {
                final int sid2 = pkt.getSMPSessionId();
                final SMPSession smpSession3 = this.connectionState.getSMPSession((short)sid2);
                if (smpSession3 == null) {
                    throw new ServerException("db.mssql.protocol.InvalidSMPSession", new Object[] { sid2 });
                }
                smpSession3.getSMPConnectionContext().put("lastSQL", batch2.getSql());
            }
            else {
                this.connectionContext.put("lastSQL", batch2.getSql());
            }
        }
        else if (pkt instanceof BulkLoadBCPPacket) {
            final BulkLoadBCPPacket msgPkt = (BulkLoadBCPPacket)pkt;
            final RawPacketReader rawReader = new RawPacketReader(this.connectionState, this.reader);
            rawReader.setCurrentPacket(rawPacket, 0);
            pkt.read(rawReader);
            final RawPacketWriter rawWriter = new RawPacketWriter(this.connectionState, msgPkt, this);
            MessageToken token = msgPkt.readNextToken(rawReader);
            while (token != null) {
                final PacketGroup<MessageToken> tokens = new PacketGroup<MessageToken>();
                tokens.addPacket(token);
                final AdapterCallbackResponse response = this.callRequestFilters(pkt, tokens);
                for (int i = 0; i < tokens.getSize(); ++i) {
                    final MessageToken tok = tokens.get(i);
                    if (!tok.isRemoved()) {
                        tok.write(rawWriter);
                    }
                }
                token = msgPkt.readNextToken(rawReader);
                if (token == null) {
                    break;
                }
            }
            rawWriter.close();
            pktGrp.clear();
            return;
        }
        if (pkt instanceof Login7Packet && this.sslAvailable) {
            pktGrp.clear();
            pktGrp.addRawPacket(this.encryptPacket(pkt));
        }
        else {
            final RawPacketWriter writer2 = new RawPacketWriter(this.connectionState, pkt, this);
            pkt.write(writer2);
            writer2.close();
            pktGrp.clear();
        }
    }
    
    private void getUserName(final MSSQLPacket pkt) {
        if (!this.connectionContext.hasMember("userName")) {
            final String username = this.queryForOneString(pkt, "select system_user as username", "username");
            if (username == null || username.isBlank()) {
                this.connectionContext.put("userName", "<unknown>");
            }
            else {
                this.connectionContext.put("userName", username);
            }
        }
    }
    
    private void readFullEncryptedPacket(final RawPacket pkt) {
        RawPacket moreRawPkt = pkt;
        while (!moreRawPkt.isEndOfMessage()) {
            moreRawPkt = this.reader.readNextPacket();
            final ByteBuffer byteBuffer = ByteBuffer.wrap(moreRawPkt.getBuffer());
            final ByteBuffer decryptOut = ByteBuffer.allocate(moreRawPkt.getBuffer().length);
            SSLEngineResult sslResult;
            try {
                sslResult = this.sslEngine.unwrap(byteBuffer, decryptOut);
            }
            catch (final Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
            final byte[] decryptedBytes = new byte[sslResult.bytesProduced()];
            System.arraycopy(decryptOut.array(), 0, decryptedBytes, 0, sslResult.bytesProduced());
            pkt.addBytes(decryptedBytes, 8);
        }
    }
    
    @Override
    protected void writeOutWithSMP(final byte[] buffer, final int offset, final int length, final int smpSessionId) throws IOException {
        final byte[] smpBuffer = new byte[length + 16];
        final SMPSession smpSession = this.connectionState.getSMPSession((short)smpSessionId);
        DataTypeWriter.encodeFourByteIntegerLow(smpBuffer, 8, smpSession.getAndIncrementClientSeqNum());
        DataTypeWriter.encodeFourByteIntegerLow(smpBuffer, 12, smpSession.getClientWindow());
        System.arraycopy(buffer, offset, smpBuffer, 16, length);
        super.writeOutWithSMP(smpBuffer, 0, smpBuffer.length, smpSessionId);
    }
    
    @Override
    protected void incrementBytesReceived(final long num) {
        this.adapter.getStatus().incrementNumRequestBytes(num);
    }
    
    @Override
    public String getReceiveFromName() {
        return "client";
    }
    
    @Override
    public String getForwardToName() {
        return "server";
    }
    
    @Override
    public String toString() {
        return "MSSQL client forwarder";
    }
    
    private String queryForOneString(final MSSQLPacket model, final String sql, final String columnName) {
        final MSSQLResultSet rs = this.queryForRows(model, sql);
        final TokenRow row = rs.getRows().get(0);
        return (String)row.getValue(columnName);
    }
    
    public MSSQLResultSet queryForRows(final MSSQLPacket model, final String sql) {
        final SQLBatchPacket userQuery = new SQLBatchPacket(this.connectionState);
        userQuery.setStatusEndOfMessage(true);
        if (model instanceof SQLBatchPacket) {
            userQuery.copyStreamHeadersFrom((SQLBatchPacket)model);
        }
        else {
            userQuery.setPacketId(model.getPacketId());
            userQuery.setSpid(model.getSpid());
            userQuery.setWindow(model.getWindow());
            userQuery.addStreamHeaders();
        }
        if (model.isWrappedInSMP()) {
            userQuery.setSMPSessionId(model.getSMPSessionId());
        }
        final Object signal = new Object();
        this.getOtherForwarder().reader.sideBandReadSignal = signal;
        userQuery.setSql(sql);
        final RawPacketWriter writer = new RawPacketWriter(this.connectionState, userQuery, this);
        userQuery.write(writer);
        synchronized (signal) {
            writer.close();
            try {
                signal.wait();
            }
            catch (final InterruptedException ioex) {
                throw new RuntimeException(ioex);
            }
        }
        final RawPacket resultPkt = this.getOtherForwarder().reader.sideBandPacket;
        if (resultPkt == null) {
            throw new RuntimeException("Got null packet from sideband read");
        }
        if (resultPkt.getTypeCode() == 23) {}
        if (resultPkt.getTypeCode() != 4) {
            throw new ServerException("db.mssql.protocol.UnexpectedPacketType", new Object[] { resultPkt.getTypeCode(), 4 });
        }
        final MessagePacket msgPkt = new MessagePacket(this.connectionState);
        final SimplePacketReader spr = new SimplePacketReader(this.connectionState, resultPkt);
        final RawPacketReader rawReader = new RawPacketReader(this.connectionState, spr);
        rawReader.setCurrentPacket(resultPkt, 0);
        msgPkt.read(rawReader);
        final MessageToken metaToken = msgPkt.readNextToken(rawReader);
        if (metaToken.getTokenTypeName().equals("ColMetadata")) {
            final TokenColMetadata meta = (TokenColMetadata)metaToken;
            final List<TokenRow> rows = new ArrayList<TokenRow>();
            while (true) {
                final MessageToken dataToken = msgPkt.readNextToken(rawReader);
                if (dataToken == null || dataToken.getTokenTypeName().equals("Done")) {
                    break;
                }
                if (!dataToken.getTokenTypeName().equals("Row")) {
                    continue;
                }
                final TokenRow dataRow = (TokenRow)dataToken;
                rows.add(dataRow);
            }
            final MSSQLResultSet rs = new MSSQLResultSet(meta, rows);
            return rs;
        }
        if ("Error".equals(metaToken.getTokenTypeName())) {
            return new MSSQLResultSet((TokenError)metaToken);
        }
        throw new ServerException("db.mssql.protocol.InternalError", new Object[] { "Error in queryForRows: first token not meta" });
    }
    
    protected void startSSL(final byte[] sslOpening) {
        ClientForwarder.log.trace(Markers.MSSQL, "Beginning SSL handshake for client");
        SSLEngineResult sslResult = null;
        SSLSession sslSession;
        SSLEngineResult.HandshakeStatus handshakeStatus;
        try {
            (this.sslEngine = this.adapter.clientSSLContext.createSSLEngine()).setUseClientMode(false);
            this.sslEngine.setEnabledProtocols(new String[] { "TLSv1.2" });
            sslSession = this.sslEngine.getSession();
            this.sslEngine.beginHandshake();
            handshakeStatus = this.sslEngine.getHandshakeStatus();
        }
        catch (final SSLException sex) {
            throw new RuntimeException(sex);
        }
        this.networkReadBuffer = new byte[sslSession.getPacketBufferSize()];
        (this.networkReadBytes = ByteBuffer.wrap(this.networkReadBuffer)).put(sslOpening);
        this.networkReadBytes.flip();
        this.networkWriteBuffer = new byte[sslSession.getPacketBufferSize() * 5];
        this.networkWriteBytes = ByteBuffer.wrap(this.networkWriteBuffer);
        final byte[] sslAppBuffer = new byte[sslSession.getApplicationBufferSize()];
        this.sslAppBytes = ByteBuffer.wrap(sslAppBuffer);
        final ByteBuffer emptyBytes = ByteBuffer.allocate(0);
    Label_1001:
        while (handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
            ClientForwarder.sslLog.trace(Markers.MSSQL, "SSL handshake: handshakeStatus={}", (Object)handshakeStatus);
            switch (handshakeStatus) {
                case NEED_TASK: {
                    ClientForwarder.sslLog.trace(Markers.MSSQL, "SSL handshake: need to run some tasks...");
                    Runnable task;
                    while ((task = this.sslEngine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    ClientForwarder.sslLog.trace(Markers.MSSQL, "SSL handshake: tasks have been run");
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
                        throw new RuntimeException("NEED_WRAP is stuck in a loop");
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
                            throw new RuntimeException("NEED_WRAP is stuck in a loop");
                        }
                    }
                    this.writePreLoginSSLPacket();
                    break;
                }
                case NEED_UNWRAP: {
                    if (this.networkReadBytes.position() >= this.networkReadBytes.limit()) {
                        if (ClientForwarder.sslLog.isEnabled(Level.TRACE)) {
                            ClientForwarder.sslLog.trace(Markers.MSSQL, "UNWRAP: position is: " + this.networkReadBytes.position() + ",  limit is: " + this.networkReadBytes.limit());
                        }
                        if (!this.readNextPreLoginSSLPacket()) {
                            ClientForwarder.sslLog.trace(Markers.MSSQL, "Nothing more SSL to read from the network");
                            break Label_1001;
                        }
                    }
                    else if (ClientForwarder.sslLog.isTraceEnabled()) {
                        ClientForwarder.sslLog.trace(Markers.MSSQL, "UNWRAP: still some bytes in the buffer, position is: {},  limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                    }
                    if (ClientForwarder.sslLog.isEnabled(Level.TRACE)) {
                        ClientForwarder.sslLog.trace(Markers.MSSQL, "We have " + (this.networkReadBytes.limit() - this.networkReadBytes.position()) + " bytes in the buffer, about to unwrap");
                    }
                    int numBytesConsumed = 0;
                    while (numBytesConsumed < this.networkReadBytes.limit() && (sslResult == null || sslResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)) {
                        if (ClientForwarder.sslLog.isTraceEnabled()) {
                            ClientForwarder.sslLog.trace(Markers.MSSQL, "About to UNWRAP, position is: {}, limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
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
                                ClientForwarder.sslLog.trace(Markers.MSSQL, "No data received after an SSL handshake underflow??");
                                break;
                            }
                            continue;
                        }
                        else {
                            if (sslResult.bytesConsumed() == 0) {
                                ClientForwarder.sslLog.trace(Markers.MSSQL, "UNWRAP has consumed zero, so done for now");
                                break;
                            }
                            numBytesConsumed += sslResult.bytesConsumed();
                            if (ClientForwarder.sslLog.isEnabled(Level.TRACE)) {
                                ClientForwarder.sslLog.trace(Markers.MSSQL, "unwrapping result: {}, numBytesConsumed: {}", (Object)sslResult.getHandshakeStatus(), (Object)numBytesConsumed);
                            }
                            this.networkReadBytes.compact();
                            this.networkReadBytes.flip();
                            if (!ClientForwarder.sslLog.isTraceEnabled()) {
                                continue;
                            }
                            ClientForwarder.sslLog.trace(Markers.MSSQL, "After compact, position is: {}, limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                        }
                    }
                    break;
                }
                default: {
                    throw new RuntimeException("Unexpected handshake status: " + String.valueOf(handshakeStatus));
                }
            }
            handshakeStatus = this.sslEngine.getHandshakeStatus();
            if (ClientForwarder.sslLog.isTraceEnabled()) {
                ClientForwarder.sslLog.trace(Markers.MSSQL, "SSL status is: {}", (Object)((sslResult == null) ? "null" : sslResult.toString()));
            }
        }
        this.reader.setSslEngine(this.sslEngine);
        final int numLeftOver = this.networkReadBytes.limit() - this.networkReadBytes.position();
        ClientForwarder.sslLog.debug(Markers.MSSQL, "SSL handshake is done, there are {} bytes left over", (Object)numLeftOver);
    }
    
    private boolean readNextPreLoginSSLPacket() {
        ClientForwarder.sslLog.trace(Markers.MSSQL, "Reading next prelogin SSL packet (client)");
        final RawPacket rawPkt = this.reader.readNextPacketPlain(true);
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
        final RawPacketWriter writer = new RawPacketWriter(this.connectionState, pkt, this.getOtherForwarder());
        pkt.write(writer);
        ClientForwarder.sslLog.trace(Markers.MSSQL, "Writing prelogin SSL packet (client)");
        writer.close();
    }
    
    static {
        SP_UNPREPARE_PATTERN = Pattern.compile("exec\\s+sp_unprepare\\s+(\\d+)", 10);
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}

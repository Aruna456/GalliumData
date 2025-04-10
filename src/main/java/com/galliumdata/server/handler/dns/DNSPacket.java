// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.js.JSListWrapper;
import java.util.Collections;
import com.galliumdata.server.handler.dns.answers.DNSAnswerTXT;
import com.galliumdata.server.handler.dns.answers.DNSAnswerSOA;
import com.galliumdata.server.handler.dns.answers.DNSAnswerPTR;
import com.galliumdata.server.handler.dns.answers.DNSAnswerOPT;
import com.galliumdata.server.handler.dns.answers.DNSAnswerNS;
import com.galliumdata.server.handler.dns.answers.DNSAnswerMX;
import com.galliumdata.server.handler.dns.answers.DNSAnswerCNAME;
import com.galliumdata.server.handler.dns.answers.DNSAnswerAAAA;
import com.galliumdata.server.handler.dns.answers.DNSAnswerA;
import java.util.Collection;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.ServerException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import com.galliumdata.server.handler.dns.answers.DNSAnswer;
import java.util.List;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyObject;

public class DNSPacket implements ProxyObject
{
    private short transactionId;
    private short clientTransactionId;
    private boolean isQuery;
    private short opcode;
    private boolean authoritative;
    private boolean truncated;
    private boolean recursionDesired;
    private boolean recursionAvailable;
    private byte reserved;
    private byte responseCode;
    private final List<DNSQuestion> questions;
    private final List<DNSAnswer> answers;
    private final List<DNSAnswer> nameServers;
    private final List<DNSAnswer> additionalRecords;
    private Map<String, Short> strings;
    private boolean uncompressedStrings;
    private boolean modified;
    private byte[] originalBytes;
    private final long ts;
    
    public DNSPacket() {
        this.questions = new ArrayList<DNSQuestion>();
        this.answers = new ArrayList<DNSAnswer>();
        this.nameServers = new ArrayList<DNSAnswer>();
        this.additionalRecords = new ArrayList<DNSAnswer>();
        this.strings = new HashMap<String, Short>();
        this.ts = System.currentTimeMillis();
    }
    
    public int read(final byte[] bytes) {
        if (bytes.length < 12) {
            throw new RuntimeException("Packet is too short to be a DNS packet");
        }
        this.strings = new HashMap<String, Short>();
        int idx = 0;
        this.transactionId = readShort(bytes, idx);
        idx += 2;
        final short flags = readShort(bytes, idx);
        idx += 2;
        this.isQuery = ((flags & 0x8000) == 0x0);
        this.opcode = (short)((flags & 0x7800) >> 7);
        this.authoritative = ((flags & 0x400) != 0x0);
        this.truncated = ((flags & 0x200) != 0x0);
        this.recursionDesired = ((flags & 0x100) != 0x0);
        this.recursionAvailable = ((flags & 0x80) != 0x0);
        this.reserved = (byte)(flags & 0x70);
        this.responseCode = (byte)(flags & 0xF);
        final int queryCount = readShort(bytes, idx);
        idx += 2;
        final int answerCount = readShort(bytes, idx);
        idx += 2;
        final int nameServerCount = readShort(bytes, idx);
        idx += 2;
        final int additionalRecordCount = readShort(bytes, idx);
        idx += 2;
        if (this.responseCode != 0) {
            System.arraycopy(bytes, 0, this.originalBytes = new byte[idx], 0, idx);
            return idx;
        }
        for (int i = 0; i < queryCount; ++i) {
            final DNSQuestion question = new DNSQuestion(this);
            idx += question.read(bytes, idx);
            this.questions.add(question);
        }
        for (int i = 0; i < answerCount; ++i) {
            final DNSAnswer answer = DNSAnswer.readAnswer(bytes, idx, this);
            idx += answer.read(bytes, idx);
            this.answers.add(answer);
        }
        for (int i = 0; i < nameServerCount; ++i) {
            final DNSAnswer answer = DNSAnswer.readAnswer(bytes, idx, this);
            idx += answer.read(bytes, idx);
            this.nameServers.add(answer);
        }
        for (int i = 0; i < additionalRecordCount; ++i) {
            final DNSAnswer answer = DNSAnswer.readAnswer(bytes, idx, this);
            idx += answer.read(bytes, idx);
            this.additionalRecords.add(answer);
        }
        System.arraycopy(bytes, 0, this.originalBytes = new byte[idx], 0, idx);
        return idx;
    }
    
    public static short getShortFromBuffer(final byte[] buf, final int offset) {
        return readShort(buf, offset);
    }
    
    public static void setShortInBuffer(final byte[] buf, final int offset, final int id) {
        writeShort((short)id, buf, offset);
    }
    
    public int getSerializedSize() {
        if (!this.modified) {
            return this.originalBytes.length;
        }
        this.strings = new HashMap<String, Short>();
        int size = 2;
        size += 2;
        size += 8;
        for (final DNSQuestion question : this.questions) {
            size += question.getSerializedSize(size);
        }
        for (final DNSAnswer answer : this.answers) {
            size += answer.getSerializedSize(size);
        }
        for (final DNSAnswer answer : this.nameServers) {
            size += answer.getSerializedSize(size);
        }
        for (final DNSAnswer answer : this.additionalRecords) {
            size += answer.getSerializedSize(size);
        }
        return size;
    }
    
    public int writeToBytes(final byte[] buffer, final int offset) {
        if (!this.modified) {
            System.arraycopy(this.originalBytes, 0, buffer, offset, this.originalBytes.length);
            return this.originalBytes.length;
        }
        this.strings = new HashMap<String, Short>();
        int idx = offset;
        writeShort(this.transactionId, buffer, idx);
        idx += 2;
        short flags = 0;
        if (!this.isQuery) {
            flags |= (short)32768;
        }
        flags |= (short)(this.opcode << 7);
        if (this.authoritative) {
            flags |= 0x400;
        }
        if (this.truncated) {
            flags |= 0x200;
        }
        if (this.recursionDesired) {
            flags |= 0x100;
        }
        if (this.recursionAvailable) {
            flags |= 0x80;
        }
        flags |= (short)(this.reserved << 1);
        flags |= this.responseCode;
        writeShort(flags, buffer, idx);
        idx += 2;
        writeShort((short)this.questions.size(), buffer, idx);
        idx += 2;
        writeShort((short)this.answers.size(), buffer, idx);
        idx += 2;
        writeShort((short)this.nameServers.size(), buffer, idx);
        idx += 2;
        writeShort((short)this.additionalRecords.size(), buffer, idx);
        idx += 2;
        for (final DNSQuestion question : this.questions) {
            idx += question.writeToBytes(buffer, idx);
        }
        for (final DNSAnswer answer : this.answers) {
            idx += answer.writeToBytes(buffer, idx);
        }
        for (final DNSAnswer answer : this.nameServers) {
            idx += answer.writeToBytes(buffer, idx);
        }
        for (final DNSAnswer answer : this.additionalRecords) {
            idx += answer.writeToBytes(buffer, idx);
        }
        return idx - offset;
    }
    
    public static int readInt(final byte[] bytes, final int offset) {
        final int byte1 = bytes[offset] & 0xFF;
        final int byte2 = bytes[offset + 1] & 0xFF;
        final int byte3 = bytes[offset + 2] & 0xFF;
        final int byte4 = bytes[offset + 3] & 0xFF;
        return (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
    }
    
    public static short readShort(final byte[] bytes, final int offset) {
        final int byte1 = bytes[offset] & 0xFF;
        final int byte2 = bytes[offset + 1] & 0xFF;
        return (short)((byte1 << 8) + byte2);
    }
    
    public static void writeInt(final int i, final byte[] bytes, final int offset) {
        bytes[offset] = (byte)((i & 0xFF000000) >> 24);
        bytes[offset + 1] = (byte)((i & 0xFF0000) >> 16);
        bytes[offset + 2] = (byte)((i & 0xFF00) >> 8);
        bytes[offset + 3] = (byte)(i & 0xFF);
    }
    
    public static void writeShort(final short i, final byte[] bytes, final int offset) {
        bytes[offset] = (byte)((i & 0xFF00) >> 8);
        bytes[offset + 1] = (byte)(i & 0xFF);
    }
    
    public int readPossiblyCompressedString(final byte[] bytes, final int offset, final StringBuffer sb) {
        int idx = offset;
        while (true) {
            final int numChars = bytes[idx] & 0xFF;
            ++idx;
            if (numChars == 0) {
                if (!this.uncompressedStrings) {
                    final String[] parts = sb.toString().split("\\.");
                    short strIdx = (short)offset;
                    for (int i = 0; i < parts.length; ++i) {
                        String partialName = "";
                        if (i > 0) {
                            strIdx += (short)(1 + parts[i - 1].length());
                        }
                        for (int j = i; j < parts.length; ++j) {
                            if (partialName.length() > 0) {
                                partialName = partialName;
                            }
                            partialName += parts[j];
                        }
                        if (this.strings.get(partialName) != null) {
                            this.uncompressedStrings = true;
                            break;
                        }
                        this.strings.put(partialName, strIdx);
                    }
                }
                return idx - offset;
            }
            if (numChars >= 192) {
                short str2idx = (short)((numChars & 0x3F) << 8);
                str2idx += (short)(bytes[idx] & 0xFF);
                ++idx;
                this.readPossiblyCompressedString(bytes, str2idx, sb);
                return idx - offset;
            }
            if (sb.length() > 0) {
                sb.append(".");
            }
            sb.append(new String(bytes, idx, numChars));
            idx += numChars;
        }
    }
    
    public void rememberString(final String s, final int offset) {
        if (offset < 12) {
            throw new ServerException("db.dns.proto.BadCompressedString");
        }
        final String[] parts = s.split("\\.");
        short strIdx = (short)offset;
        for (int i = 0; i < parts.length; ++i) {
            String partialName = "";
            if (i > 0) {
                strIdx += (short)(1 + parts[i - 1].length());
            }
            for (int j = i; j < parts.length; ++j) {
                if (partialName.length() > 0) {
                    partialName = partialName;
                }
                partialName += parts[j];
            }
            if (this.strings.get(partialName) != null) {
                return;
            }
            this.strings.put(partialName, strIdx);
        }
    }
    
    public int getSizeOfString(final String s, final int offset, final boolean remember) {
        if (s == null) {
            throw new ServerException("db.dns.NameCannotBeNull");
        }
        final String[] parts = s.split("\\.");
        int size = 0;
        for (int i = 0; i < parts.length; ++i) {
            String partialName = "";
            for (int j = i; j < parts.length; ++j) {
                if (partialName.length() > 0) {
                    partialName = partialName;
                }
                partialName += parts[j];
            }
            if (this.strings.get(partialName) != null) {
                if (i > 0 && remember) {
                    this.rememberString(s, offset);
                }
                return size + 2;
            }
            size = ++size + parts[i].length();
        }
        if (remember) {
            this.rememberString(s, offset);
        }
        return size + 1;
    }
    
    public int writeString(final String s, final byte[] bytes, final int offset) {
        final String[] parts = s.split("\\.");
        int idx = offset;
        for (int i = 0; i < parts.length; ++i) {
            String partialName = "";
            for (int j = i; j < parts.length; ++j) {
                if (partialName.length() > 0) {
                    partialName = partialName;
                }
                partialName += parts[j];
            }
            if (this.strings.get(partialName) != null) {
                if (i > 0) {
                    this.rememberString(s, offset);
                }
                short stringIdx = this.strings.get(partialName);
                stringIdx |= (short)49152;
                writeShort(stringIdx, bytes, idx);
                return idx - offset + 2;
            }
            final String part = parts[i];
            bytes[idx] = (byte)part.length();
            ++idx;
            final byte[] strBytes = part.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(strBytes, 0, bytes, idx, strBytes.length);
            idx += strBytes.length;
        }
        bytes[idx] = 0;
        ++idx;
        this.rememberString(s, offset);
        return idx - offset;
    }
    
    public long getTimestamp() {
        return this.ts;
    }
    
    public boolean hasExpired() {
        final long age = System.currentTimeMillis() - this.ts;
        for (final DNSAnswer answer : this.answers) {
            if (age > answer.getTtl() * 1000L) {
                return true;
            }
        }
        for (final DNSAnswer answer : this.nameServers) {
            if (age > answer.getTtl() * 1000L) {
                return true;
            }
        }
        for (final DNSAnswer answer : this.additionalRecords) {
            if (age > answer.getTtl() * 1000L) {
                return true;
            }
        }
        return false;
    }
    
    public short getClientTransactionId() {
        return this.clientTransactionId;
    }
    
    public void setClientTransactionId(final short clientTransactionId) {
        this.clientTransactionId = clientTransactionId;
    }
    
    public short getTransactionId() {
        return this.transactionId;
    }
    
    public void setTransactionId(final short transactionId) {
        this.transactionId = transactionId;
        this.setModified();
    }
    
    public void setTransactionIdNoChange(final short transactionId) {
        this.transactionId = transactionId;
        if (!this.modified) {
            writeShort(transactionId, this.originalBytes, 0);
        }
    }
    
    public boolean isQuery() {
        return this.isQuery;
    }
    
    public void setQuery(final boolean query) {
        this.isQuery = query;
        this.setModified();
    }
    
    public short getOpcode() {
        return this.opcode;
    }
    
    public String getOpcodeName() {
        switch (this.opcode) {
            case 0: {
                return "query";
            }
            case 1: {
                return "inverse query";
            }
            case 2: {
                return "status";
            }
            case 4: {
                return "notify";
            }
            case 5: {
                return "update";
            }
            case 6: {
                return "DNS Stateful Operations";
            }
            default: {
                return "" + this.opcode;
            }
        }
    }
    
    public void setOpcode(final short opcode) {
        this.opcode = opcode;
        this.setModified();
    }
    
    public void setOpcodeName(final String s) {
        final String lowerCase = s.toLowerCase();
        switch (lowerCase) {
            case "query": {
                this.setOpcode((short)0);
                break;
            }
            case "inverse query": {
                this.setOpcode((short)1);
                break;
            }
            case "status": {
                this.setOpcode((short)2);
                break;
            }
            case "notify": {
                this.setOpcode((short)4);
                break;
            }
            case "update": {
                this.setOpcode((short)5);
                break;
            }
            case "DNS Stateful Operations": {
                this.setOpcode((short)6);
                break;
            }
            default: {
                throw new ServerException("db.dns.UnknownOpcode", this.opcode);
            }
        }
    }
    
    public boolean isAuthoritative() {
        return this.authoritative;
    }
    
    public void setAuthoritative(final boolean authoritative) {
        this.authoritative = authoritative;
        this.setModified();
    }
    
    public boolean isTruncated() {
        return this.truncated;
    }
    
    public void setTruncated(final boolean truncated) {
        this.truncated = truncated;
        this.setModified();
    }
    
    public boolean isRecursionDesired() {
        return this.recursionDesired;
    }
    
    public void setRecursionDesired(final boolean recursionDesired) {
        this.recursionDesired = recursionDesired;
        this.setModified();
    }
    
    public boolean isRecursionAvailable() {
        return this.recursionAvailable;
    }
    
    public void setRecursionAvailable(final boolean recursionAvailable) {
        this.recursionAvailable = recursionAvailable;
        this.setModified();
    }
    
    public byte getReserved() {
        return this.reserved;
    }
    
    public void setReserved(final byte b) {
        this.reserved = b;
        this.setModified();
    }
    
    public byte getResponseCode() {
        return this.responseCode;
    }
    
    public String getResponseCodeName() {
        switch (this.responseCode) {
            case 0: {
                return "OK";
            }
            case 1: {
                return "Format error";
            }
            case 2: {
                return "Server failure";
            }
            case 3: {
                return "Name does not exist";
            }
            case 4: {
                return "Not implemented";
            }
            case 5: {
                return "Refused";
            }
            case 6: {
                return "Name that should not exist, does exist";
            }
            case 7: {
                return "Record that should not exist, does exist";
            }
            case 8: {
                return "Record that should exist does not";
            }
            case 9: {
                return "Server not authoritative for the zone";
            }
            case 10: {
                return "Name not contained in zone";
            }
            case 16: {
                return "Bad OPT Version";
            }
            case 17: {
                return "Key not recognized";
            }
            case 18: {
                return "Signature out of time window";
            }
            case 19: {
                return "Bad TKEY Mode";
            }
            case 20: {
                return "Duplicate key name";
            }
            case 21: {
                return "Algorithm not supported";
            }
            case 22: {
                return "Bad Truncation";
            }
            case 23: {
                return "Bad/missing Server Cookie";
            }
            default: {
                return "Unknown:" + this.responseCode;
            }
        }
    }
    
    public void setResponseCode(final byte responseCode) {
        this.responseCode = responseCode;
        this.setModified();
    }
    
    public List<DNSQuestion> getQuestions() {
        return this.questions;
    }
    
    public DNSQuestion getQuestion(final int idx) {
        return this.questions.get(idx);
    }
    
    public DNSQuestion addQuestion() {
        final DNSQuestion question = new DNSQuestion(this);
        this.questions.add(question);
        this.setModified();
        return question;
    }
    
    public void addQuestions(final List<DNSQuestion> questions) {
        this.questions.addAll(questions);
        this.setModified();
    }
    
    public void removeQuestion(final DNSQuestion question) {
        this.questions.remove(question);
        this.setModified();
    }
    
    public void removeQuestion(final int idx) {
        if (idx < 0 || idx >= this.questions.size()) {
            throw new ServerException("db.dns.logic.InvalidCollectionIndex", "questions", idx, this.questions.size());
        }
        this.questions.remove(idx);
    }
    
    public void removeQuestions() {
        this.questions.clear();
    }
    
    public List<DNSAnswer> getAnswers() {
        return this.answers;
    }
    
    public DNSAnswer getAnswer(final int idx) {
        return this.answers.get(idx);
    }
    
    public DNSAnswer addAnswer(final String type) {
        final DNSAnswer answer = this.createDNSAnswer(type);
        this.answers.add(answer);
        this.setModified();
        return answer;
    }
    
    public void removeAnswer(final DNSAnswer answer) {
        this.answers.remove(answer);
        this.setModified();
    }
    
    public void removeAnswer(final int idx) {
        if (idx < 0 || idx >= this.answers.size()) {
            throw new ServerException("db.dns.logic.InvalidCollectionIndex", "answers", idx, this.answers.size());
        }
        this.answers.remove(idx);
    }
    
    public void removeAnswers() {
        this.answers.clear();
    }
    
    public List<DNSAnswer> getNameServers() {
        return this.nameServers;
    }
    
    public DNSAnswer getNameServer(final int idx) {
        return this.nameServers.get(idx);
    }
    
    public DNSAnswer addNameServer(final String type) {
        final DNSAnswer answer = this.createDNSAnswer(type);
        this.nameServers.add(answer);
        this.setModified();
        return answer;
    }
    
    public void removeNameServer(final DNSAnswer answer) {
        this.nameServers.remove(answer);
        this.setModified();
    }
    
    public void removeNameServer(final int idx) {
        if (idx < 0 || idx >= this.nameServers.size()) {
            throw new ServerException("db.dns.logic.InvalidCollectionIndex", "nameServers", idx, this.nameServers.size());
        }
        this.nameServers.remove(idx);
    }
    
    public void removeNameServers() {
        this.nameServers.clear();
    }
    
    public List<DNSAnswer> getAdditionalRecords() {
        return this.additionalRecords;
    }
    
    public DNSAnswer getAdditionalRecord(final int idx) {
        return this.additionalRecords.get(idx);
    }
    
    public DNSAnswer addAdditionalRecord(final String type) {
        final DNSAnswer answer = this.createDNSAnswer(type);
        this.additionalRecords.add(answer);
        this.setModified();
        return answer;
    }
    
    public void removeAdditionalRecord(final DNSAnswer answer) {
        this.additionalRecords.remove(answer);
        this.setModified();
    }
    
    public void removeAdditionalRecord(final int idx) {
        if (idx < 0 || idx >= this.additionalRecords.size()) {
            throw new ServerException("db.dns.logic.InvalidCollectionIndex", "additionalRecords", idx, this.additionalRecords.size());
        }
        this.additionalRecords.remove(idx);
    }
    
    public void removeAdditionalRecords() {
        this.additionalRecords.clear();
    }
    
    private DNSAnswer createDNSAnswer(final String type) {
        switch (type) {
            case "A": {
                return new DNSAnswerA(this);
            }
            case "AAAA": {
                return new DNSAnswerAAAA(this);
            }
            case "CNAME": {
                return new DNSAnswerCNAME(this);
            }
            case "MX": {
                return new DNSAnswerMX(this);
            }
            case "NS": {
                return new DNSAnswerNS(this);
            }
            case "OPT": {
                return new DNSAnswerOPT(this);
            }
            case "PTR": {
                return new DNSAnswerPTR(this);
            }
            case "SOA": {
                return new DNSAnswerSOA(this);
            }
            case "TXT": {
                return new DNSAnswerTXT(this);
            }
            case "": {
                return new DNSAnswer(this);
            }
            default: {
                throw new ServerException("db.dns.UnknownAnswerType", type);
            }
        }
    }
    
    public List<DNSAnswer> getAllAnswers() {
        final List<DNSAnswer> result = new ArrayList<DNSAnswer>();
        result.addAll(this.answers);
        result.addAll(this.nameServers);
        result.addAll(this.additionalRecords);
        return Collections.unmodifiableList(result);
    }
    
    public boolean isModified() {
        return this.modified;
    }
    
    public void setModified() {
        this.modified = true;
    }
    
    public boolean hadUncompressedStrings() {
        return this.uncompressedStrings;
    }
    
    public DNSPacket duplicate() {
        final byte[] buf = new byte[this.getSerializedSize()];
        this.writeToBytes(buf, 0);
        final DNSPacket pkt = new DNSPacket();
        pkt.read(buf);
        return pkt;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "transactionId": {
                return this.transactionId;
            }
            case "isQuery": {
                return this.isQuery;
            }
            case "opcode": {
                return this.opcode;
            }
            case "opcodeName": {
                return this.getOpcodeName();
            }
            case "authoritative": {
                return this.authoritative;
            }
            case "truncated": {
                return this.truncated;
            }
            case "recursionDesired": {
                return this.recursionDesired;
            }
            case "recursionAvailable": {
                return this.recursionAvailable;
            }
            case "responseCode": {
                return this.responseCode;
            }
            case "responseCodeName": {
                return this.getResponseCodeName();
            }
            case "questions": {
                return new JSListWrapper(this.questions, () -> this.setModified());
            }
            case "answers": {
                return new JSListWrapper(this.answers, () -> this.setModified());
            }
            case "nameServers": {
                return new JSListWrapper(this.nameServers, () -> this.setModified());
            }
            case "additionalRecords": {
                return new JSListWrapper(this.additionalRecords, () -> this.setModified());
            }
            case "addQuestion":
                return (Function<Value[],Object>) arguments -> this.addQuestion();

            case "addAnswer": {
                return (Function<Value[],Object>) arguments -> {
                    final Value arg0 = arguments[0];
                    if (!arg0.isString()) {
                        throw new ServerException("db.dns.logic.InvalidArgumentType", "addAnswer", "string", arg0);
                    }
                    return this.addAnswer(arg0.asString());
                };
            }
            case "addNameServer": {
                return (Function<Value[],Object>)arguments -> {
                    final Value arg0 = arguments[0];
                    if (!arg0.isString()) {
                        throw new ServerException("db.dns.logic.InvalidArgumentType", "addNameServer", "string", arg0);
                    }
                    return this.addNameServer(arg0.asString());
                };
            }
            case "addAdditionalRecord": {
                return (Function<Value[],Object>)arguments -> {
                    final Value arg0 = arguments[0];
                    if (!arg0.isString()) {
                        throw new ServerException("db.dns.logic.InvalidArgumentType", "addAdditionalRecord", "string", arg0);
                    }
                    return this.addAdditionalRecord(arg0.asString());
                };
            }
            case "allAnswers": {
                return new JSListWrapper(this.getAllAnswers(), () -> {
                    throw new ServerException("db.dns.logic.CollectionCannotBeModified", "getAllAnswers");
                });
            }
            case "toString": {
                return (Function<Value[],Object>)arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.dns.logic.NoSuchMember", key);
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "transactionId", "isQuery", "opcode", "authoritative", "truncated", "recursionDesired", "recursionAvailable", "responseCode", "questions", "answers", "nameServers", "additionalRecords", "addQuestion", "addAnswer", "addNameServer", "addAdditionalRecord", "allAnswers", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "transactionId":
            case "isQuery":
            case "opcode":
            case "opcodeName":
            case "authoritative":
            case "truncated":
            case "recursionDesired":
            case "recursionAvailable":
            case "responseCode":
            case "responseCodeName":
            case "questions":
            case "answers":
            case "nameServers":
            case "additionalRecords":
            case "addQuestion":
            case "addAnswer":
            case "addNameServer":
            case "addAdditionalRecord":
            case "allAnswers":
            case "toString": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "transactionId": {
                this.transactionId = value.asShort();
                break;
            }
            case "isQuery": {
                this.isQuery = value.asBoolean();
                break;
            }
            case "opcode": {
                this.opcode = value.asShort();
                break;
            }
            case "opcodeName": {
                this.setOpcodeName(value.asString());
                break;
            }
            case "authoritative": {
                this.authoritative = value.asBoolean();
                break;
            }
            case "truncated": {
                this.truncated = value.asBoolean();
                break;
            }
            case "recursionDesired": {
                this.recursionDesired = value.asBoolean();
                break;
            }
            case "recursionAvailable": {
                this.recursionAvailable = value.asBoolean();
                break;
            }
            case "responseCode": {
                this.responseCode = value.asByte();
                break;
            }
            case "questions": {
                if (value.hasArrayElements()) {
                    this.setModified();
                    this.questions.clear();
                    for (int i = 0; i < value.getArraySize(); ++i) {
                        final Value val = value.getArrayElement(i);
                        if (!val.isProxyObject()) {
                            throw new ServerException("db.dns.logic.WrongTypeForCollection", "questions");
                        }
                        this.questions.add(val.asProxyObject());
                    }
                    break;
                }
                throw new ServerException("db.dns.logic.WrongTypeForCollection", "questions");
            }
            case "answers": {
                if (value.hasArrayElements()) {
                    this.setModified();
                    this.answers.clear();
                    for (int i = 0; i < value.getArraySize(); ++i) {
                        final Value val = value.getArrayElement(i);
                        if (!val.isProxyObject()) {
                            throw new ServerException("db.dns.logic.WrongTypeForCollection", "answers");
                        }
                        this.answers.add(val.asProxyObject());
                    }
                    break;
                }
                throw new ServerException("db.dns.logic.WrongTypeForCollection", "answers");
            }
            case "nameServers": {
                if (value.hasArrayElements()) {
                    this.setModified();
                    this.nameServers.clear();
                    for (int i = 0; i < value.getArraySize(); ++i) {
                        final Value val = value.getArrayElement(i);
                        if (!val.isProxyObject()) {
                            throw new ServerException("db.dns.logic.WrongTypeForCollection", "nameServers");
                        }
                        this.nameServers.add(val.asProxyObject());
                    }
                    break;
                }
                throw new ServerException("db.dns.logic.WrongTypeForCollection", "nameServers");
            }
            case "additionalRecords": {
                if (value.hasArrayElements()) {
                    this.setModified();
                    this.additionalRecords.clear();
                    for (int i = 0; i < value.getArraySize(); ++i) {
                        final Value val = value.getArrayElement(i);
                        if (!val.isProxyObject()) {
                            throw new ServerException("db.dns.logic.WrongTypeForCollection", "additionalRecords");
                        }
                        this.additionalRecords.add(val.asProxyObject());
                    }
                    break;
                }
                throw new ServerException("db.dns.logic.WrongTypeForCollection", "additionalRecords");
            }
            default: {
                throw new ServerException("db.dns.logic.NoSuchMember", key);
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.dns.logic.CannotRemoveMember", key, "DNSPacket");
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        if (this.isQuery) {
            hash |= 0x8000;
        }
        hash |= this.opcode << 7;
        if (this.recursionDesired) {
            hash |= 0x100;
        }
        for (final DNSQuestion question : this.questions) {
            hash += question.hashCode();
        }
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DNSPacket)) {
            return false;
        }
        final DNSPacket pkt = (DNSPacket)obj;
        if (this.isQuery != pkt.isQuery) {
            return false;
        }
        if (this.recursionDesired != pkt.recursionDesired) {
            return false;
        }
        if (this.opcode != pkt.opcode) {
            return false;
        }
        if (this.questions.size() != pkt.questions.size()) {
            return false;
        }
        for (final DNSQuestion question : this.questions) {
            if (!pkt.questions.contains(question)) {
                return false;
            }
        }
        for (final DNSAnswer answer : pkt.answers) {
            if (!this.answers.contains(answer)) {
                return false;
            }
        }
        for (final DNSAnswer answer : pkt.nameServers) {
            if (!this.answers.contains(answer)) {
                return false;
            }
        }
        for (final DNSAnswer answer : pkt.additionalRecords) {
            if (!this.answers.contains(answer)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Tx: " + ((this.clientTransactionId != 0) ? this.clientTransactionId : this.transactionId));
        sb.append(", q:" + this.isQuery);
        sb.append(", #qs:" + this.questions.size());
        sb.append(", #as:" + this.answers.size());
        sb.append(", #ns:" + this.nameServers.size());
        sb.append(", #ad:" + this.additionalRecords.size());
        sb.append(", code:" + this.getResponseCodeName());
        if (this.truncated) {
            sb.append(", trunc");
        }
        for (final DNSQuestion q : this.questions) {
            sb.append("\n");
            sb.append(q);
        }
        for (final DNSAnswer ans : this.answers) {
            sb.append("\n    A - ");
            sb.append(ans);
        }
        for (final DNSAnswer ans : this.nameServers) {
            sb.append("\n    NS - ");
            sb.append(ans);
        }
        for (final DNSAnswer ans : this.additionalRecords) {
            sb.append("\n    AR - ");
            sb.append(ans);
        }
        return sb.toString();
    }
}

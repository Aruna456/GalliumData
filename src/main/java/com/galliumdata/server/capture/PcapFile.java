// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.capture;

import java.net.InetSocketAddress;
import java.io.FileOutputStream;
import com.galliumdata.server.ServerException;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.OutputStream;
import java.io.File;

public class PcapFile
{
    private final File file;
    private final OutputStream out;
    public static final int LOCAL_SERVER_INTERFACE = 0;
    public static final int REMOTE_SERVER_INTERFACE = 1;
    public static final int REMOTE_CLIENT_INTERFACE = 2;
    public static final int LOCAL_CLIENT_INTERFACE = 3;
    private final PcapSection section;
    private final AtomicInteger[] sequenceIds;
    private static final int MAX_PACKET_LENGTH = 65000;
    
    public PcapFile(final String fileName) {
        this.sequenceIds = new AtomicInteger[] { new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger() };
        String dirName = null;
        if (dirName == null || dirName.trim().isEmpty()) {
            dirName = System.getProperty("java.io.tmpdir");
        }
        final File dir = new File(dirName);
        if (!dir.exists()) {
            throw new ServerException("settings.BadCaptureDirectory", new Object[] { dirName, "does not exist" });
        }
        if (!dir.isDirectory()) {
            throw new ServerException("settings.BadCaptureDirectory", new Object[] { dirName, "not a directory" });
        }
        if (!dir.canWrite()) {
            throw new ServerException("settings.BadCaptureDirectory", new Object[] { dirName, "cannot write" });
        }
        this.file = new File(dir.getAbsolutePath() + File.separator + fileName);
        if (this.file.exists()) {
            throw new ServerException("settings.BadCaptureFile", new Object[] { this.file.getAbsolutePath(), "file already exists" });
        }
        try {
            this.out = new FileOutputStream(this.file);
        }
        catch (final Exception ex) {
            throw new ServerException("settings.BadCaptureFile", new Object[] { this.file.getAbsolutePath(), ex.getMessage() });
        }
        (this.section = new PcapSection()).addHeaderOption(2, System.getProperty("os.name") + ", " + System.getProperty("os.arch") + ", " + System.getProperty("os.version"));
        this.section.addHeaderOption(3, "Gallium Data Community 1.9.3 build 2245");
    }
    
    public String getName() {
        return this.file.getAbsolutePath();
    }
    
    public void setInterfaces(final InetSocketAddress serverAddr, final InetSocketAddress localAddr1, final InetSocketAddress clientAddr, final InetSocketAddress localAddr2) {
        this.section.addInterfaceDescription().setAddress(serverAddr);
        this.section.addInterfaceDescription().setAddress(localAddr1);
        this.section.addInterfaceDescription().setAddress(clientAddr);
        this.section.addInterfaceDescription().setAddress(localAddr2);
    }
    
    public PcapInterfaceDescription getInterface(final int id) {
        return this.section.getInterfaceDescription(id);
    }
    
    public PcapPacket addPacketFromClient(final byte[] bytes) {
        return this.addPacket(2, 3, bytes);
    }
    
    public PcapPacket addPacketToClient(final byte[] bytes) {
        return this.addPacket(3, 2, bytes);
    }
    
    public PcapPacket addPacketFromServer(final byte[] bytes) {
        return this.addPacket(1, 0, bytes);
    }
    
    public PcapPacket addPacketToServer(final byte[] bytes) {
        return this.addPacket(0, 1, bytes);
    }
    
    public PcapPacket addPacket(final int sourceId, final int destinationId, final byte[] bytes) {
        int numToWrite = bytes.length;
        int idx = 0;
        PcapPacket firstPacket = null;
        while (numToWrite > 0) {
            final PcapPacket pkt = this.section.addPacket(this);
            if (firstPacket == null) {
                firstPacket = pkt;
            }
            pkt.setInterfaceIds(sourceId, destinationId);
            if (numToWrite > 65000) {
                final byte[] buff = new byte[65000];
                System.arraycopy(bytes, idx, buff, 0, 65000);
                idx += 65000;
                numToWrite -= 65000;
                pkt.setData(buff);
            }
            else if (idx > 0) {
                final byte[] buff = new byte[numToWrite];
                System.arraycopy(bytes, idx, buff, 0, numToWrite);
                idx += numToWrite;
                numToWrite -= numToWrite;
                pkt.setData(buff);
            }
            else {
                pkt.setData(bytes);
                numToWrite = 0;
            }
        }
        return firstPacket;
    }
    
    public int getNextSequenceId(final int interfaceId, final int inc) {
        return this.sequenceIds[interfaceId].getAndAdd(inc);
    }
    
    public void close() {
        final byte[] bytes = this.section.serialize();
        try {
            this.out.write(bytes);
            this.out.close();
        }
        catch (final Exception ex) {
            throw new ServerException("core.CaptureError", new Object[] { this.file.getAbsolutePath(), ex.getMessage() });
        }
    }
}

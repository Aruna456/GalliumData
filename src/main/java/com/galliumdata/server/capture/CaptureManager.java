// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.capture;

import java.util.HashMap;
import java.net.InetSocketAddress;
import java.util.Map;
import com.galliumdata.server.repository.Connection;

public class CaptureManager
{
    private final Connection connection;
    private boolean running;
    private PcapFile pcapFile;
    private static final Map<Connection, CaptureManager> managers;
    
    private CaptureManager(final Connection conn) {
        this.running = false;
        this.connection = conn;
    }
    
    public static CaptureManager getManager(final Connection conn) {
        CaptureManager mgr = CaptureManager.managers.get(conn);
        if (mgr == null) {
            mgr = new CaptureManager(conn);
            CaptureManager.managers.put(conn, mgr);
        }
        return mgr;
    }
    
    public void start(final InetSocketAddress serverAddr, final InetSocketAddress localAddr1, final InetSocketAddress clientAddr, final InetSocketAddress localAddr2) {
        this.running = true;
        String fileName = this.connection.getName();
        fileName = fileName.replaceAll(" ", "_");
        fileName = fileName + "_" + System.currentTimeMillis() + ".pcapng";
        (this.pcapFile = new PcapFile(fileName)).setInterfaces(serverAddr, localAddr1, clientAddr, localAddr2);
    }
    
    public void stop() {
        this.running = false;
        this.pcapFile.close();
    }
    
    public boolean isRunning() {
        return this.running;
    }
    
    public PcapPacket addPacketFromClient(final byte[] bytes, final int idx, final int len) {
        if (!this.running) {
            return new PcapPacket(this.pcapFile);
        }
        return this.pcapFile.addPacketFromClient(bytes);
    }
    
    public PcapPacket addPacketToClient(final byte[] bytes, final int idx, final int len) {
        if (!this.running) {
            return new PcapPacket(this.pcapFile);
        }
        return this.pcapFile.addPacketFromClient(bytes);
    }
    
    public PcapPacket addPacketFromServer(final byte[] bytes, final int idx, final int len) {
        if (!this.running) {
            return new PcapPacket(this.pcapFile);
        }
        return this.pcapFile.addPacketFromClient(bytes);
    }
    
    public PcapPacket addPacketToServer(final byte[] bytes, final int idx, final int len) {
        if (!this.running) {
            return new PcapPacket(this.pcapFile);
        }
        return this.pcapFile.addPacketFromClient(bytes);
    }
    
    static {
        managers = new HashMap<Connection, CaptureManager>();
    }
}

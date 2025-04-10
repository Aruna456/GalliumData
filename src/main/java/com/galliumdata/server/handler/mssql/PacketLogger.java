// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.nio.file.FileSystems;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.util.BinaryDump;
import java.time.temporal.TemporalField;
import java.time.temporal.ChronoField;
import java.time.ZonedDateTime;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.file.Path;

public class PacketLogger
{
    private static Path logPath;
    private static final AtomicLong packetNum;
    
    public static void saveIncomingPacket(final Socket sock, final byte[] buffer, final int offset, final int length) {
        final int localPort = sock.getLocalPort();
        final int remotePort = ((InetSocketAddress)sock.getRemoteSocketAddress()).getPort();
        final String remoteHost = ((InetSocketAddress)sock.getRemoteSocketAddress()).getHostString();
        savePacket(remotePort, localPort, remoteHost, buffer, offset, length);
    }
    
    public static void saveOutgoingPacket(final Socket sock, final byte[] buffer, final int offset, final int length) {
        final int localPort = sock.getLocalPort();
        final int remotePort = ((InetSocketAddress)sock.getRemoteSocketAddress()).getPort();
        final String remoteHost = ((InetSocketAddress)sock.getRemoteSocketAddress()).getHostString();
        savePacket(localPort, remotePort, remoteHost, buffer, offset, length);
    }
    
    public static void savePacket(final int fromPort, final int toPort, final String remoteHost, final byte[] buffer, final int offset, final int length) {
        final ZonedDateTime now = ZonedDateTime.now();
        final int hours = now.get(ChronoField.CLOCK_HOUR_OF_DAY);
        final int minutes = now.get(ChronoField.MINUTE_OF_HOUR);
        final int secs = now.get(ChronoField.SECOND_OF_MINUTE);
        String hoursStr = "" + hours;
        if (hours < 10) {
            hoursStr = "0" + hours;
        }
        String minutesStr = "" + minutes;
        if (minutes < 10) {
            minutesStr = "0" + minutes;
        }
        String secondsStr = "" + secs;
        if (secs < 10) {
            secondsStr = "0" + secs;
        }
        final long nanos = System.nanoTime() % 1000000000L;
        final String nanosStr = String.format("%09d", nanos);
        final Path logSubdir = PacketLogger.logPath.resolve(hoursStr).resolve(minutesStr).resolve(secondsStr);
        logSubdir.toFile().mkdirs();
        final String filename = nanosStr + "_" + fromPort + "_" + toPort + "_" + remoteHost + ".txt";
        final Path filePath = logSubdir.resolve(filename);
        final String pktLog = BinaryDump.getBinaryDump(buffer, offset, length);
        final byte[] pktLogBytes = pktLog.getBytes(StandardCharsets.UTF_8);
        try {
            final FileOutputStream outStr = new FileOutputStream(filePath.toFile());
            outStr.write(pktLogBytes);
            outStr.close();
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
    
    static {
        PacketLogger.logPath = FileSystems.getDefault().getPath("/tmp/pkts", new String[0]);
        packetNum = new AtomicLong();
    }
}

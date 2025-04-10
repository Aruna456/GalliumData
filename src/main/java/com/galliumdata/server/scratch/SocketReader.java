// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.scratch;

import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
//import com.galliumdata.server.handler.mysql.DataTypeReader;
import java.net.ServerSocket;
import java.net.InetAddress;

public class SocketReader
{
    public static void main(final String[] args) throws Exception {
        final InetAddress localAddress = InetAddress.getByName("localhost");
        final ServerSocket serverSocket = new ServerSocket(3333, 10, localAddress);
        final Socket outSocket = serverSocket.accept();
        final InputStream inStream = outSocket.getInputStream();
        final byte[] buffer = new byte[500000];
        int readOffset = 0;
        int nextPacketStart = 0;
        boolean needToReadMore = true;
        while (true) {
            if (needToReadMore) {
                final int numRead = inStream.read(buffer, readOffset, buffer.length - readOffset);
                if (numRead == -1) {
                    System.out.println("Socket was closed, exiting");
                    return;
                }
                readOffset += numRead;
            }
            if (readOffset < nextPacketStart + 5) {
                System.arraycopy(buffer, nextPacketStart, buffer, 0, readOffset - nextPacketStart);
                readOffset -= nextPacketStart;
                nextPacketStart = 0;
                needToReadMore = true;
                if (readOffset == buffer.length) {
                    throw new RuntimeException("Unable to read packet -- too large");
                }
                continue;
            }
//            else {
//                final int packetLen = DataTypeReader.readPacketLength(buffer, nextPacketStart);
//                if (packetLen == 5) {
//                    System.out.println("Slow down -- final packet");
//                }
//                if (nextPacketStart + packetLen > readOffset) {
//                    if (nextPacketStart == 0) {
//                        throw new RuntimeException("Unable to read packet -- too large");
//                    }
//                    System.arraycopy(buffer, nextPacketStart, buffer, 0, readOffset - nextPacketStart);
//                    Arrays.fill(buffer, nextPacketStart, buffer.length, (byte)0);
//                    readOffset -= nextPacketStart;
//                    nextPacketStart = 0;
//                    needToReadMore = true;
//                    if (readOffset == buffer.length) {
//                        throw new RuntimeException("Unable to read packet -- too large");
//                    }
//                    continue;
//                }
//                else {
//                    final FunnyPacket packet = new FunnyPacket(buffer, nextPacketStart, packetLen);
//                    packetReceived(packet);
//                    if (packet.isFinalPacket()) {
//                        System.out.println("Final packet received -- reset ---------------------------------------------");
//                    }
//                    nextPacketStart += packetLen;
//                    needToReadMore = false;
//                }
//            }
        }
    }
    
    private static void packetReceived(final FunnyPacket packet) {
        System.out.println("Received packet: " + String.valueOf(packet));
    }
}

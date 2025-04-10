// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.scratch;

import java.net.Socket;
import java.net.InetAddress;

public class SocketRead
{
    public static void main(final String[] args) throws Exception {
        final InetAddress localAddress = InetAddress.getByName("localhost");
        final Socket sock = new Socket(localAddress, 1521);
        final byte[] bytes = new byte[2000];
        System.out.println("About to read bytes");
        sock.getInputStream().read(bytes);
        System.out.println("Read bytes");
    }
}

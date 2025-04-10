// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.scratch;

import java.io.OutputStream;
//import com.galliumdata.server.handler.mysql.DataTypeWriter;
import java.util.Arrays;
import java.util.Random;
import java.net.Socket;
import java.net.InetAddress;

public class SocketStuffer
{
    private static final int BUF_SIZE = 10000000;
    private static final int BUF_MIN = 100000;
    public static final int PKT_MAX = 500000;
    
    public static void main(final String[] args) throws Exception {
        final InetAddress localAddress = InetAddress.getByName("localhost");
        final Socket outSocket = new Socket(localAddress, 3333);
        final OutputStream outStream = outSocket.getOutputStream();
        final byte[] buffer = new byte[10000000];
        final Random random = new Random();
        for (int i = 0; i < 1000000; ++i) {
            Arrays.fill(buffer, (byte)0);
            final int rnd = random.nextInt(9900000) + 100000;
            int totalWritten = 0;
            while (true) {
                final int pktSize = random.nextInt(2495) + 5;
                if (totalWritten + pktSize + 5 >= rnd) {
                    break;
                }
//                DataTypeWriter.encodePacketSize(buffer, totalWritten, pktSize);
//                buffer[totalWritten + pktSize - 1] = -2;
//                totalWritten += pktSize;
            }
//            DataTypeWriter.encodePacketSize(buffer, totalWritten, 5L);
            totalWritten += 4;
            buffer[totalWritten] = -1;
            ++totalWritten;
            outStream.write(buffer, 0, totalWritten);
            System.out.println("Wrote request: " + totalWritten + " bytes");
        }
        outSocket.close();
    }
}

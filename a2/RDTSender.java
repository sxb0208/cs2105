/**
 * RDTSender : Encapsulate a reliable data sender that runs
 * over a unreliable channel that may drop and corrupt packets 
 * (but always delivers in order).
 *
 * Ooi Wei Tsang
 * CS2105, National University of Singapore
 * 12 March 2013
 */
import java.io.*;
import java.util.*;

/**
 * RDTSender receives a byte array from "above", construct a
 * data packet, and send it via UDT.  It also receives
 * ack packets from UDT.
 */
class RDTSender {
    UDTSender udt;
    int seqNumber;
    Timer timer;
    int totalSent;

    static long DELAY = 100;

    RDTSender(String hostname, int port) throws IOException
    {
        udt = new UDTSender(hostname, port);
        seqNumber = 0;
        totalSent = 0;
    }

    /**
     * send() delivers the given array of bytes reliably and should
     * not return until it is sure that the packet has been
     * delivered.
     */
    void send(byte[] data, int length) throws IOException, ClassNotFoundException
    {
        // send packet
        DataPacket p = new DataPacket(data, length, seqNumber);
        System.out.println("S (RDT): send " + p.seq);
        udt.send(p);

        timer = new Timer();
        timer.schedule(new PacketTimer(udt, p), DELAY, DELAY);

        while (true) {
            AckPacket ack = udt.recv();
            if (!ack.isCorrupted && ack.ack == seqNumber) {
                System.out.println("S (RDT): totalSent --------------- " + totalSent);
                totalSent++;
                timer.cancel();
                seqNumber++;
                seqNumber %= 2;
                break;
            }
            System.out.println("S (RDT): While true");
        }
    }

    /**
     * close() is called when there is no more data to send.
     * This method creates an empty packet with 0 bytes and
     * send it to the receiver, to indicate that there is no
     * more data.
     * 
     * This method should not return until it is sure that
     * the empty packet has been delivered correctly.  It 
     * catches any EOFException (which signals the receiver
     * has closed the connection) and close its own connection.
     */
    void close() throws IOException, ClassNotFoundException
    {
        DataPacket p = new DataPacket(null, 0, seqNumber);
        System.out.println("S (RDT): Closing packet");
        udt.send(p);
        try {
            AckPacket ack = udt.recv();
        } catch (EOFException e) {
            System.out.println("S (RDT): EOF");
            udt.close();
        } finally {
            System.out.println("S (RDT): EOF Finally");
            udt.close();
        }
    }
}

class PacketTimer extends TimerTask {
    DataPacket resendPacket;
    UDTSender udt;
    PacketTimer(UDTSender u, DataPacket p) {
        udt = u;
        resendPacket = p;
    }
    public void run() {
        System.out.println("S (RDT): resend " + resendPacket.seq);
        try {
            udt.send(resendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

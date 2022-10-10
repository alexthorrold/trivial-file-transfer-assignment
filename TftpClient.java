import java.net.*;
import java.io.*;

class TftpClient {
    public static void main(String args[]) {
        /* expect three arguments */
        if (args.length != 3) {
            System.err.println("usage: TftpClient <name> <port> <file>\n");
            return;
        }

        /* process the command line arguments */
        String name = args[0];
        String filename = args[2];

        /*
         * use Integer.parseInt to get the number from the second
         * (port) argument
         */
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage() + "here1");
            return;
        }

        /*
         * use InetAddress.getByName to get an IP address for the name
         * argument
         */
        InetAddress ia;
        try {
            ia = InetAddress.getByName(name);
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage() + "here2");
            return;
        }

        /* allocate a DatagramSocket, and setSoTimeout to 6s (6000ms) */
        DatagramSocket ds;
        try {
            ds = new DatagramSocket();
            ds.setSoTimeout(6000);
        } catch (SocketException e) {
            System.out.println(e.getMessage() + "here3");
            return;
        }

        /*
         * open an output file; preface the filename with "rx-" so
         * that you do not try to overwrite a file that the server is
         * about to try to send to you.
         */
        FileOutputStream fos;

        try {
            fos = new FileOutputStream("rx-" + filename);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage() + "here4");
            ds.close();
            return;
        }

        /*
         * create a read request using TftpPacket.createRRQ and then
         * send the packet over the DatagramSocket.
         */
        DatagramPacket rrqPacket = TftpPacket.createRRQ(ia, port, filename);

        try {
            ds.send(rrqPacket);
        } catch (IOException e) {
            System.out.println(e.getMessage() + "here5");
            ds.close();
            return;
        }

        /*
         * declare an integer to keep track of the block that you
         * expect to receive next, initialized to one.  allocate a
         * byte buffer of 514 bytes (i.e., 512 block size plus two one
         * byte header fields) to receive DATA packets.  allocate a
         * DatagramPacket backed by that byte buffer to pass to
         * DatagramSocket::receive to receive packets into.
         */
        int nextBlock = 1;
        byte[] dataPackets = new byte[514];
        DatagramPacket dp = new DatagramPacket(dataPackets, dataPackets.length);

        /*
         * an infinite loop that we will eventually break out of, when
         * either an exception occurs, or we receive a block less than
         * 512 bytes in size.
         */
        while (true) {
            try {
                /*
                 * receive a packet on the DatagramSocket, and then
                 * parse it with TftpPacket.parse.  get the IP address
                 * and port where the packet came from.  The port will
                 * be different to the port you sent the RRQ to, and
                 * we will use these values to transmit the ACK to
                 */
                ds.receive(dp);
                TftpPacket tp = TftpPacket.parse(dp);

                InetAddress resAddress = tp.getAddr();
                int resPort = tp.getPort();

                /*
                 * if we could not parse the packet (parse returns
                 * null), then use "continue" to loop again without
                 * executing the remaining code in the loop.
                 */

                if (tp == null) {
                    continue;
                }

                /*
                 * if the response is an ERROR packet, then print the
                 * error message and return.
                 */
                if (tp.getType() == TftpPacket.Type.ERROR) {
                    System.out.println(tp.getError());
                    return;
                }

                /*
                 * if the packet is not a DATA packet, then use
                 * "continue" to loop again without executing the
                 * remaining code in the loop.
                 */
                if (tp.getType() != TftpPacket.Type.DATA) {
                    continue;
                }

                /*
                 * if the block number is exactly the block that we
                 * were expecting, then get the data (TftpPacket::getData)
                 * and then write it to disk.  then, send an ack for the
                 * block.  then, check to see if we received less than
                 * 512 bytes in that block; if we did, then we infer that
                 * the sender has finished, and break out of the while loop.
                 */
                if (tp.getBlock() == nextBlock) {
                    byte[] data = tp.getData();
                    fos.write(data);

                    DatagramPacket ackPacket = TftpPacket.createACK(resAddress, resPort, nextBlock);
                    ds.send(ackPacket);

                    if (data.length < 512) {
                        break;
                    }
                }

                /*
                 * else, if the block number is the same as the block
                 * number we just received, send an ack without writing
                 * the block to disk, etc.  in this case, the server
                 * didn't receive the ACK we sent, and retransmitted.
                 */
                else if (tp.getBlock() == TftpPacket.lastBlock(nextBlock)) {
                    DatagramPacket ackPacket = TftpPacket.createACK(resAddress, resPort, tp.getBlock());
                    ds.send(ackPacket);
                }
            } catch (Exception e) {
                System.err.println("Exception: " + e);
                break;
            }
        }

        /* cleanup -- close the output file and the DatagramSocket */
        try {
            fos.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        ds.close();
    }
}

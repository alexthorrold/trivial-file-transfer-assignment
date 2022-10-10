import java.net.*;
import java.io.*;

// Alex Thorrold
// 1578693

class TftpServerWorker extends Thread {
    private DatagramPacket req;

    public void run() {
        /* parse the request packet, ensuring that it is an RRQ. */
        TftpPacket tp = TftpPacket.parse(req);

        if (tp == null || tp.getType() != TftpPacket.Type.RRQ) {
            return;
        }

        /*
         * make a note of the address and port the client's request
         * came from
         */
        InetAddress ia = tp.getAddr();
        int port = tp.getPort();

        String filename = tp.getFilename();

        System.out.println(filename);

        /* create a datagram socket to send on, setSoTimeout to 1s (1000ms) */
        DatagramSocket ds;

        try {
            ds = new DatagramSocket();
            ds.setSoTimeout(1000);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
            return;
        }

        /* try to open the file.  if not found, send an error */
        FileInputStream fis;

        try {
            fis = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            DatagramPacket errPacket = TftpPacket.createERROR(ia, port, filename + " could not be found.");

            try {
                ds.send(errPacket);
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }

            return;
        }

        /*
         * Allocate a txbuf byte buffer 512 bytes in size to read
         * chunks of a file into, and declare an integer that keeps
         * track of the current block number initialized to one.
         *
         * allocate a rxbuf byte buffer 2 bytes in size to receive
         * TFTP ack packets into, and then allocate a DatagramPacket
         * backed by that rxbuf to pass to the DatagramSocket::receive
         * method
         */
        byte[] txbuf = new byte[512];
        int currentBlock = 1;

        byte[] rxbuf = new byte[2];
        DatagramPacket ackPacket = new DatagramPacket(rxbuf, rxbuf.length);

        while (true) {
            /*
             * read a chunk from the file, and make a note of the size
             * read.  if we get EOF, signalled by
             * FileInputStream::read returning -1, then set the chunk
             * size to zero to cause an empty block to be sent.
             */
            int chunkSize;

            try {
                chunkSize = fis.read(txbuf);

                if (chunkSize == -1) {
                    chunkSize = 0;
                }
            } catch (IOException e) {
                chunkSize = 0;
                System.out.println(e.getMessage());
            }

            /*
             * use TftpPacket.createData to create a DATA packet
             * addressed to the client's address and port, specifying
             * the block number, the contents of the block, and the
             * size of the block
             */
            DatagramPacket dataPacket = TftpPacket.createDATA(ia, port, currentBlock, txbuf, chunkSize);

            /*
             * declare a boolean value to control transmission through
             * each loop, and an integer to count the number of
             * transmission attempts made with the current block.
             */
            boolean transmit = true;
            int numberOfTransmissions = 0;

            while (transmit) {
                /*
                 * if we are to transmit the packet this pass through
                 * the loop, send the packet and increment the number
                 * of attempts we have made with this block.  set the
                 * boolean value to false to prevent the packet being
                 * retransmitted except on a SocketTimeoutException,
                 * noted below.
                 */
                try {
                    ds.send(dataPacket);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }

                numberOfTransmissions++;
                transmit = false;

                /*
                 * call receive, looking for an ACK for the current
                 * block number.  if we get an ack, break out of the
                 * retransmission loop.  otherwise, if we get a
                 * SocketTimeoutException, set the boolean value to
                 * true.  if we have tried five times, then we break
                 * out of the loop to give up.
                 */
                try {
                    ds.receive(ackPacket);

                    TftpPacket ack = TftpPacket.parse(ackPacket);

                    if (ack.getType() == TftpPacket.Type.ACK) {
                        break;
                    } else {
                        transmit = true;

                        if (numberOfTransmissions >= 5) {
                            break;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    transmit = true;

                    if (numberOfTransmissions >= 5) {
                        break;
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }

            /*
             * outside the loop, determine if we just sent our last
             * transmission (the block size was less than 512 bytes,
             * or we tried five times without getting an ack
             */
            if (chunkSize < 512 || numberOfTransmissions >= 5) {
                break;
            }

            /*
             * use TftpPacket.nextBlock to determine the next block
             * number to use.
             */
            currentBlock = TftpPacket.nextBlock(currentBlock);
        }

        /* cleanup: close the FileInputStream and the DatagramSocket */
        try {
            fis.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        ds.close();
    }

    public TftpServerWorker(DatagramPacket req) {
        this.req = req;
    }
}

class TftpServer {
    public static void main(String args[]) {
        try {
            /*
             * allocate a DatagramSocket, and find out what port it is
             * listening on
             */
            DatagramSocket ds = new DatagramSocket();
            System.out.println("TftpServer on port " + ds.getLocalPort());

            for (; ; ) {
                /*
                 * allocate a byte buffer to back a DatagramPacket
                 * with.  I suggest 1472 byte array for this.
                 * allocate the corresponding DatagramPacket, and call
                 * DatagramSocket::receive
                 */
                byte[] buf = new byte[1472];
                DatagramPacket p = new DatagramPacket(buf, 1472);
                ds.receive(p);

                /*
                 * allocate a new worker thread to process this
                 * packet.  implement the logic looking for an RRQ in
                 * the worker thread's run method.
                 */
                TftpServerWorker worker = new TftpServerWorker(p);
                worker.start();
            }
        } catch (Exception e) {
            System.err.println("TftpServer::main Exception: " + e);
        }
    }
}

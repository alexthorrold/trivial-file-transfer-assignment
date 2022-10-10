import java.net.*;
import java.io.*;

class TftpServerWorker extends Thread {
    private DatagramPacket req;

    public void run() {
        /* parse the request packet, ensuring that it is a RRQ. */
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
        /* XXX: implement */

        /* try to open the file.  if not found, send an error */
        /* XXX: implement */

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
        /* XXX: implement */

        while (true) {
            /*
             * read a chunk from the file, and make a note of the size
             * read.  if we get EOF, signalled by
             * FileInputStream::read returning -1, then set the chunk
             * size to zero to cause an empty block to be sent.
             */
            /* XXX: implement */

            /*
             * use TftpPacket.createData to create a DATA packet
             * addressed to the client's address and port, specifying
             * the block number, the contents of the block, and the
             * size of the block
             */
            /* XXX: implement */

            /*
             * declare a boolean value to control transmission through
             * each loop, and an integer to count the number of
             * transmission attempts made with the current block.
             */
            /* XXX: implement */

            while (true) {
                /*
                 * if we are to transmit the packet this pass through
                 * the loop, send the packet and increment the number
                 * of attempts we have made with this block.  set the
                 * boolean value to false to prevent the packet being
                 * retransmitted except on a SocketTimeoutException,
                 * noted below.
                 */
                /* XXX: implement */

                /*
                 * call receive, looking for an ACK for the current
                 * block number.  if we get an ack, break out of the
                 * retransmission loop.  otherwise, if we get a
                 * SocketTimeoutException, set the boolean value to
                 * true.  if we have tried five times, then we break
                 * out of the loop to give up.
                 */
                /* XXX: implement */

                /* XXX: delete the break, once you have implemented the loop */
                break;
            }

            /*
             * outside of the loop, determine if we just sent our last
             * transmission (the block size was less than 512 bytes,
             * or we tried five times without getting an ack
             */
            /* XXX: implement */

            /*
             * use TftpPacket.nextBlock to determine the next block
             * number to use.
             */
            /* XXX: implement */

            /* XXX: delete the break, once you have implemented the loop */
            break;
        }

        /* cleanup: close the FileInputStream and the DatagramSocket */
        /* XXX: implement */

        return;
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
                 * packet.  implement the logic looking for a RRQ in
                 * the worker thread's run method.
                 */
                TftpServerWorker worker = new TftpServerWorker(p);
                worker.start();
            }
        } catch (Exception e) {
            System.err.println("TftpServer::main Exception: " + e);
        }

        return;
    }
}

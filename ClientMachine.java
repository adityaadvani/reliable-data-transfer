//package project2fcn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class acts as the sender class for the file transfer.
 *
 * @author Aditya Advani
 */
public class ClientMachine {

    // static variables are shared and used by sender and receiver threads.
    static String file;
    static int timeout;
    static int server_port;
    static String server_address;
    static boolean quiet;
    static String algo;
    static HashMap<Integer, packet> packets;
    static DatagramSocket d;
    static DatagramSocket e;
    static int port2;
    static int packetnumber;
    static boolean terminate = false;
    static int packetsize;
    static int remainder;

    // default constructor
    ClientMachine() {
    }

    // parameterized constructor
    ClientMachine(String server_address1, int port1, int timeout1, String file1, boolean quiet1, String algo1) {
        file = file1;
        timeout = timeout1;
        quiet = quiet1;
        algo = algo1;
        server_address = server_address1;
        server_port = port1;
    }

    // main method to initiate the sender
    public void connect() {

        try {
            //get HashMap of packets
            Client c = new Client();
            packets = c.getpackets(file);
            remainder = c.getRemainder();

            //initialize the size and number of packets to be transfered 
            packetnumber = packets.size();
            packetsize = packets.get(1).dataSize;

            //create socket
            d = new DatagramSocket();
            port2 = server_port + 1;
            e = new DatagramSocket(port2);

            //create and start receiver and sender threads for Sender machine.
            Thread CSend = new Thread(new ClientSender(d, packetsize));
            Thread CRec = new Thread(new ClientReceiver(e));
            CSend.start();
            CRec.start();
        } catch (SocketException ex) {
            Logger.getLogger(ClientMachine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

/**
 * This class provides the functionality of the sender thread for the Sender
 * machine.
 *
 * @author Aditya Advani
 */
class ClientSender extends Thread {

    // instance variables
    DatagramSocket d;
    int packetnumber;
    int packetsize;
    int winstart = 1;

    //static variables shared with receiver thread
    static boolean[] sentpackets;
    static ArrayList<Integer> next_packet_to_send = new ArrayList<>();
    static float cwnd = 1;
    static boolean synreceived = false;

    //low thresh to test congestion
//    static int ssthresh = 5;
    static int ssthresh = 50;
    static float cwndinc = 0;

    packet p;

    //runnable constructor
    public ClientSender(Runnable target) {
        super(target);
    }

    //default constructor
    ClientSender() {
    }

    //parameterized constructor
    ClientSender(DatagramSocket d, int packetsize) {
        this.d = d;
        this.packetnumber = ClientMachine.packetnumber;
        this.packetsize = packetsize;
        sentpackets = new boolean[packetnumber + 1];
    }

    @Override
    //this method provides the business logic of the sender thread of the sender machine
    public void run() {

        //get size of complete packet
        packet s = ClientMachine.packets.get(1);
        int packetsizewithheader = serialize(ClientMachine.packets.get(1)).length;

        //variable to calculate transfer speed 
        float speed;

        if (ClientMachine.quiet == false) {
            System.out.println("S: started sender\n");
        }

        try {
            InetAddress address = InetAddress.getByName(ClientMachine.server_address);

            byte[] send = new byte[2048];
            byte[] receive = new byte[2048];

            //declare datagram packet and helper variable
            DatagramPacket pack;
            int pack_to_send;

            p = new packet();
            p.FIN = false;
            p.ACKnum = ClientMachine.remainder;
            p.data = null;
            p.sequenceNumber = 0;
            p.dataSize = 0;

            //current packet = packet at start of congestion window
            int currpack = winstart;

            //SYN packet to initiate the handshake
            while (!synreceived) {
                send = serialize(p);
                pack = new DatagramPacket(send, send.length, address, ClientMachine.server_port);
                d.send(pack);
                sleep(150);
            }

            if (ClientMachine.quiet == false) {
                System.out.println("\n\nS: To server: " + "SYN1");
            }

            //update SYN packet sent
            sentpackets[0] = true;

            //infinite loop
            for (;;) {
                System.out.print("");

                //if transfer is completed
                if (ClientMachine.terminate) {
                    break;
                }
//Enter sleep here for delay simulation

                //if no timeout or retransmission interrupts
                if (!ClientReceiver.retransmit) {

                    //if an acknowledgement is received
                    if (ClientReceiver.ack_received == true) {

                        //while the current packet is a valid packet that is within the congestion window bounds
                        while (currpack <= ClientMachine.packetnumber && currpack <= (ClientReceiver.currentack + cwnd - 1)) {

                            try {
                                //if the packet has not been previously sent
                                if (!sentpackets[currpack]) {
                                    pack_to_send = currpack;

                                    //send current packet to the receiver
                                    send = serialize(ClientMachine.packets.get(pack_to_send));
//                                    System.out.println("packet size: "+ send.length);
                                    pack = new DatagramPacket(send, send.length, address, ClientMachine.server_port);
                                    d.send(pack);

                                    //display transfer speed with every packet that is sent if quiet mode is not enabled
                                    if (ClientMachine.quiet == false) {
                                        System.out.println("S: packet sent: " + pack_to_send);
                                        speed = (packetsizewithheader * cwnd) * 8 / 1024;
                                        if (currpack % 100 == 0) {
                                            System.out.println("\nCurrent "
                                                    + "Transfer rate: " + speed
                                                    + " Kilo bits.\nCurrent "
                                                    + "Sending rate: " + cwnd+" "
                                                    + "packets.\n");
                                        }
                                    }

                                    //dispose all acknowledgements that are less than the currently transmitted packet's sequence number
                                    try {
                                        if (next_packet_to_send.get(0) < currpack) {
                                            next_packet_to_send.remove(0);
                                        }
                                    } catch (Exception e) {
                                    }

                                    //update current packet sent
                                    sentpackets[pack_to_send] = true;

                                }

                                //get next packet
                                currpack++;
                            } catch (Exception e) {
                            }
                        }
                    }

                    //if retransmission or timeout interrupt occurs 
                } else {
                    currpack = ClientReceiver.currentack;
                    send = serialize(ClientMachine.packets.get(ClientReceiver.currentack));
                    pack = new DatagramPacket(send, send.length, address, ClientMachine.server_port);
                    d.send(pack);
                    ClientReceiver.retransmit = false;
                    ClientReceiver.timeout = false;
                }
            }

            //after the whole file has been transfered
            System.out.println("\nS: transfer complete");

            if (ClientMachine.quiet == false) {
                System.out.println("S: Sending FIN packet");
            }
            //send packet to terminate connection
            send = serialize(new packet(0, 0, 0, true, 0, null));
            pack = new DatagramPacket(send, send.length, address, ClientMachine.server_port);
            d.send(pack);

            //display speed for transfer of connection terminating packet
            speed = (packetsizewithheader * cwnd) * 8 / 1024;
            System.out.println("Current Transfer rate: " + speed + " Kilo bits.");
            System.out.println("Current Sending rate: " + cwnd + " packets.\n");
            System.out.println("S: FIN packet sent");

            //calculate checksum of the whole file for the state before it was sent
            byte[] file = new byte[(ClientMachine.packetsize * ClientMachine.packetnumber)];
            for (int i = 1; i <= ClientMachine.packetnumber; i++) {
                for (int j = 0; j < ClientMachine.packetsize; j++) {
                    file[j + ((i - 1) * ClientMachine.packetsize)] = ClientMachine.packets.get(i).data[j];
                }
            }

            //display file's checksum
            File f = new File(ClientMachine.file);
            System.out.println("File checksum: "+fileCheck(f));

        } catch (SocketException ex) {
            Logger.getLogger(ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        } //catch (InterruptedException ex) {
        catch (Exception ex) {
            Logger.getLogger(ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //this method returns the checksum of the array given to it as a paramater, using MD5
    public static int getCheckSum(byte[] arr) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(arr, 0, arr.length);
        BigInteger i = new BigInteger(1, md.digest());
        int ans = i.intValue();
        return ans;
    }

    //this method returns the serialized byte array of the object given to it as a parameter
    public static byte[] serialize(Object obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(out);
        } catch (IOException e) {
            System.out.println("In seralize");
            e.printStackTrace();
        }
        try {
            os.writeObject(obj);
        } catch (IOException e) {
            System.out.println("In serialize afer write");
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static String fileCheck(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(file);
        byte[] dataBytes = new byte[1024];

        int nread = 0;

        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        };

        byte[] mdbytes = md.digest();

        //convert the byte to hex format
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

}

/**
 * This class provides the functionality of the receiver thread for the Sender
 * machine.
 *
 * @author Aditya Advani
 */
class ClientReceiver extends Thread {

    //instance variables
    DatagramSocket d;
    packet p;
    int packetnumber;
    int previousack;
    int dupackcount;

    //static variables shared by sender thread of sender machine
    static int currentack;
    static boolean retransmit;
    static boolean timeout;
    static boolean ack_received;

    //default constructor
    ClientReceiver() {
    }

    //parameterized constructor
    ClientReceiver(DatagramSocket d) {
        this.d = d;
        this.packetnumber = ClientMachine.packetnumber;
    }

    @Override
    //this method contains the business logic of the receiver thread of the sender machine
    public void run() {

        if (ClientMachine.quiet == false) {
            System.out.println("R: started receiver");
        }

        //declare and initialize instance variables
        byte[] send = new byte[2048];
        byte[] receive = new byte[2048];
        DatagramPacket pack;
        previousack = 0;
        currentack = 0;
        dupackcount = 0;
        retransmit = false;
        int retransmitcount = 0;
        int dupackcount = 0;
        boolean ackresent;
        timeout = false;
        int timeoutcount = 0;
        p = new packet();
        p.ACKnum = 0;

        //while the acknowledgement received is of a valid packet 
        while (p.ACKnum <= packetnumber) {
            try {

//Enter sleep here for delay simulation
                //if listening socket not timedout 
                if (!timeout) {
                    previousack = currentack;
                }

                //new datagram packet
                pack = new DatagramPacket(receive, receive.length);
                try {

                    //try to receive the acknowledgement before socket timeout
                    d.receive(pack);
                    d.setSoTimeout(ClientMachine.timeout);
                    ack_received = true;
                } catch (Exception e) {

                    //socket timeout occured
                    timeout = true;
                }

                //if socket did not timeout
                if (!timeout) {

                    //deserialize the byte array to reterieve the packet object
                    p = (packet) (deserialize(pack.getData()));
                    currentack = p.ACKnum;
                    ackresent = p.FIN;

                    if (currentack == 1) {
                        ClientSender.synreceived = true;
                    }

                    // simulating artificial congestion
//                    if(currentack == 299){
//                        retransmit = true;
//                        System.out.println("\nR: simulating artificial congestion");
//                    }
                    //if previous time ack was last, this is the resent ack
                    if (ackresent && ClientMachine.quiet == false) {
//                        retransmit = true;
                        System.out.println("\nR: received a retransmitted ack, lost previously\n");
                    }

                    //current acknowledgement = the number in the acknowledgement header of the received packet
                    if (ClientMachine.quiet == false) {
                        System.out.println("R: received ack: " + p.ACKnum);
                    }

                    //if acknowledgent requests an invalid packet
                    if (p.ACKnum > packetnumber) {
                        break;
                    }

                    //if a duplicate acknowledgement is received
                    if (currentack == previousack) {
                        dupackcount++;

                        //if three duplicate acknowledgements are received, retransmit the packet immediately
                        if (dupackcount == 3) {
                            retransmit = true;
                        }
                    }

                    //if new acknowledgement
                    if (previousack != currentack && !ackresent) {
                        dupackcount = 0;
                        try {

                            //if size of congestion window is greater than the slow start threshold
                            if (ClientSender.cwnd >= ClientSender.ssthresh) {
                                if (ClientMachine.quiet == false) {
                                    if (currentack % 20 == 0) {
                                        System.out.println("\nCCS: congestion avoidance\n");
                                    }
                                }
                                //window size increment = windowsize + 1/windowsize
                                ClientSender.cwndinc = ClientSender.cwndinc + (1 / ClientSender.cwnd);
                                if (ClientSender.cwndinc >= 1) {
                                    ClientSender.cwnd += 1;
                                    ClientSender.cwndinc -= 1;
                                }

                                //if size of the congestion window is less than the slow start threshold
                            } else {
                                if (ClientMachine.quiet == false) {
                                    if (currentack % 20 == 0) {
                                        System.out.println("\nCCS: exponential increase\n");
                                    }
                                }
                                //window size increases exponentially
                                ClientSender.cwnd += 1;
                            }
                        } catch (Exception e) {
                            System.out.println("e- " + e);
                        }
                    }

                    //if the acknowledgement is of a new packet that has not been transmitted
                    if (ClientSender.sentpackets[currentack] == false && retransmit == false) {
                        ClientSender.next_packet_to_send.add(currentack);
                        ack_received = true;

                        //if retransmission interrupt activated
                    } else if (retransmit == true) {
                        ClientSender.ssthresh = (int) (ClientSender.cwnd / 2);
                        ClientSender.cwnd = 1;

                        if (ClientMachine.quiet == false && !ackresent) {
                            System.out.println("\nRetransmitting packet " + currentack + "\n");
                        }
                        ClientSender.next_packet_to_send.add(currentack);
                        ack_received = true;
                        retransmitcount++;
                    }

                    //if timeout interrupt activated
                } else {
                    ClientSender.ssthresh = (int) (ClientSender.cwnd / 2);
                    ClientSender.cwnd = 1;
                    retransmit = true;

                    if (ClientMachine.quiet == false) {
                        System.out.println("\nTimeout. Retransmitting packet " + currentack + "\n");
                    }
                    ClientSender.next_packet_to_send.add(currentack);
                    ack_received = true;
                    timeoutcount++;
                }

            } catch (Exception e) {
//                System.out.println("e: " + e);
            }
        }
        ClientMachine.terminate = true;
    }

    //this method returns a raw object that is the deserialized byte array.
    public static Object deserialize(byte[] data) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(in);
        } catch (IOException e) {
            System.out.println("In deserialize");
            e.printStackTrace();
        }
        try {
            return is.readObject();
        } catch (ClassNotFoundException | IOException e) {
            System.out.println("\n-----RECEIVED A CORRUPT PACKET-----\n");
//            e.printStackTrace();
        }
        return is;
    }

}

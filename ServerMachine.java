
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package project2fcn;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class acts as the receiver class for the file transfer.
 *
 * @author Aditya Advani
 */
public class ServerMachine {

    // static variables are shared and used by sender and receiver threads.
    static HashMap<Integer, Boolean> received = new HashMap<>();
    static int pnum, psize;
    static int port;
    static boolean quiet;
    static String algo;
    static HashMap<Integer, packet> packets = new HashMap<>();
    static ArrayList<Integer> received_packets = new ArrayList<>();
    static boolean filetransferred = false;
    DatagramSocket d;
    DatagramSocket e;
    static InetAddress address;
    static int Client_port;
    static boolean send_SYN_ACK = false;
    static boolean packet_accepted = false;

    //defailt constructor
    ServerMachine() {
    }

    //parameterized constructor
    ServerMachine(int port1, boolean quiet1, String algo1) {
        port = port1;
        quiet = quiet1;
        algo = algo1;
    }

    // main method to initiate the receiver
    public void connect() throws IOException {
        try {

            //create sockets
            d = new DatagramSocket(port);
            e = new DatagramSocket();
            received.put(0, false);

            //create and start receiver and sender threads for Receiver machine.
            Thread SRec = new Thread(new ServerReceiver(d));
            Thread SSend = new Thread(new ServerSender(e));
            SRec.start();
            SSend.start();

        } catch (SocketException ex) {
            Logger.getLogger(ServerMachine.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}

/**
 * This class provides the functionality of the sender thread for the Receiver
 * machine.
 *
 * @author Aditya Advani
 */
class ServerSender extends Thread {

    //instance variables
    DatagramSocket d;
    packet p;
    static boolean resendack = false;

    //default constructor
    ServerSender() {
    }

    //parameterized constructor
    ServerSender(DatagramSocket d1) {
        this.d = d1;
    }

    @Override
    //this method provides the business logic of the sender thread of the receiver machine
    public void run() {
        if (ServerMachine.quiet == false) {
            System.out.println("S: started sender");
        }
        byte[] receive = new byte[2048];
        byte[] send = new byte[2048];
        boolean ackdropped = false;

        //while SYN packet is not received
        while (!ServerMachine.send_SYN_ACK) {
            System.out.print("");
        }

        if (ServerMachine.quiet == false) {
            System.out.println("S: ServerMachine.send_SYN_ACK sending");
        }
        //create first ack after getting SYN packet
        p = new packet();
        p.FIN = false;
        p.ACKnum = 1;
        p.data = null;
        p.sequenceNumber = 0;
        p.dataSize = 0;
        p.CheckSum = 0;

//        System.out.println();
        
        //send the ack
        System.out.println("SYN_ACK: " + ServerMachine.send_SYN_ACK);
        while (ServerMachine.send_SYN_ACK == false) {
            send = serialize(p);
            DatagramPacket ACK = new DatagramPacket(send, send.length, ServerMachine.address, ServerMachine.port + 1);
            System.out.println("ack sent");
            try {
                d.send(ACK);
                ServerMachine.packet_accepted = false;
            } catch (IOException ex) {
                Logger.getLogger(ServerSender.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        int i;

        //send remaining acknowledgements as and when packets arrive
        while (!ServerMachine.filetransferred) {
            try {

                //if a packet is accepted
                if (ServerMachine.packet_accepted == true) {
                    for (i = 0; i < ServerMachine.received.size(); i++) {
                        if (ServerMachine.received.get(i) == false) {
                            break;
                        }
                    }
// simulating artificial congestion
//                    if(i == 300 && !ackdropped){
//                        ackdropped = true;
//                        ServerMachine.packet_accepted = false;
//                        continue;
//                    }

                    //create acknowledgement packet
                    try {
                        if (!resendack) {
                            p.FIN = false;
                            p.ACKnum = i;
                            p.data = null;
                            p.sequenceNumber = 0;
                            p.dataSize = 0;
                        } else {
                            p.FIN = true;
                            p.ACKnum = i;
                            p.data = null;
                            p.sequenceNumber = 0;
                            p.dataSize = 0;
                            resendack = false;
                        }

//Enter sleep here for delay simulation
                        //send acknowledgement
                        send = serialize(p);
//                        System.out.println("size of ack: "+ send.length);
                        DatagramPacket ACKs = new DatagramPacket(send, send.length, ServerMachine.address, ServerMachine.port + 1);
                        d.send(ACKs);
                        ServerMachine.packet_accepted = false;
                        if (ServerMachine.quiet == false) {
                            System.out.println("S: packet requested: " + i);
                        }

                    } catch (Exception e) {
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(ServerSender.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (ServerMachine.quiet == false) {
            System.out.println("S: ending thread since transfer completed");
        }
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
            System.out.println("In serialize after write");
            e.printStackTrace();
        }
        return out.toByteArray();
    }

}

/**
 * This class provides the functionality of the receiver thread for the Receiver
 * machine.
 *
 * @author Aditya Advani
 */
class ServerReceiver extends Thread {

    //instance variables
    DatagramSocket d;
    packet p;
    boolean checkSumMatch;
    boolean resendack = false;
    int lastpacket;

    //default constructor
    ServerReceiver() {
    }

    //parameterized constructor
    ServerReceiver(DatagramSocket d) {
        this.d = d;
    }

    @Override
    //this method contains the business logic of the receiver thread of the sender machine
    public void run() {
        if (ServerMachine.quiet == false) {
            System.out.println("R: started receiver");
        }

        HashMap<Integer, Boolean> droppedflag = new HashMap<>();
        byte[] receive = new byte[2048];
        byte[] send = new byte[2048];
//        System.out.println();
        try {
            //declare and define instance variables
            DatagramPacket pack;
            p = new packet();
            p.FIN = false;
            boolean finish = false;
            boolean complete = false;
            int falsecount = 0;

            //while all the packets have not been successfully received
            while (finish == false || complete == false) {

//Enter sleep here for delay simulation
                try {
                    //receive the packet
                    pack = new DatagramPacket(receive, receive.length);
                    d.receive(pack);
                    p = (packet) (deserialize(pack.getData()));

                    //for SYN1 packet
                    if (p.sequenceNumber == 0 && ServerMachine.send_SYN_ACK == false) {
                        System.out.println("R: Client message: SYN1");
                        lastpacket = p.ACKnum;

                        //get sender machine's address and port
                        ServerMachine.address = pack.getAddress();
                        ServerMachine.Client_port = pack.getPort();

                        //update variables
                        ServerMachine.received.put(0, true);
                        ServerMachine.received.put(1, false);
                        ServerMachine.packet_accepted = true;
                        ServerMachine.send_SYN_ACK = true;
                        continue;
                    }

                    //if not initialized before, initialize now
                    if (!ServerMachine.received.containsKey(p.sequenceNumber)) {
                        ServerMachine.received.put(p.sequenceNumber, false);
                    }

                    //perform checksum cecking
                    checkSumMatch = (p.CheckSum == getCheckSum(p.data));

//artificial or manual packet drop
//                        if ((p.sequenceNumber % 30 == 0) && !droppedflag.containsKey(p.sequenceNumber)) {
//                            droppedflag.put(p.sequenceNumber, true);
//                            checkSumMatch = false;
//                        }
                    if (ServerMachine.quiet == false) {
//                        System.out.println("\nR: received packet: " + p.sequenceNumber);
//                        System.out.println("R: checksum for " + p.sequenceNumber + " before transfer: " + p.CheckSum);
//                        System.out.println("R: checksum for " + p.sequenceNumber + " after transfer:  " + getCheckSum(p.data));
                    }

                    if (p.sequenceNumber == 0) {
                        checkSumMatch = true;
                    }

                    //if checksum matches, accept the packet
                    if (checkSumMatch) {
                        if (ServerMachine.quiet == false) {
                            System.out.println("R: checksum passed for: " + p.sequenceNumber);
                        }
                        ServerMachine.packets.put(p.sequenceNumber, p);
                        ServerMachine.received_packets.add(p.sequenceNumber);

                        //update packet acceptance
                        if (ServerMachine.received.get(p.sequenceNumber) == false) {
                            ServerMachine.received.put(p.sequenceNumber, true);

                            //initialize next expected packet
                            if (!ServerMachine.received.containsKey(p.sequenceNumber + 1)) {
                                ServerMachine.received.put(p.sequenceNumber + 1, false);
                            }
                            //globally notify packet acceptance
                            ServerMachine.packet_accepted = true;

                            //if ack was previously lost by client
                        } else {
//                                System.out.println("resending ack");
                            ServerSender.resendack = true;
                            ServerMachine.packet_accepted = true;
                        }

                        //if checksum match fails, reject the packet
                    } else {
                        if (ServerMachine.quiet == false) {
                            System.out.println("R: checksum failed, rejected packet " + p.sequenceNumber);
                        }
                        ServerMachine.packet_accepted = true;
                    }

                    //if last packet from file received
                    if (p.FIN) {
                        finish = true;
                    }
                    //if last packet is encountered, check if all previous packets have been successfully received
                    if (finish) {
                        for (int k = 1; k < ServerMachine.received.size() - 1; k++) {
                            if (ServerMachine.received.get(k) == false) {
//                                System.out.println(k + ": " + ServerMachine.received.get(k));
                                falsecount++;
                            }
                        }
                        //if all packets have been successfully received, mark the completion of transfer
                        if (falsecount == 0) {
                            complete = true;
                        }
                        falsecount = 0;
                    }

                } catch (Exception e) {
//                    System.out.println("R: e- " + e);
//                    e.printStackTrace();
                }
                //if last packet is encountered, set the flag

            }
            try {
                //accept the connection terminating packet
                pack = new DatagramPacket(receive, receive.length);
                d.receive(pack);
                p = (packet) (deserialize(pack.getData()));
                if (ServerMachine.quiet == false) {
                    System.out.println("\nR: received FIN packet");
                }

                //globally notify that the file has been received successfully
                ServerMachine.filetransferred = true;
                System.out.println("R: Transfer terminated.");
            } catch (Exception e) {
            }

            //get the number of packets and the size of each packet
            ServerMachine.pnum = ServerMachine.packets.size();
            int packetsize = ServerMachine.packets.get(1).data.length;
            ServerMachine.psize = packetsize;

//                    System.out.println("\n\nR: Displaying packets on the client machine: \n");
//                    for (int i = 1; i <= ServerMachine.pnum; i++) {
//                        System.out.print("R: SeqNum: " + ServerMachine.packets.get(i).sequenceNumber + "\tsize: " + ServerMachine.packets.get(i).dataSize + "\t ACKnum: " + ServerMachine.packets.get(i).ACKnum + "\t End of File: " + ServerMachine.packets.get(i).FIN + "\t data: ");
//                        for (int j = 0; j < packetsize; j++) {
//                            System.out.print((char) ServerMachine.packets.get(i).data[j]);
//                        }
//                        System.out.println();
//                    }
            //reconstruct the file from all the received packets
            byte[] file1 = new byte[(ServerMachine.psize * ServerMachine.pnum)];
            if (lastpacket == 0) {
                lastpacket = ServerMachine.psize;
            }
            byte[] file = new byte[(ServerMachine.psize * ((ServerMachine.pnum) - 1)) + lastpacket];

            //merging all the received packets
            for (int i = 1; i <= ServerMachine.pnum; i++) {
                for (int j = 0; j < ServerMachine.psize; j++) {
                    file1[j + ((i - 1) * ServerMachine.psize)] = ServerMachine.packets.get(i).data[j];
                }
            }

            String file_name = "outputfile.bin";
            //resizing new file to original size
            for (int j = 0; j < file.length; j++) {
                file[j] = file1[j];
            }

            //write byte array to a file
            FileOutputStream fos = new FileOutputStream(file_name);
            fos.write(file);
            fos.close();
            File getfile = new File(file_name);

            //display checksum of reconstructed file
            System.out.println("\nR: All packets received and file reconstructed.\n");
            System.out.println("File checksum: " + fileCheck(getfile));

        } catch (IOException ex) {
            Logger.getLogger(ServerReceiver.class.getName()).log(Level.SEVERE, null, ex);
        } //catch (InterruptedException ex) {
        catch (Exception ex) {
            Logger.getLogger(ServerReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //this method returns the checksum of the byte array passed as parameter
    public static int getCheckSum(byte[] arr) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(arr, 0, arr.length);
        BigInteger i = new BigInteger(1, md.digest());
        int ans = i.intValue();
        return ans;
    }

    public static String fileCheck(File f) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(f);
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

    //this method returns the raw object of after deserialization of the byte array given as the parameter
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

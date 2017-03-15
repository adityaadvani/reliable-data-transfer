//package project2fcn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Class Client currently reads a file specified by the user and breaks it down
 * into small packets, size of which is specified by the user. these packets are
 * stored in a HashMap for ease of addition and retrieval. The helper class 
 * 'packet' is used to create complete packets with header fields.
 *
 * @author Aditya Advani
 */
public class Client {

    HashMap<Integer, packet> p;
    static int remainder = 0;
    static byte[] bFile;

    /**
     * @param file1 the file to be transfered
     * @return HashMap of packets with key as sequence number
     */
    public HashMap<Integer, packet> getpackets(String file1) {

        //create a file input stream
        FileInputStream fileInputStream = null;

        //read file from this location
        File file = new File(file1);

        //initialize file size
        int filesize = (int) file.length();

        //initialize packet size. changing this value changes overall packet size.
        int packetsize = 1216;
        int remaindersize = filesize % packetsize;
        remainder = remaindersize;

        //starting packet index number
        int packetnumber = 1;

        //initialization of byte array to store packet
        byte[] packetdata = new byte[packetsize];

        //read file in this byte array
        bFile = new byte[filesize];

        //hashmap to store the packets
        HashMap<Integer, byte[]> packs;
        packs = new HashMap<>();

        try {
            //convert file into array of bytes
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bFile);
            fileInputStream.close();

            //variable to check size of data going into the packet
            int sizecheck = -1;

            for (int i = 0; i < bFile.length; i++) {

                //increment for every bit of data added to the packet
                sizecheck++;

                //check if there is space available in current packet to accommodate current data bit
                if (sizecheck < packetsize) {

                    //add data bit to packet
                    packetdata[sizecheck] = bFile[sizecheck + (packetsize * (packetnumber - 1))];

                    //if packet is full
                } else if (sizecheck == packetsize) {
                    
                    //add packet to the packets hashmap
                    packs.put(packetnumber, packetdata);

                    //reinitialize and increment counters
                    sizecheck = 0;
                    packetnumber++;

                    //reinitialize packet to prepare for new data
                    packetdata = new byte[packetsize];

                    //add current data bit to the newly initialized packet
                    packetdata[sizecheck] = bFile[sizecheck + (packetsize * (packetnumber - 1))];
                }

                //if data is over but packet is not full
                if (i == bFile.length - 1) {

                    //create new packet
                    packs.put(packetnumber, packetdata);
                }
            }

//            //display the whole file
//            System.out.println("\nTotal file size: "+filesize);
//            System.out.println("\nfile: ");
//            for (int i = 0; i < bFile.length; i++) {
//                System.out.print((char) bFile[i]);
//            }

            //create an array list of packets
            HashMap<Integer, packet> packets = new HashMap<>();
            for (int i = 1; i < packetnumber; i++) {
                packets.put(i, new packet(i, packs.get(i).length, 0, false, getCheckSum(packs.get(i)), packs.get(i)));
            }
            packets.put(packetnumber, new packet(packetnumber, packs.get(packetnumber).length, 0, true, getCheckSum(packs.get(packetnumber)), packs.get(packetnumber)));

            p = packets;

        } catch (FileNotFoundException e) {
            System.out.println(e);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return p;
    }
    
    // to generate checksum of the payload
    public int getRemainder(){
        return remainder;
    }

    // to generate checksum of the payload
    public static int getCheckSum(byte[] arr) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(arr, 0, arr.length);
        BigInteger i = new BigInteger(1, md.digest());
        int ans = i.intValue();
        return ans;
    }

}
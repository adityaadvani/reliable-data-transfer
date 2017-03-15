//package project2fcn;

import java.io.Serializable;

/**
 * Class packet is a helper class that enables us to create complete packets 
 * with header and body fields. This class implements Serializable interface 
 * which allows it to be sent over the network in the form of bits and easily 
 * reconstruct in object(packet typecast needed) form upon being received 
 * completely. This class makes use of the 'rawpack' helper class. 
 * 
 * @author Aditya Advani
 */
class packet implements Serializable{
    
    //packet header fields
    int sequenceNumber;
    int dataSize;
    int ACKnum;
    boolean FIN;
    int CheckSum;
    
    //packet data field
    byte[] data;
    
    //default constructor
    packet(){
    }
    
    //parameterized constructor
    packet(int snum, int dsize, int ack, boolean eof, int cs, byte[] d){
        this.sequenceNumber=snum;
        this.dataSize=dsize;
        this.ACKnum=ack;
        this.FIN=eof;
        this.CheckSum = cs;
        this.data=d;
    }
}

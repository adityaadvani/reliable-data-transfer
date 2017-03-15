//package project2fcn;

import java.io.IOException;

/**
 * The class fcntcp provides an interface to initiate the sender and receiver
 * machines with the optional adjustments as desired by the user.
 *
 * @author Aditya Advani
 */
public class fcntcp {

    //main method
    public static void main(String[] args) throws IOException {

        // get the number of arguments passed in the command line
        int arglen = args.length;

        // argument iterator
        int i = 0;

        // If client machine instance is created
        if (args[0].equals("-c") || args[0].equals("--client")) {

            // Validate minimum and maximum argument length bounds
            if (arglen < 3) {
                client_usage();
            }

            // declare and initialize variables
            int timeout = 1000;
            boolean timeoutset = false;

            String file = "";
            boolean fileset = false;

            boolean quiet = false;
            boolean quietset = false;

            String algo = "tahoe";
            boolean algoset = false;

            // beginning index of optional arguments
            i = 1; 

            // scan for optional arguments
            while (i < arglen - 2) {

                //give an error if server initiation is attempted
                if (args[i].equals("-s") || args[i].equals("--server")) {
                    i++;
                    client_usage();
                }

                //ignore argument is client initiation is attempted
                if (args[i].equals("-c") || args[i].equals("--client")) {
                    i++;
                }
                
                //get the file from user that is to be transfered
                if (args[i].equals("-f") || args[i].equals("--file")) {
                    i++;

                    //if file has not been set yet
                    if (!fileset) {
                        fileset = true;
                        //if file name is invalid or missing, throw an error
                        if (args[i].charAt(0) == '-') {
                            client_usage();
                            //set file name
                        } else {
                            file = args[i];
                            i++;
                        }
                        //if file name is already set, throw an error
                    } else {
                        client_usage();
                    }

                }

                //get the socket timeout time interval from the user
                if (args[i].equals("-t") || args[i].equals("--timeout")) {
                    i++;

                    //if timeout has not been set yet
                    if (!timeoutset) {
                        timeoutset = true;
                        //if timeout is missing or has an invalid character, throw an error
                        if (args[i].charAt(0) == '-') {
                            client_usage();
                            //set timeoit
                        } else {
                            timeout = Integer.parseInt(args[i]);
                            i++;
                        }
                        //if timeout interval has already been set, throw an error
                    } else {
                        client_usage();
                    }
                }

                //if quiet mode is enabled
                if (args[i].equals("-q") || args[i].equals("--quiet")) {
                    //if quiet mode has not been set
                    if (!quietset) {
                        quietset = true;
                        //set quiet mode to true
                        quiet = true;
                        i++;
                        //if quiet mode has already been set, throw an error
                    } else {
                        client_usage();
                    }
                }

                //select algorithm to be used for congestion control. default is tahoe
                if (args[i].equals("-a") || args[i].equals("--algorithm")) {
                    i++;

                    //if algorithm has not been set
                    if (!algoset) {
                        algoset = true;
                        //if entered algorithm is not a valid option, throw an error
                        if (args[i].charAt(0) == '-') {
                            client_usage();
                            //set algorithm to be used for congestion control
                        } else {
                            algo = args[i];
                            i++;
                        }
                        //if algorithm has already been set, throw an error
                    } else {
                        client_usage();
                    }

                    if (!algo.equals("tahoe")) {
                        System.out.println("Algorithm not supported");
                        client_usage();
                    }
                }

                // scan for invalid arguments
                if (!(args[i].equals("-c"))
                        && !(args[i].equals("--client"))
                        && !(args[i].equals("-f"))
                        && !(args[i].equals("--file"))
                        && !(args[i].equals("-t"))
                        && !(args[i].equals("--timeout"))
                        && !(args[i].equals("-q"))
                        && !(args[i].equals("--quiet"))
                        && !(args[i].equals("-a"))
                        && !(args[i].equals("--algorithm"))
                        && (i != arglen - 2)) {
                    System.err.println("\nIllegal argument entered. Please "
                            + "enter only valid arguments.\n");
                    client_usage();
                }

            }

            // address of server to connect to
            String server_address = args[arglen - 2];

            // address of server port to connect to
            int port = Integer.parseInt(args[arglen - 1]);

            // validate user input port number
            if (args[arglen - 1].length() != 4) {
                System.err.println("\nInvalid port number.\n");
                client_usage();
            }
            for (int z = 0; z < 4; z++) {
                if ((int) args[arglen - 1].charAt(z) < 48
                        || (int) args[arglen - 1].charAt(z) > 57) {
                    System.err.println("\nInvalid port number.\n");
                    client_usage();
                }
            }

            System.out.println("");
            //display processed values
            String client_str = "CLIENT MACHINE:\nServer address: "
                    + server_address + "\nport: " + port + "\ntimeout: "
                    + timeout + "ms\nfile: " + file + "\nquiet: " + quiet
                    + "\nalgo: " + algo;
            System.out.println(client_str);

            //instantiate client object
            ClientMachine c = new ClientMachine(
                    server_address, port, timeout, file, quiet, algo
            );

            //call ClientMachine's initiator method to create sender and receiver threads
            System.out.println("\n");
            c.connect();

        }

        // If server machine instance is created
        if (args[0].equals("-s") || args[0].equals("--server")) {

            // Validate minimum and maximum argument length bounds
            if (arglen < 2) {
                server_usage();
            }

            // declare variables
            boolean quiet = false;
            boolean quietset = false;

            String algo = "tahoe";
            boolean algoset = false;

            i = 1; // beginning index of optional arguments

            // scan for optional arguments
            while (i < arglen - 1) {

                //give an error if client initiation is attempted
                if (args[i].equals("-c") || args[i].equals("--client")) {
                    i++;
                    server_usage();
                }

                //ignore argument if server initiation is attempted
                if (args[i].equals("-s") || args[i].equals("--server")) {
                    i++;
                }

                //if quiet mode is enabled
                if (args[i].equals("-q") || args[i].equals("--quiet")) {
                    //if quiet mode has not been set
                    if (!quietset) {
                        quietset = true;
                        //set quiet mode to true
                        quiet = true;
                        i++;
                        //if quiet mode has already been set, throw an error
                    } else {
                        server_usage();
                    }
                }

                //select algorithm to be used for congestion control. default is tahoe
                if (args[i].equals("-a") || args[i].equals("--algorithm")) {
                    i++;

                    //if algorithm has not been set
                    if (!algoset) {
                        algoset = true;
                        //if entered algorithm is not a valid option, throw an error
                        if (args[i].charAt(0) == '-') {
                            server_usage();
                            //set algorithm to be used for congestion control
                        } else {
                            algo = args[i];
                            i++;
                        }
                        //if algorithm has already been set, throw an error
                    } else {
                        server_usage();
                    }

                    if (!algo.equals("tahoe")) {
                        System.out.println("Algorithm not supported");
                        server_usage();
                    }
                }

                // scan for invalid arguments
                if (!(args[i].equals("-s"))
                        && !(args[i].equals("--server"))
                        && !(args[i].equals("-q"))
                        && !(args[i].equals("--quiet"))
                        && !(args[i].equals("-a"))
                        && !(args[i].equals("--algorithm"))
                        && (i != arglen - 1)) {
                    System.err.println("\nIllegal argument entered. Please "
                            + "enter only valid arguments.\n");
                    server_usage();
                }

            }

            // server port to connect to
            int port = Integer.parseInt(args[arglen - 1]);

            // validate user input port number
            if (args[arglen - 1].length() != 4) {
                System.err.println("\nInvalid port number.\n");
                server_usage();
            }
            for (int z = 0; z < 4; z++) {
                if ((int) args[arglen - 1].charAt(z) < 48
                        || (int) args[arglen - 1].charAt(z) > 57) {
                    System.err.println("\nInvalid port number.\n");
                    server_usage();
                }
            }

            System.out.println("");
            //display processed values
            String server_str = "SERVER MACHINE:\nport:" + port + "\nquiet: " 
                    + quiet + "\nalgo: " + algo;
            System.out.println(server_str);

            //instantialte client object.
            ServerMachine s = new ServerMachine(
                    port, quiet, algo
            );

            //call ServerMachine's initiator method to create sender and receiver threads
            System.out.println("\n");
            s.connect();

        }

    }

    //display client usage message in case of wrong input arguments
    public static void client_usage() {
        System.err.println("CLIENT USAGE MESSAGE");
        System.err.println("Usage: java fcntcp -c [options] server_address server_port");
        System.err.println("\n\noptions:\n-c, --client:  run as client.\n-f "
                + "<file>, --file: specify file for client to send.\n-t <#>, "
                + "--timeout: timeout in milliseconds for retransmit timer. "
                + "Default to 1000 mS (1 second) if not specified\n-q, --quiet:"
                + " do not output detailed diagnostics.\n-a {tahoe}, "
                + "--algorithm: specify congestion control algorithm\n\n"
                + "server_address: address of server to connect to.\nserver_port:"
                + " primary connection port.\n");
        throw new IllegalArgumentException();
    }
    
    //display server usage message in case of wrong input arguments
    public static void server_usage() {
        System.err.println("SERVER USAGE MESSAGE");
        System.err.println("Usage: java fcntcp -s [options] server_port");
        System.err.println("\n\noptions:\n-s, --cserver:  run as server.\n-q, "
                + "--quiet: do not output detailed diagnostics.\n-a {tahoe}, "
                + "--algorithm: specify congestion control algorithm\n\n"
                + "server_port: primary connection port.\n");
        throw new IllegalArgumentException();
    }

}

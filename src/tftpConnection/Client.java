package tftpConnection;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.InputMismatchException;

/**
 * @Author Eric Morrissette, Andrew Nguyen, Benjamin
 * 
 *         TFTP Client
 */

public class Client extends TFTPConnection {

    // Test Variable
    private byte operation; // Operation type to be requested
    private String input;
    private ErrorSimulator errorSim;
    private int errorSimMode = 0;
    private int errorSimBlock = 0;
    private int errorSimDelay = 0;
    private int errorSimType = 0;
    private boolean errorSim4OpCode, errorSim4File, errorSim4Mode;

    public Client(ErrorSimulator errorSim) {
	this.verbose = true;
	this.errorSim = errorSim;

    }

    /**
     * Basic UI, gets input from user ** WILL be upgraded in future iterations.
     * 
     * @author Benjamin, Bloo, Eric
     */
    public synchronized void userInterface() {
	String localFile = null, serverFile = null; /*
						     * localFile: Local file to be written or read serverFile: File to
						     * be read or written on the server
						     */
	int sendPort = SERVER_PORT;

	/* Continue execution until exit is issued */
	while (true) {
	    while (true) {
		try {
		    this.print("RRQ(1), WRQ(2), settings(3), quit(4): ");
		    while (input == null) {
			try {
			    wait();
			} catch (InterruptedException e) {
			    e.printStackTrace();
			}
		    }
		    operation = Byte.valueOf(input);
		    input = null;
		    notifyAll();
		    break;
		} catch (NumberFormatException e) {
		    println("Invalid input!");
		    input = null;
		    notifyAll();
		}
	    }

	    /*
	     * Condition 1 Check whether the input was either a RRQ(1) or WRQ(2)
	     */
	    if (operation == 1 || operation == 2) {
		while (true) { // get file name

		    try {
			print("Enter local File name (don't forget \"\\\\\" in the the file path): ");
			while (input == null) {
			    try {
				wait();
			    } catch (InterruptedException e) {
				e.printStackTrace();
			    }
			}
			localFile = input;
			input = null;
			notifyAll();
			break;
		    } catch (InputMismatchException e) {
			println("Invalid input!");
			input = null;
			notifyAll();
		    }
		}

		while (true) { // get file name

		    try {
			print("Enter server File name (don't forget \"\\\\\"): ");
			while (input == null) {
			    try {
				wait();
			    } catch (InterruptedException e) {
				e.printStackTrace();
			    }
			}
			serverFile = input;
			input = null;
			notifyAll();
			break;
		    } catch (InputMismatchException e) {
			println("Invalid input!");
			input = null;
			notifyAll();
		    }
		}

		establishConnection(operation, localFile, serverFile, sendPort, errorSimMode, errorSimBlock,
			errorSimDelay);
	    }

	    /*
	     * Condition 2 Check whether the input was settings(3) Settings consists of
	     * modifying the Verbose mode and Test Mode
	     * 
	     */
	    else if (operation == 3) {

		while (true) { // get transfer mode
		    try {
			print("Verbose mode (true/false): ");
			while (input == null) {
			    try {
				wait();
			    } catch (InterruptedException e) {
				e.printStackTrace();
			    }
			}
			if (input.equals("1") || input.equals("true") || input.equals("True")) {
			    verbose = true;
			} else if (input.equals("2") || input.equals("false") || input.equals("False")) {
			    verbose = false;
			} else {
			    throw new InputMismatchException();
			}

			input = null;
			notifyAll();
			break;
		    } catch (InputMismatchException e) {
			println("Invalid input!");
			input = null;
			notifyAll();
		    }
		}

		while (true) { // get transfer mode
		    try {
			print("Test mode (true/false): ");
			while (input == null) {
			    try {
				wait();
			    } catch (InterruptedException e) {
				e.printStackTrace();
			    }
			}

			if (input.equals("1") || input.equals("true") || input.equals("True")) {
			    sendPort = ESIM_PORT; // Packets are sent through ErrorSimulator to check for any problem
			    input = null;
			    while (true) {
				try {
				    /*
				     * Choose testing Type
				     */
				    print("Test Type : LOSE_PACKET(1), DELAY_PACKET(2), \n DUPLICATE_PACKET (3), INVALID_FORMAT(4), UNKNOWN_TID (5): ");
				    while (input == null) {
					try {
					    wait();
					} catch (InterruptedException e) {
					    e.printStackTrace();
					}
				    }

				    if (input.equals("1")) { // Lose Packet Simulation
					errorSimMode = 1;
					getPacketErrorSimBlock();
					getPacketErrorSimType();

				    } else if (input.equals("2")) { // Delay Packet Simulation
					errorSimMode = 2;
					getPacketErrorSimBlock();
					getPacketErrorSimType();
					getPacketSimDelay();

				    } else if (input.equals("3")) { // Duplicate Packet Simulation
					errorSimMode = 3;
					getPacketErrorSimBlock();
					getPacketErrorSimType();
					getPacketSimDelay();

				    } else if (input.equals("4")) { // Simulate invalid packet format
					errorSimMode = 4;
					getPacketErrorSimBlock();
					getPacketErrorSimType();
					invalidFormatSimulation();

				    } else if (input.equals("5")) { // Simulate unknown TID
					errorSimMode = 5;
					getPacketErrorSimBlock();
					getPacketErrorSimType();

				    } else {
					throw new InputMismatchException();
				    }

				    // set parameters for error simulation
				    errorSim.setParameters(errorSimMode, errorSimBlock, errorSimDelay, errorSimType,
					    errorSim4OpCode, errorSim4File, errorSim4Mode);

				    input = null;
				    notifyAll();
				    break;
				} catch (InputMismatchException e) {
				    println("Invalid input!");
				    input = null;
				    notifyAll();
				}
			    }

			} else if (input.equals("2") || input.equals("false") || input.equals("False")) {
			    sendPort = SERVER_PORT; // Packets are sent directly to Server port.
			} else {
			    throw new InputMismatchException();
			}

			input = null;
			notifyAll();
			break;
		    } catch (InputMismatchException e) {
			println("Invalid input!");
			input = null;
			notifyAll();
		    }
		}

	    }

	    /*
	     * Condition 3 Check whether the input was quit(4) This will exit the loop/the
	     * program by changing "cont" to false
	     */
	    else if (operation == 4) {
		break;
	    }

	    /*
	     * Condition 4 Check whether the input was anything else besides 1,2,3,4 Nothing
	     * will happen and the loop cycles back.
	     */
	    else {
		println("Invalid input! enter 1, 2, 3 or 4");
	    }
	}
	print("Program is now closing...\n");
	for (int i = 0; i < 5; i++) {
	    if (i > 3) {
		print("Good bye!");
		timeDelay();
	    } else {
		timeDelay();
		print("..");
	    }
	}
	System.exit(0);
    }

    /**
     * Time delay to visually show the effects in the UI when options are selected.
     * Used for closing a program.
     * 
     * @author Andrew
     */
    public void timeDelay() {
	try {
	    Thread.sleep(500);
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Establishes either a WRQ or RRQ connection to the server, depending on user
     * specification
     * 
     * @author Benjamin, Andrew, Eric, BLoo
     */
    public void establishConnection(byte requestType, String localFile, String serverFile, int port, int errorSimMode,
	    int errorSimBlock, int errorSimDelay) {
	ArrayList<byte[]> data = null;
	DatagramSocket connectionSocket;
	DatagramPacket ackPacket;

	if (requestType == TFTPPacket.OP_WRQ) { // Check if the request was a write operation (2).
	    try {
		data = readFile(localFile); // Save localFile to a temp byte array. data will then be used to write to
					    // destination.
	    } catch (IOException e) {
		e.printStackTrace();
		System.exit(1);
	    }
	}

	connectionSocket = waitForSocket(); // Requests usable socket. If success, new DatagramSocket()

	// Setting up time to wait for data before timeout and retransmission.
	try {
	    connectionSocket.setSoTimeout(2000); // 2 seconds
	} catch (Exception se) {
	    se.printStackTrace();
	    System.exit(1);
	}

	try {
	    send(TFTPPacket.createRQ(requestType, serverFile.getBytes(), MODE_OCTET), connectionSocket,
		    InetAddress.getLocalHost(), port);

	    // New input for timeout and retransmission
	    // loop begins at 0 and increments as socket times out. Goes up every 2 seconds.
	    // If the data package is successfully transfered, leave loop and continue
	    // connection.

	    if (requestType == TFTPPacket.OP_WRQ) {
		ackPacket = receive(connectionSocket); // Receive a packet using the connection Socket
		if (TFTPPacket.getType(ackPacket) == TFTPPacket.OP_ACK) { // If server has given acknowledge to write
		    sendFile(data, ackPacket.getSocketAddress(), connectionSocket);

		} else if (TFTPPacket.getType(ackPacket) == TFTPPacket.OP_ERROR) {
		    System.err.println("\n" + TFTPPacket.packetToString(ackPacket)); // if the error packet hasn't
										     // already
										     // been printed
		}
	    } else if (requestType == TFTPPacket.OP_RRQ) {
		receiveFile(connectionSocket, localFile);

	    }
	} catch (UnknownHostException e) {
	    e.printStackTrace();
	    System.exit(1);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public void invalidFormatSimulation() {

	print("Select which parts of the packet you would like to wrongfully format (y/n)");
	print("OpCode?(y/n): ");
	getInvalidFormatInput(errorSim4OpCode);
	print("File?(y/n): ");
	getInvalidFormatInput(errorSim4File);
	print("Mode?(y/n): ");
	getInvalidFormatInput(errorSim4Mode);

    }

    public void getInvalidFormatInput(boolean packetItem) {

	while (true) {
	    input = null;
	    try {

		while (input == null) {
		    try {
			wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}

		if (input.equals("y") || input.equals("Y")) {
		    packetItem = true;
		} else if (input.equals("n") || input.equals("N")) {
		    packetItem = false;
		} else {
		    throw new InputMismatchException();
		}

		input = null;
		notifyAll();
		break;
	    } catch (InputMismatchException e) {
		println("Invalid input!");
		input = null;
		notifyAll();
	    }
	}

    }

    /**
     * Get Packet block # to be modified during error Simulation
     * 
     * @author Eric
     */
    public void getPacketErrorSimBlock() {

	while (true) {
	    input = null;
	    try {
		print("Block #(1 - 65000): ");
		while (input == null) {
		    try {
			wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}

		try {
		    errorSimBlock = Integer.parseInt(input);
		} catch (NumberFormatException e) {
		    throw new InputMismatchException();
		}

		input = null;
		notifyAll();
		break;
	    } catch (InputMismatchException e) {
		println("Invalid input!");
		input = null;
		notifyAll();
	    }
	}
    }

    /**
     * Get Packet delay, used for delay, and duplicate error Simulations
     * 
     * @author Eric
     */
    public void getPacketSimDelay() {

	while (true) {
	    input = null;
	    try {
		print("Delay (ms):  ");
		while (input == null) {
		    try {
			wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}

		try {
		    errorSimDelay = Integer.parseInt(input);
		} catch (NumberFormatException e) {
		    throw new InputMismatchException();
		}

		input = null;
		notifyAll();
		break;
	    } catch (InputMismatchException e) {
		println("Invalid input!");
		input = null;
		notifyAll();
	    }
	}
    }

    /**
     * Get Packet Type to be modified during error Simulation
     * 
     * @author Eric
     */
    public void getPacketErrorSimType() {

	while (true) {
	    input = null;
	    try {
		print("Packet type? RRQ(1), WRQ(2), DATA(3), ACK(4) ");
		while (input == null) {
		    try {
			wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}

		if (input.equals("1")) {
		    errorSimType = 1;
		} else if (input.equals("2")) {
		    errorSimType = 2;
		} else if (input.equals("3")) {
		    errorSimType = 3;
		} else if (input.equals("4")) {
		    errorSimType = 4;
		} else {
		    throw new InputMismatchException();
		}

		input = null;
		notifyAll();
		break;
	    } catch (InputMismatchException e) {
		println("Invalid input!");
		input = null;
		notifyAll();
	    }
	}
    }

    /**
     * closes the datagram socket and quits the program
     * 
     * @Eric
     */
    public void closeConnection(DatagramSocket socket) {
	socket.close();
	System.exit(1);
    }

    /*
     * synchronized takeInput To ensure that threads are aware of the changes made
     * from other threads. This will allow threads to access in an atomic way.
     */
    @Override
    public synchronized void takeInput(String s) {
	while (input != null) {
	    try {
		wait();
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
	input = s;
	notifyAll();
    }

}

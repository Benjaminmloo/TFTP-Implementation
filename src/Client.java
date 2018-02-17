import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * @Author Eric Morrissette, Andrew Nguyen
 * 
 *         TFTP Client
 */

public class Client extends TFTPConnection {
	
	//	Test Variable
	byte operation;		//	Operation type to be requested
	
	public Client() {
		this.verbose = true;
	}

	/**
	 * Establishes either a WRQ or RRQ connection to the server, depending on user
	 * specification
	 */
	public void establishConnection(byte requestType, String localFile, String serverFile, int port) {
		ArrayList<byte[]> data = null;
		DatagramSocket connectionSocket;
		DatagramPacket ackPacket;

		if (requestType == OP_WRQ) {
			try {
				data = readFile(localFile);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		connectionSocket = waitForSocket();

		try {
			send(createRQ(requestType, serverFile.getBytes(), MODE_OCTET), connectionSocket, InetAddress.getLocalHost(),
					port);

			if (requestType == OP_WRQ) {
				ackPacket = receive(connectionSocket);
				sendFile(data, ackPacket.getSocketAddress(), connectionSocket);
			} else if (requestType == OP_RRQ) {
				receiveFile(connectionSocket, localFile);
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * closes the datagram socket and quits the program
	 */
	public void closeConnection(DatagramSocket socket) {

		socket.close();
		System.exit(1);
	}

	/**
	 * For testing purpose - JUnit hard set operation
	 * @param testSubject
	 */
	public void setOperation(byte testSubject) {
		operation = testSubject;
	}
	
	/**
	 * For testing purpose - JUnit
	 */
	public byte getOperation() {
		return operation;
	}
	
	/**
	 * Basic UI, gets input from user ** WILL be upgraded in future iterations.
	 */
	public void userInterface() {

		Scanner n = new Scanner(System.in);	//	Scanner for user input
		String localFile = null, serverFile = null; /* 	localFile: Local file to be written or read 
														serverFile: File to be read or written on the server	*/
		boolean cont = true;	
		int sendPort = ESIM_PORT;	// Error simulator Port #

		/*	Continue execution until exit is issued	*/
		while (cont) {
			//
			while (true) {
				try {
					System.out.print("RRQ(1), WRQ(2), settings(3), quit(4): ");
					operation = n.nextByte();
					break;
				} catch (InputMismatchException e) {
					System.out.println("Invalid input!");
					n.next();
				}

			}

			if (operation == 1 || operation == 2) {
				while (true) { // get file name

					try {
						System.out.print("Enter local File name (don't forget \"\\\\\"): ");
						localFile = n.next();
						break;
					} catch (InputMismatchException e) {
						System.out.println("Invalid input!");
						n.next();
					}
				}

				while (true) { // get file name

					try {
						System.out.print("Enter server File name (don't forget \"\\\\\"): ");
						serverFile = n.next();
						break;
					} catch (InputMismatchException e) {
						System.out.println("Invalid input!");
						n.next();
					}
				}

				establishConnection(operation, localFile, serverFile, sendPort);
			} else if (operation == 3) {
				while (true) { // get transfer mode
					try {
						System.out.print("Verbose mode (true/false): ");
						verbose = n.nextBoolean();

						break;
					} catch (InputMismatchException e) {
						System.out.println("Invalid input!");
						n.next();
					}
				}

				while (true) { // get transfer mode
					try {
						System.out.print("Test mode (true/false): ");
						if (n.nextBoolean()) {
							sendPort = ESIM_PORT;
						} else {
							sendPort = SERVER_PORT;
						}
						break;
					} catch (InputMismatchException e) {
						System.out.println("Invalid input!");
						n.next();
					}
				}

			} else if (operation == 4) {
				cont = false;
			} else {
				System.out.println("Invalid input! enter 1, 2, 3 or 4");
			}
		}
		n.close();
	}

	public static void main(String args[]) {
		Client c = new Client();

		// Get information for file transfer
		c.userInterface();
	}
}

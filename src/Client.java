import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.InputMismatchException;

/**
 * @Author Eric Morrissette, Andrew Nguyen
 * 
 *         TFTP Client
 */

public class Client extends TFTPConnection {
	
	//	Test Variable
	private byte operation;		//	Operation type to be requested
	private String input;
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
				if(getType(ackPacket) == OP_ACK) {
					sendFile(data, ackPacket.getSocketAddress(), connectionSocket);
				}else if(getType(ackPacket) == OP_ERROR) {
						System.err.println("\n" + packetToString(ackPacket)); //if the error packet hasn't already been printed
				}
			} else if (requestType == OP_RRQ) {
				receiveFile(connectionSocket, localFile);
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	public synchronized void userInterface() {
		String localFile = null, serverFile = null; /* 	localFile: Local file to be written or read 
														serverFile: File to be read or written on the server	*/
		boolean cont = true;	
		int sendPort = ESIM_PORT;	// Error simulator Port #

		/*	Continue execution until exit is issued	*/
		while (cont) {
			while (true) {
				try {
					this.print("RRQ(1), WRQ(2), settings(3), quit(4): ");
					while(input == null) {
						try {
							wait();
						}catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
					operation = Byte.valueOf(input);
					input = null;
					break;
				} catch (NumberFormatException e) {
					println("Invalid input!");
					input = null;
				}

			}

			if (operation == 1 || operation == 2) {
				while (true) { // get file name

					try {
						print("Enter local File name (don't forget \"\\\\\" in the the file path): ");
						while(input == null) {
							try {
								wait();
							}catch(InterruptedException e) {
								e.printStackTrace();
							}
						}
						localFile = input;
						input = null;
						
						break;
					} catch (InputMismatchException e) {
						println("Invalid input!");
						input = null;
					}
				}

				while (true) { // get file name

					try {
						print("Enter server File name (don't forget \"\\\\\"): ");
						while(input == null) {
							try {
								wait();
							}catch(InterruptedException e) {
								e.printStackTrace();
							}
						}
						serverFile = input;
						input = null;
						break;
					} catch (InputMismatchException e) {
						println("Invalid input!");
						input = null;
					}
				}

				establishConnection(operation, localFile, serverFile, sendPort);
			} else if (operation == 3) {
				while (true) { // get transfer mode
					try {
						print("Verbose mode (true/false): ");
						while(input == null) {
							try {
								wait();
							}catch(InterruptedException e) {
								e.printStackTrace();
							}
						}
						verbose = Boolean.valueOf(input);
						input = null;
						break;
					} catch (InputMismatchException e) {
						println("Invalid input!");
						input = null;
					}
				}

				while (true) { // get transfer mode
					try {
						print("Test mode (true/false): ");
						while(input == null) {
							try {
								wait();
							}catch(InterruptedException e) {
								e.printStackTrace();
							}
						}
						
						if (Boolean.valueOf(input)) {
							sendPort = ESIM_PORT;
						} else {
							sendPort = SERVER_PORT;
						}
						input = null;
						break;
					} catch (InputMismatchException e) {
						println("Invalid input!");
						input = null;
					}
				}

			} else if (operation == 4) {
				cont = false;
			} else {
				println("Invalid input! enter 1, 2, 3 or 4");
			}
		}
	}

	@Override
	public synchronized void takeInput(String s) {
		input = s;
		notifyAll();
	}
}

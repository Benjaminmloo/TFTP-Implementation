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
	private static int retransmit_limit = 3; // Number of times to retransmit a packet due to delay.
	
	public Client() {
		this.verbose = true;
		
	}

	/**
	 * @author Benjamin, Andrew, Eric
	 * Establishes either a WRQ or RRQ connection to the server, depending on user
	 * specification
	 */
	public void establishConnection(byte requestType, String localFile, String serverFile, int port) {
		ArrayList<byte[]> data = null;
		DatagramSocket connectionSocket;
		DatagramPacket ackPacket;
		boolean packetInOrder; // Check to see if all packets are in order.
		boolean firstPacketSent = true; // First ACK packets sent, don't resend ACK. default to true.

		if (requestType == OP_WRQ) { // Check if the request was a write operation (2).
			try {
				data = readFile(localFile); // Save localFile to a temp byte array. data will then be used to write to destination.
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		connectionSocket = waitForSocket(); // Requests usable socket. If success, new DatagramSocket()
		
		// Setting up time to wait for data before timeout and retransmission.
		try {
			connectionSocket.setSoTimeout(2000); // 2 seconds
		} catch(Exception se) {
			se.printStackTrace();
			System.exit(1);
		}

		try {
			send(createRQ(requestType, serverFile.getBytes(), MODE_OCTET), connectionSocket, InetAddress.getLocalHost(),
					port);
			
			// New input for timeout and retransmission
			// loop begins at 0 and increments as socket times out. Goes up every 2 seconds.
			// If the data package is successfully transfered, leave loop and continue connection.
			for(int i = 0; i<retransmit_limit; i++) {
				try {
					if (requestType == OP_WRQ) {
						ackPacket = receive(connectionSocket); // Receive a packet using the connection Socket
						if(getType(ackPacket) == OP_ACK) { // If server has given acknowledge to write
							sendFile(data, ackPacket.getSocketAddress(), connectionSocket);
							i = retransmit_limit + 1; // Successful - leave loop
						}else if(getType(ackPacket) == OP_ERROR) {
								System.err.println("\n" + packetToString(ackPacket)); //if the error packet hasn't already been printed
						}
					} else if (requestType == OP_RRQ) {
						receiveFile(connectionSocket, localFile);
						i = retransmit_limit + 1; // Successful - leave loop
					}
				} catch(SocketTimeoutException e) {
					if(i == retransmit_limit -1) {
						System.out.println("Unresponsive Transit... Please try again. Attempts: " + retransmit_limit);
						if(firstPacketSent) {
							if(verbose) System.out.println("\nServer timed out. RRQ resent.\n");
							// connectionSocket.send();
						}
					}
				}
			}

//			if (requestType == OP_WRQ) {
//				ackPacket = receive(connectionSocket); // Receive a packet using the connection Socket
//				if(getType(ackPacket) == OP_ACK) { // If server has given acknowledge to write
//					sendFile(data, ackPacket.getSocketAddress(), connectionSocket);
//				}else if(getType(ackPacket) == OP_ERROR) {
//						System.err.println("\n" + packetToString(ackPacket)); //if the error packet hasn't already been printed
//				}
//			} else if (requestType == OP_RRQ) {
//				receiveFile(connectionSocket, localFile);
//			}

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
	 * @author Benjamin, Bloo
	 * Basic UI, gets input from user ** WILL be upgraded in future iterations.
	 */
	public synchronized void userInterface() {
		String localFile = null, serverFile = null; /* 	localFile: Local file to be written or read 
														serverFile: File to be read or written on the server	*/
		boolean cont = true;	
		int sendPort = ESIM_PORT;	// Error simulator Port #23

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
					notifyAll();
					break;
				} catch (NumberFormatException e) {
					println("Invalid input!");
					input = null;
					notifyAll();
				}
			}

			/* Condition 1
			 * Check whether the input was either a RRQ(1) or WRQ(2)
			 */
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
						while(input == null) {
							try {
								wait();
							}catch(InterruptedException e) {
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

				establishConnection(operation, localFile, serverFile, sendPort);
			}
			
			/* Condition 2
			 * Check whether the input was settings(3)
			 * Settings consists of modifying the Verbose mode and Test Mode
			 * 
			 */
			else if (operation == 3) {
				
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
						if(input.equals("1") || input.equals("true") || input.equals("True")) {
							verbose = true;
						} else if(input.equals("2") || input.equals("false") || input.equals("False")) {
							verbose = false;
						}else {
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
						while(input == null) {
							try {
								wait();
							}catch(InterruptedException e) {
								e.printStackTrace();
							}
						}
						
						if(input.equals("1") || input.equals("true") || input.equals("True")) {
							sendPort = ESIM_PORT; // Packets are sent through ErrorSimulator to check for any problem
						} else if(input.equals("2") || input.equals("false") || input.equals("False")) {
							sendPort = SERVER_PORT; // Packets are sent directly to Server port.
						}else {
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
			
			/* Condition 3
			 * Check whether the input was quit(4)
			 * This will exit the loop/the program by changing "cont" to false
			 */
			else if (operation == 4) {
				cont = false;
			} 
			
			/* Condition 4
			 * Check whether the input was anything else besides 1,2,3,4
			 * Nothing will happen and the loop cycles back.
			 */
			else {
				println("Invalid input! enter 1, 2, 3 or 4");
			}
		}
		print("Program is now closing...");
	}

	/*
	 * synchronized takeInput
	 * To ensure that threads are aware of the changes made from other threads.
	 * This will allow threads to access in an atomic way.
	 */
	@Override
	public synchronized void takeInput(String s) {
		while(input != null) {
			try {
				wait();
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		input = s;
		notifyAll();
	}
	/*public static void main(String args[]) {
		Client c = new Client();

		// Get information for file transfer
		c.userInterface();
	}*/
}

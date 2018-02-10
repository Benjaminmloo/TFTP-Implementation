
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * @Author Eric Morrissette
 * 
 *         TFTP Client
 */

public class Client extends TFTPConnection {

	// OPCODES for TFTP transfer
	private static final byte OP_RRQ = 1;
	private static final byte OP_WRQ = 2;
	private static final byte OP_DATA = 3;
	private static final byte OP_ACK = 4;

	private int sendPort;



	public Client() {
		this.verbose = true;
	}

	/**
	 * Establishes either a WRQ or RRQ connection to the server, depending on user
	 * specification
	 */
	public void establishConnection(byte requestType, String localFile, String serverFile) {
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
			send(createRQ(requestType, serverFile.getBytes(), MODE_OCTET), connectionSocket, InetAddress.getLocalHost(), sendPort);

			if(requestType == OP_WRQ) {
				ackPacket = receive(connectionSocket);
				sendFile(data, ackPacket.getSocketAddress(), connectionSocket);
			}else if(requestType == OP_RRQ) {
				receiveFile(connectionSocket, localFile);
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}
	
	protected byte[] createRQ(byte opCode, byte[] file,byte[]  mode) {		 		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(ZERO_BYTE);
			outputStream.write(opCode);
			outputStream.write(file);
			outputStream.write(ZERO_BYTE);
			outputStream.write(mode);
			outputStream.write(ZERO_BYTE);

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return outputStream.toByteArray();
	}

	/**
	 * closes the datagram socket and quits the program
	 */
	public void closeConnection(DatagramSocket socket) {

		socket.close();
		System.exit(1);
	}

	/**
	 * Basic UI, gets input from user ** WILL be upgraded in future iterations.
	 */

	public void setup() {

		Scanner n = new Scanner(System.in);
		String localFile, serverFile;
		byte type;
		

		// Get input from user for file transfer

		while (true) { // get transfer mode
			try {
				System.out.print("Verbose mode (true/false): ");
				verbose = n.nextBoolean();

				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}
		}

		while (true) { // get transfer mode
			try {
				System.out.print("Test mode (true/false): ");
				if (n.nextBoolean()) {
					sendPort = 23;
				} else {
					sendPort = 69;
				}
				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}
		}

		while (true) { // get transfer type

			try {
				System.out.print("RRQ(1) or WRQ(2): ");
				type = n.nextByte();
				if (type == 1 || type == 2) {
					break;
				} else {
					System.out.println("Invalid input! enter WRQ or RRQ");
				}
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}

		}

		while (true) { // get file name

			try {
				System.out.print("Enter local File name (don't forget \"\\\\\"): ");
				localFile = n.next();
				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}
		}

		while (true) { // get file name

			try {
				System.out.print("Enter server File name (don't forget \"\\\\\"): ");
				serverFile = n.next();
				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}
		}
		n.close();
		
		establishConnection(type, localFile, serverFile);
	}

	public static void main(String args[]) {
		Client c = new Client();

		// Get information for file transfer
		c.setup();

	}

}

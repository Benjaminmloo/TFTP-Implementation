
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Author : Eric Morrissette
 * 
 * TFTP Client
 */

public class Client extends TFTPConnection {

	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;

	// OPCODES for TFTP transfer
	private static final byte OP_WRQ = 2;
	private static final byte OP_RRQ = 1;
	private static final byte OP_DATA = 3;
	private static final byte OP_ACK = 4;

	private static byte transferType;
	private String localFileName;
	private String serverFileName;
	private ArrayList<byte[]> splitFile, tempFileToSave;
	private int sendPort;

	int blockNum = 0;
	int blocks;

	private static final byte ZERO_BYTE = 0;
	private static final String mode = "octet";
	private static final byte[] MODE = mode.getBytes();

	public Client() {
		this.verbose = true;
		// Create datagram socket to send and receive packets
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) { // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Establishes either a WRQ or RRQ connection to the server, depending on user
	 * specification
	 */
	public void establishConnection() {
		DatagramSocket connectionSocket;
		try {
			connectionSocket = new DatagramSocket();
			// create message for DatagramPacket
			byte opCode = transferType; // WRQ or RRQ
			byte file[] = serverFileName.getBytes();
	
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try {
				outputStream.write(ZERO_BYTE);
				outputStream.write(opCode);
				outputStream.write(file);
				outputStream.write(ZERO_BYTE);
				outputStream.write(MODE);
				outputStream.write(ZERO_BYTE);
	
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	
			byte msg[] = outputStream.toByteArray();
	
			try {
				send(msg, connectionSocket, InetAddress.getLocalHost(), sendPort);
				blockNum = 1;
				handleRequest(connectionSocket);
	
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		} catch (SocketException e2) {
			e2.printStackTrace();
			System.exit(1);
		}

	}
	
	

	private void handleRequest(DatagramSocket connectionSocket) {
		DatagramPacket newPacket;
		SocketAddress returnAddress;
		byte[] packetData;
		byte[] readData;
		System.out.println("Num Blocks: " + blocks);
		while(true) {
			newPacket = receive(connectionSocket);
			returnAddress = newPacket.getSocketAddress();
			packetData = newPacket.getData();
			// make sure the correct data type is returned according to sent data.
			if ((transferType == OP_WRQ && packetData[1] == OP_ACK) // if WRQ sent , ACK is expected.
					|| (transferType == OP_RRQ && packetData[1] == OP_DATA))// if RRQ sent, DATA is expected
			{
				// connection established begin transfer
				if (transferType == OP_WRQ) { // send DATA to server 1 block at a time.
					if(blockNum <= blocks) { 
						send(createData(blockNum, splitFile.get(blockNum - 1)), connectionSocket, returnAddress);
						blockNum++;
					}else {
						closeConnection();
					}
	
				}else if (transferType == OP_RRQ) { // store received data and send ACK to server.
					// for now assume no packets or lost or duplicated, just process data.
					if(getBlockNum(newPacket) == 1) {
						tempFileToSave = new ArrayList<byte[]>();
					}
					readData = getData(newPacket).getBytes();
	
					tempFileToSave.add(readData);
	
					send(createAck(getBlockNum(newPacket)), connectionSocket, returnAddress);
	
					// if message is less then 512 bytes, transfer is over. else ask for following
					// block of data
					if (readData.length < 512) {
						// file is fully transfered from server, save to appropriate location.
						try {
							saveFile();
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
						closeConnection();
					}	
				}
			} else {
				// connection to server was lost.
				closeConnection();
			}
		}
	}

	/**
	 * Receives data packets from server, handled according to opcode.
	 */

	public void receive() throws IOException {

		byte data[] = new byte[100];
		receivePacket = new DatagramPacket(data, data.length);

		try {
			System.out.println("Waiting...\n");
			// Block until a datagram is received via sendReceiveSocket.
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) { // print out details in verbose mode
			System.out.println("Client: Packet received:");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			int len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.print("Containing [String]: ");

			// Form a String from the byte array.
			String received = new String(data, 0, len);
			System.out.println(received);
			//
			System.out.print("Containing [Bytes]: ");
			// print byte array"
			//
			System.out.println(Arrays.toString(data).substring(0, 11) + "]\n");

		}
	}

	/**
	 * closes the datagram socket and quits the program
	 */
	public void closeConnection() {

		sendReceiveSocket.close();
		System.exit(1);
	}

	/**
	 * Sends ACK packet to server
	 */
	public void sendACK(int port) {

		byte opCode = OP_ACK;
		byte blockNum = 0;

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		outputStream.write(opCode);
		outputStream.write(blockNum);

		byte msg[] = outputStream.toByteArray();

		sendAPacket(msg, port);
	}

	/**
	 * send a packet via datagram socket to server
	 *
	 * @param msg
	 *            - data to be sent via packet to server
	 * 
	 */
	public void sendAPacket(byte[] msg, int port) {

		try {
			sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) { // print out info in verbose mode.
			System.out.println("Client: Sending packet:");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			int len = sendPacket.getLength();
			System.out.println("Length: " + len);

			System.out.print("Containing [String]: ");
			System.out.println(new String(sendPacket.getData(), 0, len)); // or could print "s"
			System.out.print("Containing [Bytes]: ");
			System.out.println(sendPacket.getData()); // or could print "s"

		}
		// Send the packet to the server

		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) {
			System.out.println("Client: Packet sent.\n");
		}

	}

	/**
	 * Split file into 512 byte chunks
	 * 
	 * @param fileName
	 *            - file to be split
	 */
	public void splitFileToSend(String fileName) throws IOException {
		byte[] buffer = new byte[512];
		byte[]data;
		splitFile = new ArrayList<byte[]>();
		FileInputStream in = new FileInputStream(fileName);
		int rc = in.read(buffer);
		while (rc != -1) {
			data = Arrays.copyOf(buffer, rc);
			splitFile.add(data);
			rc = in.read(buffer);
			//if(verbose)System.out.println("New block: " + new String(buffer));
		}
		in.close();

		//if(verbose)System.out.println(spiltFileToString(splitFile));
		blocks = splitFile.size();
	}
	
	public String spiltFileToString(ArrayList<byte[]> data) {
		String out ="{";
		for(byte[] d:data) {
			out += new String(d) +",\n ";
		}
		out += "}";
		return out;
	}

	/**
	 * Saves fully transfered file to appropriate location, used during RRQ transfer
	 * 
	 */
	public void saveFile() throws IOException {

		FileOutputStream out = new FileOutputStream(localFileName);

		for (int i = 0; i < tempFileToSave.size(); i++) {
			out.write(tempFileToSave.get(i));
		}
		out.close();
		blocks = tempFileToSave.size();
	}
	

	private String getLocalFileName() {
		return localFileName;
	}

	/**
	 * Basic UI, gets input from user ** WILL be upgraded in future iterations.
	 */

	public void getUserInput() {

		Scanner n = new Scanner(System.in);

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
				if(n.nextBoolean()){
					sendPort = 23;
				}else {
					sendPort = 69;
				}
				System.out.println("Port to send request: " + sendPort);
				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}
		}

		while (true) { // get transfer type

			try {
				System.out.print("RRQ(1) or WRQ(2): ");
				transferType = n.nextByte();
				if (transferType == 1 || transferType == 2) {
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
				localFileName = n.next();
				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}
		}
		
		while (true) { // get file name

			try {
				System.out.print("Enter server File name (don't forget \"\\\\\"): ");
				serverFileName = n.next();
				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}
		}
		n.close();

	}

	public static void main(String args[]){
		Client c = new Client();

		// Get information for file transfer
		c.getUserInput();

		if (transferType == OP_WRQ) {
			try {
				c.splitFileToSend(c.getLocalFileName());
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// Establish TFTP connection to server
		c.establishConnection();

	}

}

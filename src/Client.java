
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {

	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;

	private static final byte OP_WRQ = 1;
	private static final byte OP_RRQ = 2;
	private static final byte OP_DATA = 3;
	private static final byte OP_ACK = 4;

	private static boolean verbose = true;
	private static byte transferType;
	private static String fileName;
	private byte[] tempFile;
	private ArrayList<String> splitFile, tempFileToSave;

	int blockNum = 0;
	int blocks;

	private static final byte ZERO_BYTE = 0;
	private static final String mode = "octet";
	private static final byte[] MODE = mode.getBytes();

	public Client() {

		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) { // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void establishConnection() {

		// create message for DatagramPacket
		byte opCode = transferType;
		byte file[] = fileName.getBytes();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(opCode);
			outputStream.write(file);
			outputStream.write(ZERO_BYTE);
			outputStream.write(MODE);
			outputStream.write(ZERO_BYTE);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		byte msg[] = outputStream.toByteArray();

		sendAPacket(msg);
	}

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

		if (verbose) {
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

		if ((transferType == OP_WRQ && data[0] == OP_ACK) // if WRQ , ACK is expected.
				|| (transferType == OP_RRQ && data[0] == OP_DATA))// if RRQ, DATA is expected
		{
			// connection established begin transfer
			if (transferType == OP_WRQ) { // send DATA to server.

				if (blockNum < blocks) {
					sendDATA(splitFile.get(blockNum).getBytes(), blockNum);
					blockNum++;
					receive();
				}

				// file transfer done, close the connection
				closeConnection();

			}
			if (transferType == OP_RRQ) { // store received data and send ACK to server.
				// for now assume no packets or lost or duplicated, just process data.

				byte[] tempData = Arrays.copyOfRange(data, 3, data.length);
				System.out.println(new String(tempData, 0, tempData.length));

				tempFileToSave.add(tempData.toString());

				sendACK();

				// if message is less then 512 bytes, transfer is over. else ask for following
				// block of data
				if (tempData.length < 512) {
					saveFile();
					closeConnection();
				}

				receive();

			}
		} else {
			// connection to server was lost.
			closeConnection();
		}
	}
	
	public void closeConnection() {
		
		sendReceiveSocket.close();
		System.exit(1);
	}

	public void sendACK() {

		byte opCode = OP_ACK;
		byte blockNum = 0;

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		outputStream.write(opCode);
		outputStream.write(blockNum);

		byte msg[] = outputStream.toByteArray();

		sendAPacket(msg);
	}

	public void sendDATA(byte[] data, int blockNum) {

		byte opCode = OP_DATA;
		byte blockN = (byte) blockNum;
		byte d[] = data;

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		outputStream.write(opCode);
		outputStream.write(blockNum);

		try {
			outputStream.write(d);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		byte msg[] = outputStream.toByteArray();

		sendAPacket(msg);

	}

	public void sendAPacket(byte[] msg) {

		try {
			sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), 23);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) {
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

	public void splitFileToSend(String fileName) throws IOException {
		byte[] buffer = new byte[512];

		FileInputStream in = new FileInputStream("src/test.txt");
		int rc = in.read(buffer);
		while (rc != -1) {
			splitFile.add(buffer.toString());

			rc = in.read(buffer);
		}

		blocks = splitFile.size();
	}

	public void saveFile() throws IOException {

		FileOutputStream out = new FileOutputStream("src/test.txt");

		for (int i = 0; i < tempFileToSave.size(); i++) {
			out.write(tempFileToSave.get(i).getBytes());
		}

		blocks = splitFile.size();
	}

	public void getUserInput() {

		
		// Get input from user for file transfer

		while (true) { // get transfer mode
			try {
				Scanner n = new Scanner(System.in);
				System.out.print("Verbose mode (true/false): ");
				verbose = n.nextBoolean();
				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}
		}

		while (true) { // get transfer type

			try {
				Scanner n = new Scanner(System.in);
				System.out.print("WRQ(1) or RRQ(2): ");
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
				Scanner n = new Scanner(System.in);
				System.out.print("Enter File name (don't forget \"\"): ");
				fileName = n.next();
				n.close();
				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input!");
			}
		}

		

	}

	public static void main(String args[]) throws IOException {
		Client c = new Client();

		// Get information for file transfer
		c.getUserInput();

		if (transferType == OP_WRQ) {
			c.splitFileToSend("test.txt");
		}

		// Establish TFTP connection to server
		c.establishConnection();

	}
}

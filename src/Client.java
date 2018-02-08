
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
	}

	/**
	 * Establishes either a WRQ or RRQ connection to the server, depending on user
	 * specification
	 */
	public void establishConnection() {
		DatagramSocket connectionSocket;

		if (transferType == OP_WRQ) {
			try {
				splitFile = readFile(localFileName);

				blocks = splitFile.size();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

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
		while (true) {
			newPacket = receive(connectionSocket);
			returnAddress = newPacket.getSocketAddress();
			packetData = newPacket.getData();
			// make sure the correct data type is returned according to sent data.
			if ((transferType == OP_WRQ && packetData[1] == OP_ACK) // if WRQ sent , ACK is expected.
					|| (transferType == OP_RRQ && packetData[1] == OP_DATA))// if RRQ sent, DATA is expected
			{
				// connection established begin transfer
				if (transferType == OP_WRQ) { // send DATA to server 1 block at a time.
					if (blockNum <= blocks) {
						send(createData(blockNum, splitFile.get(blockNum - 1)), connectionSocket, returnAddress);
						blockNum++;
					} else {
						closeConnection(connectionSocket);
					}

				} else if (transferType == OP_RRQ) { // store received data and send ACK to server.
					// for now assume no packets or lost or duplicated, just process data.
					if (getBlockNum(newPacket) == 1) {
						tempFileToSave = new ArrayList<byte[]>();
					}
					readData = getByteData(newPacket);

					tempFileToSave.add(readData);

					send(createAck(getBlockNum(newPacket)), connectionSocket, returnAddress);

					// if message is less then 512 bytes, transfer is over. else ask for following
					// block of data
					System.out.println("Size: " + readData.length);
					if (readData.length < 512) {
						// file is fully transfered from server, save to appropriate location.
						try {
							saveFile(tempFileToSave, localFileName);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
						closeConnection(connectionSocket);
					}
				}
			} else {
				// connection to server was lost.
				closeConnection(connectionSocket);
			}
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

	public static void main(String args[]) {
		Client c = new Client();

		// Get information for file transfer
		c.getUserInput();

		// Establish TFTP connection to server
		c.establishConnection();

	}

}

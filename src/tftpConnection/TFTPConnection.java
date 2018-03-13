package tftpConnection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JScrollBar;
import javax.swing.JTextArea;

/**
 * @author BenjaminP EricM, BenjaminL, AndrewN
 *
 */
public abstract class TFTPConnection {

	// Class Variable definition start
	protected boolean verbose;
	protected JTextArea outputWindow = new JTextArea();
	protected JScrollBar scrollBar = new JScrollBar();

	protected static final String OCTET = "octet";
	protected static final String NETASCII = "netascii";
	protected static final byte[] MODE_OCTET = OCTET.getBytes();
	protected static final byte[] MODE_NETASCII = NETASCII.getBytes();

	protected static final int SERVER_PORT = 69;
	protected static final int ESIM_PORT = 23;
	protected static final int STD_DATA_SIZE = 516;
	protected static final byte ZERO_BYTE = 0;

	protected static final byte OP_RRQ = 1;
	protected static final byte OP_WRQ = 2;
	protected static final byte OP_DATA = 3;
	protected static final byte OP_ACK = 4;
	protected static final byte OP_ERROR = 5;
	private static final int receive_limit = 200;

	private DatagramPacket lastSentPkt, lastRcvPkt;
	// Class Variable definition end

	protected static Map<Byte, String> PacketTypes;
	static {
		PacketTypes = new HashMap<>();
		// RequestTypes.put( (byte) 0, "null"); Perhaps use as a shutdown request?
		PacketTypes.put((byte) OP_RRQ, "RRQ");
		PacketTypes.put((byte) OP_WRQ, "WRQ");
		PacketTypes.put((byte) OP_DATA, "DATA");
		PacketTypes.put((byte) OP_ACK, "ACK");
		PacketTypes.put((byte) OP_ERROR, "ERROR");
	}

	/**
	 * Requests a usable socket until successful
	 * 
	 * @return a usable socket
	 */
	protected DatagramSocket waitForSocket() {
		while (true) {
			try {
				return new DatagramSocket();
			} catch (SocketException e) {
				e.printStackTrace();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * waits a datagram socket on a specied port
	 * 
	 * @param port
	 *            - the port requested for the socket
	 * @return the socket requested
	 */
	protected DatagramSocket waitForSocket(int port) {
		while (true) {
			try {
				return new DatagramSocket(port);
			} catch (SocketException e) {
				e.printStackTrace();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Sends a file over network over tftp
	 * 
	 * @param packet
	 *            - the initial packet, containing return address and file name
	 * @param socket
	 *            - the socket that will be used to send data and receive acks over
	 * @throws IOException
	 */
	protected void sendFile(DatagramPacket packet, DatagramSocket socket) throws IOException {
		ArrayList<byte[]> data = new ArrayList<byte[]>();
		data = readFile(getFileName(packet));
		sendFile(data, packet.getSocketAddress(), socket);
	}

	/**
	 * sends a file over tftp
	 * 
	 * @param data
	 *            - array list holding data blocks to be sent
	 * @param recipientAddress
	 *            - address data is being sent too
	 * @param socket
	 *            - The socket the data will be sent over
	 * @throws IllegalArgumentException
	 */
	protected void sendFile(ArrayList<byte[]> data, SocketAddress recipientAddress, DatagramSocket socket)
			throws IllegalArgumentException {
		DatagramPacket ackPacket;
		for (int i = 1; i <= data.size(); i++) {
			byte sendData[] = data.get(i - 1);
			this.send(createData(i, sendData), socket, recipientAddress);

			ackPacket = receive(socket);

			if (getType(ackPacket) == OP_ERROR) {
				System.err.println("\nError Occured\n" + packetToString(ackPacket));
				return;
			}
			if (getType(ackPacket) != OP_ACK) {
				println("\nReceived packet was not expected");
				throw new IllegalArgumentException();
			}

			if (getBlockNum(ackPacket) != i) {
				println("\nACK for a different Block!");
				throw new IllegalArgumentException();
			}
		}
	}

	/**
	 * receives a file over a datagram socket and saves at specified location this
	 * method receives the read request acknowledge an initial data packet over the
	 * socket
	 * 
	 * @param socket
	 *            - Socket where communication will take place
	 * @param file
	 *            - the file path where the file will be saced
	 */
	protected void receiveFile(DatagramSocket socket, String file) throws IOException {
		receiveFile(receive(socket), socket, file); // Calls the receiveFile below
	}

	/**
	 * receives a file in tftp packets. when all packets have been received the data
	 * is sent to be saved
	 * 
	 * @param packet
	 *            - the initial acknoledge indicating the start of data transfer
	 * @param socket
	 *            - the socket used to communicate
	 * @param file
	 *            - where the received file will be stored
	 */
	protected void receiveFile(DatagramPacket packet, DatagramSocket socket, String file) throws IOException {
		ArrayList<byte[]> data = new ArrayList<byte[]>();
		SocketAddress returnAddress = packet.getSocketAddress();
		DatagramPacket newPacket;

		if (getType(packet) == OP_DATA) { // if the initial packet is a data packet
			if (getBlockNum(packet) == 1) {
				data.add(getByteData(packet));
				send(createAck(1), socket, returnAddress);
				if (getDataLength(packet) != STD_DATA_SIZE - 4) {
					saveFile(data, file);
					return;
				}
			} else {
				return;
			}

		} else if (getType(packet) == OP_ERROR) {
			System.err.println("\\nError Occured\\n" + getErrorMsg(packet));
			return;
		}

		do {
			newPacket = receive(socket);

			if (getType(newPacket) == OP_DATA) {
				data.add(getByteData(newPacket));
				send(createAck(getBlockNum(newPacket)), socket, returnAddress);
			} else if (getType(newPacket) == OP_ERROR) {
				System.err.println("ERROR: " + getErrorMsg(newPacket));
				return;
			} else {
				return;
			}

		} while (getDataLength(newPacket) == STD_DATA_SIZE - 4); // continue while the packets are full
		saveFile(data, file);
	}

	/**
	 * Sends msg to return address over the given socket
	 * 
	 * @param msg
	 *            - byte array to be sent
	 * @param socket
	 *            - socket that will be used to send packet
	 * @param returnAddress
	 *            - address the packet will be send too
	 * 
	 * @author bloo
	 */
	protected void send(byte[] msg, DatagramSocket socket, SocketAddress returnAddress) {
		send(socket, new DatagramPacket(msg, msg.length, returnAddress));
	}

	/**
	 * creates a packet to be sent by base send() method
	 * 
	 * @param msg
	 *            - byte array to be sent
	 * @param socket
	 *            - socket that will be used to send packet
	 * @param address
	 *            - address the packet will be send too
	 * @param port
	 *            - port the packet will be send too
	 * 
	 * @author bloo
	 */
	protected void send(byte[] msg, DatagramSocket socket, InetAddress address, int port) {
		DatagramPacket sendPacket = new DatagramPacket(msg, msg.length, address, port);

		for (byte c : sendPacket.getData()) {
			print(c + "");
		}
		println("");
		send(socket, sendPacket);
	}

	/**
	 * Base send method, used to send sendPacket over given socket
	 * 
	 * @param socket
	 *            - socket the packet will sent over
	 * @param sendPacket
	 *            - packet to be sent, includes destination
	 * 
	 * @author bloo
	 */
	protected void send(DatagramSocket socket, DatagramPacket sendPacket) {
		try {
			if (verbose) {
				println("Sending: ");
				println(packetToString(sendPacket));
			}

			socket.send(sendPacket);

		} catch (IOException e) {
			socket.close();
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Base receive method
	 * 
	 * Receives a DatagramPacket over the given socket
	 * 
	 * @param socket
	 *            - socket packet will be received at
	 * @return receivedPacket unless there is an exception trying to receive
	 * 
	 * @author bloo
	 */
	protected DatagramPacket receive(DatagramSocket socket) {
		return receive(socket, 516);
	}

	/**
	 * Base receive method Receives a DatagramPacket over the given socket
	 * 
	 * @param socket
	 *            - socket to receive from
	 * @param length
	 *            - size of potential packet
	 * @return receivePacket unless there is an exception trying to receive 
	 * 
	 * @author bloo
	 */
	protected DatagramPacket receive(DatagramSocket socket, int length) {
		DatagramPacket receivedPacket;

		receivedPacket = new DatagramPacket(new byte[length], length);

		try {
			while (true) {
				socket.receive(receivedPacket);
				
				if ((getType(receivedPacket) != OP_DATA || lastRcvPkt == null || (getType(lastRcvPkt) == OP_DATA && getBlockNum(receivedPacket) == getBlockNum(lastRcvPkt) + 1))){
					if (verbose) {
						println("Received:");
						println(packetToString(receivedPacket));
					}

					lastRcvPkt = receivedPacket;
					return receivedPacket;

				}
			}
		} catch (SocketTimeoutException e) {
			for (int i = 0; i < receive_limit; i++) {
				try {
					socket.receive(receivedPacket);
					break;
				} catch (IOException e1) {
					if(i % 50 == 0)print("\n");
					print(".");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return new DatagramPacket(new byte[0], 0);

	}

	/**
	 * creates acknowledge packet based on given block number
	 * 
	 * @param blockNum
	 *            - the current number the packet is acknowledging
	 * @return byte array with acknowledge data
	 * @author bloo
	 */
	protected byte[] createAck(int blockNum) {
		byte[] ack = new byte[] { 0, 4, (byte) (blockNum / 256), (byte) (blockNum % 256) };
		return ack;
	}

	/**
	 * Creates Data packet
	 * 
	 * @param blockNum
	 *            - the block num of the blcok being sent
	 * @param data
	 *            - the data being sent
	 * @return packet in the form of a byte array
	 * @author BLoo
	 */
	protected byte[] createData(int blockNum, byte[] data) {
		byte[] packet = new byte[4 + data.length];
		ByteBuffer dBuff = ByteBuffer.wrap(packet);
		dBuff.put(new byte[] { 0, 3, (byte) (blockNum / 256), (byte) (blockNum % 256) });
		dBuff.put(data);
		return packet;
	}

	/**
	 * Creates error packet
	 * 
	 * @param error
	 *            - the corresponding error number
	 * @param data
	 *            - the data being sent
	 * @return packet in the form of a byte array
	 * @author BLoo
	 */
	protected byte[] createError(int error, byte[] msg) {
		byte[] packet = new byte[4 + msg.length];
		ByteBuffer dBuff = ByteBuffer.wrap(packet);
		dBuff.put(new byte[] { 0, 5, (byte) (error / 256), (byte) (error % 256) });
		dBuff.put(msg);
		return packet;
	}

	/**
	 * Retreive the data in a packet in the form of a string
	 * 
	 * @param packet
	 *            - the packet the data will be extracted from
	 * @return the data in the form of a string
	 * @author BLoo
	 */
	protected String getData(DatagramPacket packet) {
		String data = bytesToString(readToStop(4, packet.getData(), packet.getLength()));
		return data;
	}

	/**
	 * Gets the data from a packet as a byte array
	 * 
	 * @param packet
	 *            - where the data will be extracted from
	 * @return the data the packet was holding
	 */
	protected byte[] getByteData(DatagramPacket packet) {
		return readToStop(4, packet.getData(), packet.getLength());
	}

	/**
	 * Parses a tftp packet in byte form and returns info
	 * 
	 * @param packet
	 *            - where data will be extracted
	 * @return contents of a packet
	 * @author bloo
	 */
	protected String getFileName(DatagramPacket packet) {
		return bytesToString(readToStop(2, packet.getData(), packet.getLength()));
	}

	/**
	 * Gets tftp mode from request packet
	 * 
	 * @param packet
	 *            - where data will be extracted
	 * @return requested mode as a String
	 */
	protected String getMode(DatagramPacket packet) {
		int offset = readToStop(2, packet.getData(), packet.getLength()).length + 3; // Find offset of mode field by
																						// reading the first field and
																						// adding that fields length,
																						// accounting for the
																						// terminating zero and initial
																						// offset, to the offset
		return bytesToString(readToStop(offset, packet.getData(), packet.getLength()));
	}

	/**
	 * Gets the error number from error packet
	 * 
	 * @param packet
	 *            - where data will be extracted
	 * @return Error num
	 */
	protected int getError(DatagramPacket packet) {
		return packet.getData()[3];
	}

	/**
	 * Gets error msg from error packet
	 * 
	 * @param packet
	 *            - where data will be extracted
	 * @return Error message
	 */
	protected String getErrorMsg(DatagramPacket packet) {
		return bytesToString(readToStop(4, packet.getData(), packet.getLength()));
	}

	/**
	 * Gets block number from ack or data packets
	 * 
	 * @param packet
	 *            - where data will be extracted
	 * @return block number the packet is holding
	 */
	protected int getBlockNum(DatagramPacket packet) {
		return Byte.toUnsignedInt(packet.getData()[2]) * 256 + Byte.toUnsignedInt(packet.getData()[3]);
	}

	/**
	 * gets the type of packet
	 * 
	 * @param packet
	 *            - where data will be extracted
	 * @return the type of packet
	 */
	protected int getType(DatagramPacket packet) {
		return packet.getData()[1];
	}

	/**
	 * Gets the number of bytes in the data section of a data packet
	 * 
	 * @param packet
	 *            - where the data will be extracted
	 * @return number of bytes in the packets data section
	 */
	protected int getDataLength(DatagramPacket packet) {
		return readToStop(4, packet.getData(), packet.getLength()).length;
	}

	/**
	 * Reads bytes from a byte array to terminating zero or to the end of the
	 * available data.
	 * 
	 * @param index
	 *            - Starting index of the data
	 * @param packet
	 *            - byte array of packet data
	 * @param dataLength
	 *            - the number of bytes of data
	 * @return resulting String of data
	 * 
	 * @author bloo
	 */
	protected byte[] readToStop(int offset, byte[] packet, int dataLength) {
		byte[] data = new byte[512];
		int index;

		/*
		 * Iterate, starting at the given offset as long as a terminating zero or the
		 * end of the array hasn't been reached
		 */
		for (index = offset; index < dataLength && packet[index] != 0; index++) {
			data[index - offset] = packet[index];
		}
		return Arrays.copyOfRange(data, 0, index - offset);
	}

	/**
	 * Converts byte[] to string using utf-8 encoding when available uses default
	 * when utf-8 isn't available
	 * 
	 * @param data
	 * @return decoded Sring
	 */
	protected String bytesToString(byte[] data) {
		try {
			return new String(data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new String(data);
		}
	}

	/**
	 * Split file into 512 byte chunks
	 *
	 * @param fileNameb
	 *            - file to be split
	 * @return
	 * @throws IOException
	 * @author BenjaminP
	 */
	protected ArrayList<byte[]> readFile(String fileName) throws IOException {
		ArrayList<byte[]> parsedData = new ArrayList<byte[]>();
		byte[] buffer = new byte[512];
		byte[] byteData;

		byteData = Files.readAllBytes(Paths.get(fileName));

		for (int i = 0; i < byteData.length; i += 512) {
			if (i + 512 <= byteData.length) {
				buffer = Arrays.copyOfRange(byteData, i, i + 512);
			} else {
				buffer = Arrays.copyOfRange(byteData, i, byteData.length);
			}

			parsedData.add(buffer);
		}
		return parsedData;
	}

	/**
	 * Saves data blocks to a file
	 * 
	 * @param data
	 *            - an arraylist of byte arrays storing the byte data
	 * @param fileName
	 *            - the name of the file where the data will be stored
	 * @return the number blocks saved
	 * @throws IOException
	 * @author Eric
	 */
	protected int saveFile(ArrayList<byte[]> data, String fileName) throws IOException {
		if (new File(fileName).getUsableSpace() < data.size() * 512)
			throw new FullFileSystemException("File " + fileName + "cannot fit the file's " + data.size() * 512
					+ "bytes. File has space " + new File(fileName).getUsableSpace());
		OutputStream file = new FileOutputStream(fileName);

		for (int i = 0; i < data.size(); i++) {
			file.write(data.get(i));
		}

		file.close();
		return data.size();
	}
	
	/**
	 * @author Benjamin
	 * @param b 
	 */
	public void setScrollBar(JScrollBar b)
	{
		this.scrollBar = b;
	}

	/**
	 * @author Benjamin 
	 * Gets textarea attached to child
	 * 
	 * @return
	 */
	public JTextArea getOutputWindow() {
		return this.outputWindow;
	}

	/**
	 * @author Benjamin Prints to UI and console
	 * 
	 * @param s
	 */
	public void print(String s) {
		System.out.println(s);
		this.outputWindow.append(s);
		this.scrollBar.setValue(scrollBar.getMaximum() + 1);
	}

	/**
	 * @author Benjamin Prints to UI and console
	 * 
	 * @param s
	 */
	public void println(String s) {
		System.out.println(s + "\n");
		this.outputWindow.append(s + "\n");
		this.scrollBar.setValue(scrollBar.getMaximum() + 1);
	}

	public abstract void takeInput(String s);

	/**
	 * Parses a tftp packet in byte form and returns relevant information
	 * 
	 * @param packet
	 *            - packet to convert
	 * @return contents of a packet
	 * @author bloo
	 */
	protected String packetToString(DatagramPacket packet) {
		String descriptor = "";
		byte[] data = packet.getData();
		if (data.length > 0) {
			if (data[0] == 0) {
				descriptor += "Type: ";
				if (getType(packet) == OP_RRQ) {
					descriptor += "RRQ\nFile name: " + getFileName(packet) + "\nMode: " + getMode(packet) + "\n";
				} else if (getType(packet) == OP_WRQ) {
					descriptor += "WRQ\nFile name: " + getFileName(packet) + "\nMode: " + getMode(packet) + "\n";
				} else if (getType(packet) == OP_DATA) {
					descriptor += "DATA\nBlock #: " + getBlockNum(packet) + "\nBytes of data: " + getDataLength(packet)
							+ "\n";
				} else if (getType(packet) == OP_ACK) {
					descriptor += "ACK\nBlock #: " + getBlockNum(packet) + "\n";
				} else if (getType(packet) == OP_ERROR) {
					descriptor += "ERROR\nError Num: " + getError(packet) + "\nError Msg: " + getErrorMsg(packet)
							+ "\n";
				}
			}

			return descriptor;
		}

		return null;
	}

	protected class FullFileSystemException extends IOException {
		private static final long serialVersionUID = 7770593212561838179L;

		FullFileSystemException(String msg) {
			super(msg);
		}
	}
}

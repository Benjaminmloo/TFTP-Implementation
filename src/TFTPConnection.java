import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bloo
 *
 */
public abstract class TFTPConnection {
	protected boolean verbose;
	
	protected static final int STD_DATA_SIZE = 516;
	protected static final byte ZERO_BYTE = 0;
	protected static final String OCTET = "octet";
	protected static final String NETASCII = "netascii";
	protected static final byte[] MODE_OCTET = OCTET.getBytes();
	protected static final byte[] MODE_NETASCII = NETASCII.getBytes();
	

	private static final byte OP_RRQ = 1;
	private static final byte OP_WRQ = 2;
	private static final byte OP_DATA = 3;
	private static final byte OP_ACK = 4;
	private static final byte OP_ERROR = 5;

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
	
	protected DatagramSocket waitForSocket() {
		while(true) {
			try {
				return new DatagramSocket();
			}catch(SocketException e)
			{
				e.printStackTrace();;
			}			
		}		
	}
	
	protected void sendFile(DatagramPacket packet, DatagramSocket socket) throws IOException{
		ArrayList<byte[]>data = new ArrayList<byte[]>();
		data = readFile(getFileName(packet));
		sendFile(data, packet.getSocketAddress(), socket);
	}

	/**
	 * 
	 * @param packet
	 * @author BenjaminP
	 */
	protected void sendFile(ArrayList<byte[]> data, SocketAddress returnAddress, DatagramSocket socket) throws IllegalArgumentException {
		
		for (int i = 1; i <= data.size(); i++) {
			byte sendData[] = data.get(i - 1);
			this.send(createData(i , sendData), socket,  returnAddress);

			DatagramPacket ackPacket = receive(socket);
			if (this.getType(ackPacket) != 4) {
				System.out.println("Not an ACK packet!");
				throw new IllegalArgumentException();
			}

			if (getBlockNum(ackPacket) != i) {
				System.out.println("ACK for a different Block!");
				throw new IllegalArgumentException();
			}
		}
		socket.close();

		/*
		 * List of byte arrays of max size 512 = Call parser here
		 * 
		 */

		/*
		 * LOOP this.send( 1st array, packet.getSocketAddress); Wait for ACK...
		 * this.send( 2nd array, packet.getSocketAddress); Wait for ACK etc...
		 */
	}
	
	protected void receiveFile(DatagramSocket socket, String file){
		 receiveFile(receive(socket), socket, file);
	}
		
	protected void receiveFile(DatagramPacket packet, DatagramSocket socket, String file) {
		ArrayList<byte[]> data = new ArrayList<byte[]>();
		SocketAddress returnAddress = packet.getSocketAddress();
		DatagramPacket newPacket;
		
		if(getType(packet) == OP_DATA) { //if the initial packet is a data packet 
			data.add(packet.getData());
			send(createAck(1), socket, returnAddress);
		}

		do {
			newPacket = receive(socket);
			
			data.add(getByteData(newPacket));
			send(createAck(getBlockNum(newPacket)), socket, returnAddress); // send ack to the client
			
		} while (newPacket.getLength() == STD_DATA_SIZE); // continue while the packets are full
		socket.close();

		try {
			saveFile(data, file);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}	
	}


	/**
	 * Base Send method
	 * 
	 * Sends byte[] msg to the returnAddress
	 * 
	 * @param msg
	 * @param returnAddress
	 * 
	 * @author bloo
	 */
	protected void send(byte[] msg, SocketAddress returnAddress) {
		DatagramSocket socket;
		DatagramPacket sendPacket;
		try {
			socket = new DatagramSocket();

			sendPacket = new DatagramPacket(msg, msg.length, returnAddress);

			if (verbose) {
				System.out.println("Sending:");
				System.out.println(packetToString(sendPacket));
			}

			socket.send(sendPacket);
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
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
		DatagramPacket sendPacket;
		try {

			sendPacket = new DatagramPacket(msg, msg.length, returnAddress);

			if (verbose) {
				System.out.println("Sending: ");
				System.out.println(packetToString(sendPacket));
			}

			socket.send(sendPacket);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
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
		send(socket, sendPacket);
	}

	/**
	 * Base send method, used to send sendPacket over given socket
	 * 
	 * @param socket
	 *            - socket the packet will sent too
	 * @param sendPacket
	 *            - packet to be sent
	 * 
	 * @author bloo
	 */
	protected void send(DatagramSocket socket, DatagramPacket sendPacket) {
		try {
			if (verbose) {
				System.out.println("Sending: ");
				System.out.println(packetToString(sendPacket));
			}

			socket.send(sendPacket);

		} catch (IOException e) {
			socket.close();
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
	DatagramPacket receive(DatagramSocket socket) {
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
	DatagramPacket receive(DatagramSocket socket, int length) {
		DatagramPacket receivedPacket;

		receivedPacket = new DatagramPacket(new byte[length], length);

		try {
			socket.receive(receivedPacket);

			if (verbose) {
				System.out.println("Received:");
				System.out.println(packetToString(receivedPacket));
			}
			return receivedPacket;
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
		dBuff.put(new byte[] { 0, 3, (byte) (error / 256), (byte) (error % 256) });
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
	 * @param fileName
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
	 * Saves fully transfered file to appropriate location
	 * 
	 * @author Eric
	 */
	protected int saveFile(ArrayList<byte[]> data, String fileName) throws IOException {
		OutputStream file = new FileOutputStream(fileName);

		for (int i = 0; i < data.size(); i++) {
			file.write(data.get(i));
		}

		file.close();
		return data.size();
	}

	/**
	 * Parses a tftp packet in byte form and returns info
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
				if (data[1] == 1) {
					descriptor += "RRQ\nFile name: " + getFileName(packet) + "\nMode: " + getMode(packet) + "\n";
				} else if (data[1] == 2) {
					descriptor += "WRQ\nFile name: " + getFileName(packet) + "\nMode: " + getMode(packet) + "\n";
				} else if (data[1] == 3) {
					descriptor += "DATA\nBlock #: " + getBlockNum(packet) + "\nBytes of data: " + getDataLength(packet)
							+ "\n";
				} else if (data[1] == 4) {
					descriptor += "ACK\nBlock #: " + getBlockNum(packet) + "\n";
				} else if (data[1] == 5) {
					descriptor += "ERROR\nError Code" + getBlockNum(packet) + "ErrMsg: " + getError(packet) + "\n";
				}
			}

			return descriptor;
		}

		return null;
	}
}

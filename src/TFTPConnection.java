import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
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

	public static int STD_DATA_SIZE = 516;

	protected static Map<Byte, String> PacketTypes;
	static {
		PacketTypes = new HashMap<>();
		// RequestTypes.put( (byte) 0, "null"); Perhaps use as a shutdown request?
		PacketTypes.put((byte) 1, "RRQ");
		PacketTypes.put((byte) 2, "WRQ");
		PacketTypes.put((byte) 3, "DATA");
		PacketTypes.put((byte) 4, "ACK");
		PacketTypes.put((byte) 5, "ERROR");
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
	 * @param sendSocket
	 *            - socket that will be used to send packet
	 * @param returnAddress
	 *            - address the packet will be send too
	 * 
	 * @author bloo
	 */
	protected void send(byte[] msg, DatagramSocket sendSocket, SocketAddress returnAddress) {
		DatagramPacket sendPacket;
		try {

			sendPacket = new DatagramPacket(msg, msg.length, returnAddress);

			if (verbose) {
				System.out.println("Sending: ");
				System.out.println(packetToString(sendPacket));
			}

			sendSocket.send(sendPacket);

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
	 * Parses a tftp packet in byte form and returns info
	 * 
	 * @param packet
	 * @return contents of a packet
	 * 
	 * @author bloo
	 */
	protected String getFileName(DatagramPacket packet) {
		return bytesToString(readToStop(2, packet.getData(), packet.getLength()));
	}

	protected String getMode(DatagramPacket packet) {
		int offset = readToStop(2, packet.getData(), packet.getLength()).length + 3; // Find offset of mode field by
																						// reading the first field and
																						// adding that fields length,
																						// accounting for the
																						// terminating zero and initial
																						// offset, to the offset
		return bytesToString(readToStop(offset, packet.getData(), packet.getLength()));
	}

	protected String getError(DatagramPacket packet) {
		return bytesToString(readToStop(4, packet.getData(), packet.getLength()));
	}

	protected int getBlockNum(DatagramPacket packet) {
		return Byte.toUnsignedInt(packet.getData()[2]) * 256 + Byte.toUnsignedInt(packet.getData()[3]);
	}

	protected int getType(DatagramPacket packet) {
		return packet.getData()[1];
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
		for (index = offset; index < dataLength && packet[index] != 0; index++) {
			data[index - offset] = packet[index];
		}
		return Arrays.copyOfRange(data, 0, index - offset);
	}

	protected String bytesToString(byte[] data) {
		try {
			return new String(data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new String(data);
		}
	}

	protected int getDataLength(DatagramPacket packet) {
		return readToStop(4, packet.getData(), packet.getLength()).length;
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
	public ArrayList<byte[]> readFile(String fileName) throws IOException {
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
	public int saveFile(ArrayList<byte[]> data, String fileName) throws IOException {
		OutputStream file = new FileOutputStream(fileName);
		System.out.println("Writing to file");
		// for(byte b: data.get(0))
		// System.out.println(b +", ");

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

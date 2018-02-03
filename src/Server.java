import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author BenjaminP, BenB
 *
 */
public class Server {
	/*
	 * File transferred in 512-byte blocks, 1 block per packet transfer. Block < 512 bytes terminates transfer
	 * Packet types: RRQ, WRQ, DATA, ACK, ERROR
	 * TID(transfer ID)
	 * Client gets random TID when a request is prepared, when a server grants a request it also gets a random TID
	 * Source and destination TID associated with every packet but not stored in the packet, used as source/destination ports for UDP
	 * Write: Client WRQ -> Server ACK Block 0 -> Client Data Block 1 -> Server ACK Block 1 -> Client Data Block 2 -> etc... Server ACK Block n
	 * Read: Client RRQ -> Server Data Block 1 -> Client ACK Block 1 -> Server Data Block 2 -> etc... Client ACK Block n
	 * RRQ acknowledged with DATA, WRQ by ACK
	 * */
	private DatagramSocket serverReceiveSocket;
	
	private DatagramPacket sendPacket, receivePacket;

	private static Map<Byte, String> RequestTypes;
	static {
		RequestTypes = new HashMap<>();
		//RequestTypes.put( (byte) 0, "null"); Perhaps use as a shutdown request?
		RequestTypes.put( (byte) 1, "RRQ");
		RequestTypes.put( (byte) 2, "WRQ");
		RequestTypes.put( (byte) 3, "DATA");
		RequestTypes.put( (byte) 4, "ACK");
		RequestTypes.put( (byte) 5, "ERROR");
	}
	
	private final byte[] readResponce = { 0, 3, 0, 1 };
	private final byte[] writeResponce = { 0, 4, 0, 0 };

	/**
	 * Constructor for a Server
	 * 
	 * @param serverPort
	 */
	Server(int serverPort) {
		try {
			serverReceiveSocket = new DatagramSocket(69);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Default receive method
	 * 
	 * receives a packet over the servers socket
	 * 
	 * @return
	 */
	DatagramPacket receive() {
		return receive(serverReceiveSocket);
	}

	/**
	 * Base receive method
	 * 
	 * Receives a DatagramPacket over the given socket
	 * 
	 * @param socket
	 * @return receivePacket unless there is an exception trying to receive
	 */
	DatagramPacket receive(DatagramSocket socket) {
		byte[] msg;
		receivePacket = new DatagramPacket(new byte[100], 100);

		try {
			socket.receive(receivePacket);
			msg = receivePacket.getData();

			System.out.println("Server Received:");
			System.out.println(new String(msg));
			System.out.println(Arrays.toString(msg) + "\n");

			return receivePacket;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return new DatagramPacket(new byte[0], 0);
	}

	/**
	 * Base Send method
	 * 
	 * Sends byte[] msg to the returnAddress
	 * 
	 * @param msg
	 * @param returnAddress
	 */
	void send(byte[] msg, SocketAddress returnAddress) {
		DatagramSocket socket;
		try {
			socket = new DatagramSocket();

			sendPacket = new DatagramPacket(msg, msg.length, returnAddress);

			System.out.println("Server Sending:");
			System.out.println(new String(sendPacket.getData()));
			System.out.println(Arrays.toString(sendPacket.getData()) + "\n");

			socket.send(sendPacket);
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * methods the manages packet being received
	 * 
	 * sends any received packets to be processed by another method
	 */
	void waitForRequest() {
		while (true) {
			try {
				processPacket(receive());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	/**
	 * Process a received packet and send a response if necessary
	 * 
	 * @param receivedPacket
	 * @throws IllegalArgumentException
	 */
	void processPacket(DatagramPacket receivedPacket) throws IllegalArgumentException {
		int numZeroes;
		byte[] msg = receivedPacket.getData();

		msg = Arrays.copyOf(msg, receivedPacket.getLength()); // extract data

		// Verify validity of the data

		if (msg[0] == 0 & msg[msg.length - 1] == 0) { // check the static zeroes bytes at start and end of the array

			numZeroes = 0;
			for (int i = 2; i < msg.length - 1; i++) { // look for a zero in between to strings
				if (msg[i] == 0) {
					numZeroes++;
				}
			}
			if (numZeroes == 1) { // if one zero is found
				if (msg[1] == 1) { // send either read or write response
					send(readResponce, receivedPacket.getSocketAddress());
				} else if (msg[1] == 2) {
					send(writeResponce, receivedPacket.getSocketAddress());
				} else {
					throw new IllegalArgumentException(); // If there is any invalidity with the data send
															// IllegalArgumentException
				}
			} else {
				throw new IllegalArgumentException();
			}
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * 
	 * Pulls request type from packet and returns
	 * 
	 * @param packet
	 * @return	Returns request type
	 * @throws IllegalArgumentException	When data is not in proper format or request is not a recognized type
	 */
	private byte getRequest(DatagramPacket packet) throws IllegalArgumentException
	{
		byte data[] = packet.getData();
		
		if(data[0] == 0 &&  data[data.length -1] == 0)
		{
			byte request = data[1];
			if( RequestTypes.containsKey(request) )
			{
				return request;
			}
			else
				throw new IllegalArgumentException();
		}
		else
			throw new IllegalArgumentException();
	}
	
	/**
	 * 
	 * Checks for last data block
	 * 
	 * @param packet	Packet containing data block
	 * @return	Boolean, returns false when the last packet has been received ie. 0 <= data size < 512
	 */
	private boolean blockReceive(DatagramPacket packet)
	{
		byte data[] = packet.getData();
		/* This checks for a data block */
		if(data.length == 512)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Start the sever waiting for request
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Server s = new Server(69);
		s.waitForRequest();
	}
}
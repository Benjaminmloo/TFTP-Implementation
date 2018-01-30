import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author BLoo
 *
 */
public class Client {
	private DatagramSocket clientSocket;
	private DatagramPacket sendPacket, receivePacket;

	final static private String file = "Test.txt";
	final static private String mode = "netascii";

	byte read[], write[];

	/**
	 * default constructor for the client
	 */
	Client() {

		try {
			clientSocket = new DatagramSocket();

		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Default receive method for the client
	 * 
	 * receives response on the port that the client initially sent to 
	 */
	void receive() {
		byte[] msg;
		receivePacket = new DatagramPacket(new byte[100], 100);

		try {
			clientSocket.receive(receivePacket);
			msg = receivePacket.getData();

			System.out.println("Client Received:");
			System.out.println(new String(msg));
			System.out.println(Arrays.toString(msg) + "\n");

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * default send method for the client
	 * 
	 * sends the msg to port 23 on the local host
	 * 
	 * @param msg
	 */
	void send(byte[] msg) {
		try {
			sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), 23);

			System.out.println("Client Sending:");
			System.out.println(new String(sendPacket.getData()));
			System.out.println(Arrays.toString(sendPacket.getData()) + "\n");

			clientSocket.send(sendPacket);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Sends a volley of packets to the server and receives the responses
	 */
	void packetVolley() {
		int packetSize = 4 + file.length() + mode.length();

		read = new byte[packetSize];
		write = new byte[packetSize];

		ByteBuffer dataBuffer = ByteBuffer.wrap(read); //a byte buffer is created to easily fill the byte array, read

		dataBuffer.put(new byte[] { 0, 1 }); //set up the contents of the read data
		dataBuffer.put(file.getBytes());
		dataBuffer.put((byte) 0);
		dataBuffer.put(mode.getBytes());
		dataBuffer.put((byte) 0);

		write = Arrays.copyOf(read, read.length); //put a copy of the read data to initialize what should be in write date 
		write[1] = 2;	//set the flag byte in the start of the data to indicate a write

		for (int i = 0; i < 10; i++) { //Send 10 packets alternating between read and writes
			if (i % 2 == 0) {
				send(read);
			} else {
				send(write);
			}
			receive();
		}

		send(new byte[] { 1, 1, 1, 1, 1 }); //send an invalid packet
		receive();
		clientSocket.close();
	}

	/**
	 * creates a client and starts a volley of packets
	 * @param args
	 */
	public static void main(String[] args) {
		Client c = new Client();
		c.packetVolley();
	}
}

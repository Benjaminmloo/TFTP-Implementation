import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class UDPConnection {
	protected static Map<Byte, String> RequestTypes;
	static {
		RequestTypes = new HashMap<>();
		// RequestTypes.put( (byte) 0, "null"); Perhaps use as a shutdown request?
		RequestTypes.put((byte) 1, "RRQ");
		RequestTypes.put((byte) 2, "WRQ");
		RequestTypes.put((byte) 3, "DATA");
		RequestTypes.put((byte) 4, "ACK");
		RequestTypes.put((byte) 5, "ERROR");
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
		DatagramPacket sendPacket;
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

	void send(byte[] msg, DatagramSocket sendSocket, SocketAddress returnAddress) {
		DatagramPacket sendPacket;
		try {

			sendPacket = new DatagramPacket(msg, msg.length, returnAddress);

			System.out.println("Server Sending:");
			System.out.println(new String(sendPacket.getData()));
			System.out.println(Arrays.toString(sendPacket.getData()) + "\n");

			sendSocket.send(sendPacket);

		} catch (IOException e) {
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
	 * @return receivePacket unless there is an exception trying to receive
	 */
	DatagramPacket receive(DatagramSocket socket) {
		return receive(socket, 516);
	}

	/**
	 * Base receive method
	 * 
	 * Receives a DatagramPacket over the given socket
	 * 
	 * @param socket
	 * @return receivePacket unless there is an exception trying to receive
	 */
	DatagramPacket receive(DatagramSocket socket, int length) {
		DatagramPacket receivedPacket;

		byte[] msg;
		receivedPacket = new DatagramPacket(new byte[length], length);

		try {
			socket.receive(receivedPacket);
			msg = receivedPacket.getData();

			System.out.println("Server Received:");
			System.out.println(new String(msg));
			System.out.println(Arrays.toString(msg) + "\n");

			return receivedPacket;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return new DatagramPacket(new byte[0], 0);
	}
}

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author BenjaminP, BenB
 *
 */
public class Server {
	/*
	 * File transferred in 512-byte blocks, 1 block per packet transfer. Block < 512
	 * bytes terminates transfer Packet types: RRQ, WRQ, DATA, ACK, ERROR
	 * TID(transfer ID) Client gets random TID when a request is prepared, when a
	 * server grants a request it also gets a random TID Source and destination TID
	 * associated with every packet but not stored in the packet, used as
	 * source/destination ports for UDP Write: Client WRQ -> Server ACK Block 0 ->
	 * Client Data Block 1 -> Server ACK Block 1 -> Client Data Block 2 -> etc...
	 * Server ACK Block n Read: Client RRQ -> Server Data Block 1 -> Client ACK
	 * Block 1 -> Server Data Block 2 -> etc... Client ACK Block n RRQ acknowledged
	 * with DATA, WRQ by ACK
	 */
	private DatagramSocket serverReceiveSocket;

	private DatagramPacket sendPacket, receivePacket;
	
	private boolean verbose;

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
	Server(int serverPort, boolean verbose) {
		this.verbose = verbose;
		try {
			serverReceiveSocket = new DatagramSocket(69);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	Server(int serverPort) {
		this(serverPort, true);
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

	private class RequestReceiver extends Thread {
		DatagramSocket requestSocket;

		RequestReceiver(DatagramSocket requestSocket) {
			this.requestSocket = requestSocket;
		}

		@Override
		public void run() {
			DatagramPacket receivedPacket;
			while (true) {
				receivedPacket = receive(requestSocket); // wait for new request packet
				new ClientConnection(receivedPacket).start(); // start new client connection for the recently acquired
																// request
			}
		}

	}

	private class ClientConnection extends Thread {
		SocketAddress returnAddress;

		ClientConnection(DatagramPacket requestPacket) {
			returnAddress = requestPacket.getSocketAddress(); // set the return address of the packet
		}

		@Override
		public void run() {// TODO parse request then transfer files.
			while (true) {

			}
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
	DatagramPacket receive(DatagramSocket socket, int length) {
		byte[] msg;
		receivePacket = new DatagramPacket(new byte[length], length);

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
	
	void send(byte[] msg, DatagramSocket sendSocket, SocketAddress returnAddress) {
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
	 * methods the manages packet being received
	 * 
	 * sends any received packets to be processed by another method
	 * @param threaded flag to determine whether or not to use threads to wait for request
	 */
	void waitForRequest(boolean threaded) {
		if (threaded) { 
			new RequestReceiver(serverReceiveSocket).start();
		} else {
			while (true) {
				try {
					processPacket(receive());
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					System.exit(1);
				}
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
	 * @author BenjaminP
	 * Handles packet requests
	 * 
	 * @param packet
	 */
	private void requestHandler(DatagramPacket packet)
	{
		byte request = this.getRequest(packet);
		switch (request){
			
			/* Read Request */
			case 1: 
				// Respond with Data block 1 and 0 bytes of data
				byte data[] = { 0, 3, 0, 1};
				this.send(data, packet.getSocketAddress() );
				break;
				
			/* Write Request */
			case 2:
				// Respond with ACK block 0
				byte data1[] = { 0, 4, 0, 0};
				this.send(data1, packet.getSocketAddress() );
				break;
				
			/* Data */
			case 3:
				System.out.println("Received out of time DATA packet");
				break;
				
			/* Acknowledge */
			case 4:
				System.out.println("Received out of time ACK packet");
				break;
				
			/* Error */
			case 5:
				/* Currently does nothing */
				break;
		}
			
	}
	
	/**
	 * @author BenjaminP
	 * @param packet
	 */
	private void readRequestHandler(DatagramPacket packet) throws IllegalArgumentException
	{
		List<byte[]> data = this.parseRRQ(this.getFileName(packet));
		
		for(int i = 0; i < data.size(); i++)
		{
			byte sendData[] = data.get(i);
			this.send( sendData, packet.getSocketAddress() );
			
			DatagramPacket ackPacket = this.receive();
			if(this.getRequest(ackPacket) != (byte) 4)
			{
				System.out.println("Not an ACK packet!");
				throw new IllegalArgumentException();
			}
			
			if( processACK(ackPacket.getData()) != i)
			{
				System.out.println("ACK for a different Block!");
				throw new IllegalArgumentException();
			}
		}
		/* 
		 * List of byte arrays of max size 512 = Call parser here
		 * 
		 *  */
		
		/* 
		 * LOOP
		 * this.send( 1st array, packet.getSocketAddress);
		 * Wait for ACK...
		 * this.send( 2nd array, packet.getSocketAddress);
		 * Wait for ACK etc...
		 */
	}
	
	private int processACK(byte[] data)
	{
		return ((data[2] * 10) + data[3]);
	}
	
	/**
	 * @author BenjaminP
	 * Parses given file to be sent through data packets
	 * 
	 * @param file
	 * @return
	 */
	private List<byte[]> parseRRQ(String file)
	{
		String data = "";
		try {
			FileReader fileReader = new FileReader(file);
			
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String line = null;
			while(	(line = bufferedReader.readLine()) != null )
			{
				data = data.concat(" " + line);
			}
			
			bufferedReader.close();
			
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		List<byte[]> outList = new ArrayList<byte[]>();
		byte byteData[] = data.getBytes();
		for(int i = 0; i < byteData.length; i += 512)
		{
			byte temp[] = null;
			if(i + 512 <= byteData.length)
			{
				temp = Arrays.copyOfRange(byteData, i, i + 512);
				outList.add(temp);
			}
			else
			{
				temp = Arrays.copyOfRange(byteData, i, byteData.length);
				outList.add(temp);
			}
		}
		
		return outList;
	}
	
	/**
	 * 
	 * @param packet
	 */
	private void writeRequestHandler(DatagramPacket packet)
	{
		int blockNum = 0;
		
		SocketAddress returnAddress = packet.getSocketAddress();
		DatagramSocket rrqSocket = null;
		DatagramPacket newPacket;
		OutputStream file = null;
		try {
			file = new FileOutputStream(getFileName(packet));
			rrqSocket = new DatagramSocket();
			send(createAck(blockNum), rrqSocket, returnAddress);
			do{
				newPacket = receive(rrqSocket, 516);
				file.write(newPacket.getData(), 4, newPacket.getLength()); //write the data section of the packet to the file
				send(createAck(++blockNum), rrqSocket, returnAddress); //send ack to the client
			}while(newPacket.getLength() == 516); //continue while the packets are full
			file.close(); //close the file when done
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (SocketException e) {
			if(rrqSocket != null) {
				rrqSocket.close();
			}
			try {
				file.close();
			} catch (IOException x) {
				x.printStackTrace();
			}
			e.printStackTrace();
			System.exit(1);
		}catch (IOException e) {
			if(rrqSocket != null) {
				rrqSocket.close();
			}
			e.printStackTrace();
			System.exit(1);
		}
		/* 
		 * LOOP
		 * Send ACK block 0
		 * Wait for next Data
		 * file.add(receivedData);
		 * Send ACK block 1
		 * Wait for next Data 
		 * etc...
		 */
	}
	}
	
	private byte[] createAck(int blockNum) {
		byte[] ack = new byte[] { 0, 4, (byte)(blockNum / 256), (byte)(blockNum % 256)};
		return ack;
		
		
		return null;
		
	private void createAck(int blockNum) {
		
	}
	
	private String readBytes(int index, byte[] packet, int dataLength) {
		String data = "";
		while (index < dataLength - index && packet[index] != 0) {
			data += (char) packet[index++];
		}
		return data;
	}
	
	private String getFileName(DatagramPacket packet) {
		return readBytes(2, packet.getData(), packet.getLength());
	}

	/**
	 * Start the sever waiting for request
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Server s = new Server(69);
		s.waitForRequest(false);
	}
}
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
public class ThreadedConnection extends TFTPConnection implements Runnable{
	
	private DatagramPacket requestPacket;
	
	public ThreadedConnection(DatagramPacket p)
	{
		this.verbose = true;
		requestPacket = p;
	}
	
	/**
	 * 
	 * Pulls request type from packet and returns
	 * 
	 * @param packet
	 * @return Returns request type
	 * @throws IllegalArgumentException
	 *             When data is not in proper format or request is not a recognized
	 *             type
	 */
	private byte getRequest(DatagramPacket packet) throws IllegalArgumentException {
		byte data[] = packet.getData();

		if (data[0] == 0 && data[data.length - 1] == 0) {
			byte request = data[1];
			if (PacketTypes.containsKey(request)) {
				return request;
			} else
				throw new IllegalArgumentException();
		} else
			throw new IllegalArgumentException();
	}
	
	/**
	 * @author BenjaminP 
	 * Handles packet requests
	 * 
	 * @param packet
	 */
	private void requestHandler(DatagramPacket packet) {
		byte request = this.getRequest(packet);
		switch (request) {

		/* Read Request */
		case 1:
			// Respond with Data block 1 and 0 bytes of data

			//if(verbose)System.out.println("Handleing rrq");
			readRequestHandler(packet);
			break;

		/* Write Request */
		case 2:
			// Respond with ACK block 0
			//if(verbose)System.out.println("Handleing wrq");
			writeRequestHandler(packet);
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
	private void readRequestHandler(DatagramPacket packet) throws IllegalArgumentException {
		ArrayList<byte[]> data;
		DatagramSocket rrqSocket;
		
		try {
			data = readFile(getFileName(packet));
			try {
				rrqSocket = new DatagramSocket();
				
				for (int i = 1; i <= data.size(); i++) {
					byte sendData[] = data.get(i - 1);
					this.send(createData(i , sendData), rrqSocket,  packet.getSocketAddress());

					DatagramPacket ackPacket = receive(rrqSocket);
					if (this.getType(ackPacket) != 4) {
						System.out.println("Not an ACK packet!");
						throw new IllegalArgumentException();
					}

					if (getBlockNum(ackPacket) != i) {
						System.out.println("ACK for a different Block!");
						throw new IllegalArgumentException();
					}
				}
				rrqSocket.close();
			} catch (SocketException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		/*
		 * List of byte arrays of max size 512 = Call parser here
		 * 
		 */

		/*
		 * LOOP this.send( 1st array, packet.getSocketAddress); Wait for ACK...
		 * this.send( 2nd array, packet.getSocketAddress); Wait for ACK etc...
		 */
	}
	
	/**
	 * handles tftp write requests
	 * receives data to be written from client and write it too a file
	 * @param packet
	 * @author bloo
	 */
	private void writeRequestHandler(DatagramPacket packet) {
		ArrayList<byte[]> file = new ArrayList<byte[]>();
		SocketAddress returnAddress = packet.getSocketAddress();
		DatagramSocket wrqSocket = null;
		DatagramPacket newPacket;
		int blockNum = 0;
		try {
			wrqSocket = new DatagramSocket();
			send(createAck(blockNum), wrqSocket, returnAddress);
			
			do {
				newPacket = receive(wrqSocket);
				file.add(getByteData(newPacket)); // write the data section of the packet to
				// the file
				blockNum++;
				send(createAck(getBlockNum(newPacket)), wrqSocket, returnAddress); // send ack to the client
			} while (newPacket.getLength() == STD_DATA_SIZE); // continue while the packets are full
			wrqSocket.close();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		try {
			saveFile(file, getFileName(packet));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	@Override
	public void run() {
		if(verbose)System.out.println("starting new connection");
		requestHandler(requestPacket);
	}
}

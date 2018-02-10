import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
	 *             - When data is not in proper format or request is not a
	 *             recognized type
	 * @author BenjaminP
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
	 * Decides how to handle any request
	 * 
	 * Handles packet requests
	 * 
	 * @param packet
	 * @author BenjaminP 
	 */
	private void requestHandler(DatagramPacket packet) {
		byte request = this.getRequest(packet);
		DatagramSocket handlerSocket = waitForSocket();
		try {
		switch (request) {
		/* Read Request */
		case 1:
			//if(verbose)System.out.println("Handleing rrq");
			sendFile(packet, handlerSocket);
			break;

		/* Write Request */
		case 2:
			// Respond with ACK block 0
			//if(verbose)System.out.println("Handleing wrq");
			send(createAck(0), handlerSocket, packet.getSocketAddress());
			receiveFile(packet, handlerSocket, getFileName(packet));
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
		}catch(IOException e) {
			//handle file IO error
			e.printStackTrace();
			System.exit(1);
		}

	}
	
	/** 
	 * Starts off a new connection
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 *
	 */
	@Override
	public void run() {
		if(verbose)System.out.println("starting new connection");
		requestHandler(requestPacket);
	}
}

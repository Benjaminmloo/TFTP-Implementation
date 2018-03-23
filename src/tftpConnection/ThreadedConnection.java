package tftpConnection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import javax.swing.JTextArea;

public class ThreadedConnection extends TFTPConnection implements Runnable {

    private DatagramPacket requestPacket;

    public ThreadedConnection(DatagramPacket p, JTextArea outputWindow) {
	this(p, true, outputWindow);
    }

    public ThreadedConnection(DatagramPacket p, boolean verbose, JTextArea outputWindow) {
	this.outputWindow = outputWindow;
	this.verbose = verbose;
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
	    if (TFTPPacket.PacketTypes.containsKey(request)) {
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
	String fileName = TFTPPacket.getFileName(packet);
	try {
	    switch (request) {
	    /* Read Request */
	    case 1:
		if (verbose)
		    println("Handleing rrq");

		if (!Files.exists(Paths.get(fileName)))
		    throw new FileNotFoundException("File \"" + fileName + "\" not found"); // verify existence of file
											    // before operation
		sendFile(packet, handlerSocket);
		break;

	    /* Write Request */
	    case 2:
		// Respond with ACK block 0
		if (verbose)
		    println("Handleing wrq");
		if (Files.exists(Paths.get(fileName)))
		    throw new FileAlreadyExistsException("File \"" + fileName + "\" Already Exists"); // verify
												      // existence of
												      // file before
												      // operation
		send(TFTPPacket.createAck(0), handlerSocket, packet.getSocketAddress());
		receiveFile(packet, handlerSocket, fileName);
		break;

	    /* Data */
	    case 3:
		println("Received out of time DATA packet");
		break;

	    /* Acknowledge */
	    case 4:
		println("Received out of time ACK packet");
		break;

	    /* Error */
	    case 5:
		/* Currently does nothing */
		break;
	    }
	} catch (FileNotFoundException | NoSuchFileException | InvalidPathException e) {
	    send(TFTPPacket.createError(1, e.getMessage().getBytes()), handlerSocket, packet.getSocketAddress());
	} catch (AccessDeniedException e) {
	    send(TFTPPacket.createError(2, e.getMessage().getBytes()), handlerSocket, packet.getSocketAddress());
	} catch (FullFileSystemException e) {
	    send(TFTPPacket.createError(3, e.getMessage().getBytes()), handlerSocket, packet.getSocketAddress());
	} catch (FileAlreadyExistsException e) {
	    send(TFTPPacket.createError(6, e.getMessage().getBytes()), handlerSocket, packet.getSocketAddress());
	} catch (IOException e) {

	    e.printStackTrace();
	    System.exit(1);
	}

	handlerSocket.close();
    }

    /**
     * Starts off a new connection (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     *
     */
    @Override
    public void run() {
	if (verbose)
	    println("starting new connection");
	requestHandler(requestPacket);
    }

    @Override
    public void takeInput(String s) {
	// TODO Auto-generated method stub

    }
}

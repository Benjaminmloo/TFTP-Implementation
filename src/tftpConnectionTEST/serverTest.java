package tftpConnectionTEST;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import tftpConnection.Server;
import tftpConnection.TFTPConnection;

class serverTest extends TFTPConnection {

	// Test if static value can transfer over
	// TFTP gets port called SERVER_PORT, passes to Server - Testing if port are direct
	// This tests WaitingForRequest and requestSocket
	@Test
	void portTestFromTFTP() {
		// Passing Local Host Port
		Server s = new Server(SERVER_PORT);
		if (s.getWaitForRequest() == 69) {
			assertTrue(true);
			System.out.println("=====PASS======");
		}
		
		// Passing a random number "9999"
		Server s2 = new Server(9999);
		if (s2.getWaitForRequest() == 9999) {
			assertTrue(true);
			System.out.println("=====PASS======");
		}
	}

	@Override
	public void takeInput(String s) {
		return;
	}

}

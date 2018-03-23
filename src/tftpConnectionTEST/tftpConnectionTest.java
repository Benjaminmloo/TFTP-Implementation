package tftpConnectionTEST;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.io.*;

import tftpConnection.*;

public class tftpConnectionTest extends TFTPConnection {

	@Test
	public void formRQ(){
//		try {
//			String fileName = "helloworld.txt";
//			String mode = "octet";
//			 
//		} catch(UnknownHostException e) {
//			e.printStackTrace();
//		}		
//		byte opCode = 2;
//		byte[] file = new byte[2];
//		byte[] mode = new byte[2];
//		
//		
//		Client c = new Client();
//		c.createRQ(opCode, file, mode);
		
		tftpConnectionTest t = new tftpConnectionTest();

	}

	@Override
	public void takeInput(String s) {
		// TODO Auto-generated method stub
		
	}

}

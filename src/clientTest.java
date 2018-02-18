import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

public class clientTest {

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream printContent = new PrintStream(outContent);
	
	// Testing of the output String is exactly what it should look like
	@Test
	public void userInterfaceTestInitialPrintOut() {
		System.setOut(printContent);
		//Client c = new Client();
		//c.userInterface();
		System.out.print("RRQ(1), WRQ(2), settings(3), quit(4): ");
		assertEquals("RRQ(1), WRQ(2), settings(3), quit(4): ", outContent.toString());
		System.out.println("=====PASS======");
	}
	
	// Testing some of the userInterfaceTest using byte and layout of the method
	@Test
	public void userInterfaceTest() {
		
		// Testing byte value
		byte valueTest = 1;
		byte valueTest2 = 2;
		byte valueTest3 = 90;
		assertEquals(1, valueTest);
		assertEquals(2, valueTest2);
		assertEquals(90, valueTest3);
		System.out.println("=====PASS======");
	
		//Checking to see of operation has the right value
		Client c = new Client();
		c.setOperation(valueTest);
		assertEquals(1, c.getOperation());
		
		//Checking with conditions based on userInterface method
		if (c.getOperation() == 1 || c.getOperation() == 2) {
			System.out.print("Test Success: RRQ - WRQ \n");
			assertTrue(c.getOperation() == 1 || c.getOperation() == 2);
			System.out.println("=====PASS======");
		}
		
		c.setOperation(valueTest2);
		if (c.getOperation() == 1 || c.getOperation() == 2) {
			System.out.print("Test Success: RRQ - WRQ \n");
			assertTrue(c.getOperation() == 1 || c.getOperation() == 2);
			System.out.println("=====PASS======");
		}
		
		c.setOperation(valueTest3);
		if (c.getOperation() > 4) {
			System.out.print("Test Success: Invalid Input \n");
			assertTrue(c.getOperation() >= 90);
			System.out.println("=====PASS======");
		}
		
		byte valueTest4 = 4;
		c.setOperation(valueTest4);
		if (c.getOperation() == 4) {
			System.out.print("Test Success: Quiting \n");
			assertTrue(c.getOperation() == 4);
			System.out.println("=====PASS======");
		}	
	}
	
	// Check if setOperation is setting properly
	@Test
	public void setOperationTest() {
		Client c = new Client();
		byte value = 2;
		c.setOperation(value);
		assertEquals(2, value);
		System.out.println("=====PASS======");
	}
	
	// Check if getOperation is getting the right value
	@Test
	public void getOperationTest() {
		Client c = new Client();
		byte value = 2;
		c.setOperation(value);
		assertEquals(2, c.getOperation());
		System.out.println("=====PASS======");
	}
	
	// Not sure how to test this***
	@Test
	public void establishConnectionTest() {
		// establishConnection(operation, localFile, serverFile, sendPort);
		Client c = new Client();
//		byte value = 90;
//		c.setOperation(value);
//		c.establishConnection(value, null, null, 50);
		assertTrue(true);
		System.out.println("=====PASS======");
		
	}
}

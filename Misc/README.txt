/** READ ME FILE
*
* SYSC 3303 Group Project
* Project: Design and Implementation of file transfer system based on the TFTP
* System consist of TFTP client - error - server.
*
* @author: Loo Ben, Nguyen Andrew, Morrissette Eric, Palko Ben
* 
*/

FILES:
========================================

Source Files:
src/tftpConnection/Client.java
src/tftpConnection/ErrorSimulator.java
src/tftpConnection/Server.java
src/tftpConnection/TFTPConnection.java
src/tftpConnection/ThreadedConnection.java
src/tftpConnection/UserInterface.java

JUnit Test Files;
src/tftpConnectionTEST/clientTest.java
src/tftpConnectionTEST/errorSimulatorTest.java
src/tftpConnectionTEST/serverTest.java
src/tftpConnectionTEST/tftpConnectionTest.java



STEP INSTRUCTION:
========================================

Running the application:
1. Run the UserInterface class (You don't have to run the classes separately any more).
	- A new window should open up
	- It'll ask for input commands

2. Select one of the four input commands (RRQ(1), WRQ(2), settings(3), quit(4)).
	- If (1) or (2) is selected (continue on to "Step 3").
	- If (3) is selected (jump to "Step 6").
	- If (4) is selected, the application is done ("Jump to Step 9").

3. Input file to Read/Write on client (eg: "src\\test.txt").
	- Press Enter.
	
4. Input file destination on server (eg: "Misc\\test.txt").
	- Press Enter.
	- Packet should be transferring back and fourth between client and server.
	- Information will be shown on each class tab in User Interface.
	- If it's Read, ACK will be sent and server will send DATA back.
	- If it's Write. Data will be sent and server will send ACK back.
	
5. Once it is done, you can start again (Go back to "Step 2").

6. Setting(3) - User Interface will ask for an input for Verbose (eg: "true/false").
	- If (true), User Interface will show and trace the packets being sent back and fourth.
	- If (false), file transfer will be hidden.
	
7. User Interface will ask for an input for Testing (eg: "true/false").
	- If (true), the packet will transmit with ErrorSimulator.
	- If (false), the packet will ignore ErrorSimulator. Client will send and receive directly to/from Server.
	
8. User Interface will jump back to the main four input commands (Go back to "Step 2").

9. Close the User Interface Window.

END


Running the Test:
1. First check if computer is running JUnit 4 or JUnit 5.

2. Right click tftpConnectionTEST > Run As > JUnit Test.

3. Testing was done manually.



UPDATE NOTES:
========================================
*************************************************************************************************
March 10th, 2018
ITERATION 3

Setup Instruction
========================================
To start the program run through the UserInterface class, input is tied to your current tab (Client tab will send client input etc.)

Testing Classes - Make it runs independently( Without the Client-Server running since it makes its own Mock Server)

Class files: Client, ErrorSimulator, Server, ThreadedConnection, TFTPConnection, User Interface
clientTest, errorSimulator, serverTest, tftpConnectionTest; .java

Class Updates 
========================================
- Adding in Network Error Check. (Timeout, Duplicates, Acknowledgement0 Check, Retransmit)
- Refactor and simplified the classes for better understanding
- More Testing!!!

Diagrams
========================================
- Updated UCM, UML, Timing Diagram (Different Scenario)



*************************************************************************************************
*************************************************************************************************
Feb 17th, 2018
ITERATION 2

Setup Instruction
========================================
To start the program run through the UserInterface class, input is tied to your current tab (Client tab will send client input etc.)

Testing Classes - Make it runs independently( Without the Client-Server running since it makes its own Mock Server)

Class files: Client, ErrorSimulator, Server, ThreadedConnection, TFTPConnection, User Interface
clientTest, errorSimulator, serverTest, tftpConnectionTest; .java

Class Updates 
========================================
- Added all the Test Classes
- Checks some methods and connections if ports are passing correctly
- Some confusion with protocol testing
- Added some methods to mock test: Client, Server


UserInterface
========================================
- GUI
- Have 3 classes, each having it's own tab. Switching between them will also switch the input destination to the respective tabs class



*************************************************************************************************
*************************************************************************************************
Feb 3rd, 2018
ITERATION 1 w/ ITERATION 0

Setup Instruction
========================================
Run Server > ErrorSimulator > Client
In this order above.


Class files: Client, ErrorSimulator, Server, ThreadedConnection, UDPConnection; .java

Client Class
========================================
Iteration 0
- WRQ Connections
- RRQ Connections
- Sending packet to server with no steady state file transfer
- Establish both RRQ and WRQ
- Shutdown implementation
Iteration 1
- Support Steady State file transfer



ErrorSimulator Class
========================================
Iteration 0
- Pass on Packets (Client to server, and server to client; Similar to ImmediateHost)
- No Error Simulation yet
Iteration 1
- Support Steady State file transfer
- No Errors occur (No TFTP ERROR)
- No duplicates, no delay, lost in transit yet implemented (TFTP timeout not supported)
- Just passing on packets for now


Server Class
========================================
Iteration 0
- Multiple concurrent connections using multi threads
- Server responds with DATA block 1 and 0 for RRQ
- Server responds with ACK block 0 for WRQ
- New created client connections thread terminates after acknowledged packet send.
- Shutdown implementation
Iteration 1
- Support Steady State file transfer


ThreadedConnection Class
========================================
Iteration 1
- Checks for proper request
- Handle Packet Request
- Parses file through Data Packet
- Process ACK
- Read and Parse byte forms


UDPConnection Class
=========================================
- Check for Request Type; RRQ, WRQ, DATA, ACK, ERROR
- Sends Packet to Port Location
- Receive Packet from Port Location
- Packet to String

Contribution
========================================
Loo Ben: Error Simulator, Server, UDPConnection, TFTPConnection
Nguyen Andrew: UML, UCM, READ_ME, Testing Classes, Client
Morrissette Eric: Client, Trello
Palko Ben: Server, Threaded Connection, Trello, UserInterface

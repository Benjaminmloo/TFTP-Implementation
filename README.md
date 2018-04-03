README FILE

(Last Update: March 24th, 2018)

Personal Note: Ctrl+F (,) 
Anything with (,) may be changed or updated in the future iterations
Delete (,) on the final submission
*************************************************************************************************
*************************************************************************************************


TABLE OF CONTENT
======================================== 
1. PROJECT OBJECTIVE
2. AUTHOR
3. CONTRIBUTION
4. FILES
5. MISC FILES
6. EXECUTION STEP INSTRUCTIONS
7. UPDATE NOTES (Iterations 1-5)


1. PROJECT OBJECTIVE
======================================== 
Design and Implementation of file transfer system based on the TFTP - RFC 1350
System will consist of TFTP client(s) running on multiplie computers act as multithread for a server.
There will be an error simulator that'll be handling errors between the transmission.


2. AUTHOR
========================================
Loo Ben, Nguyen Andrew, Morrissette Eric, Palko Ben


3. CONTRIBUTION (,)
========================================
Loo Ben: Client*, ErrorSimulator, Server*, TFTPConnection*, TFTPPacket, ThreadedConnection
Nguyen Andrew: FinalProject Doc*, Diagrams*, READ_ME.txt*, Client, TFTPConnection, UserInterface
Morrissette Eric: Client*, ErrorSimulator*, TFTPConnection, TFTPPacket, ThreadedConnection, Trello* 
Palko Ben: Client, Server, ThreadedConnection*, TFTPConnection, UserInterface*, Trello

Anything with "*" is the main focus of the individual's contribution
Everyone did abit of everything: We all did our best to contribute what we can.

4. FILES (,)
========================================
Source Files:
src/tftpConnection/Client.java
src/tftpConnection/ErrorSimulator.java
src/tftpConnection/Server.java
src/tftpConnection/TFTPConnection.java
src/tftpConnection/TFTPPacket.java
src/tftpConnection/ThreadedConnection.java
src/tftpConnection/UserInterface.java


5. MISC FILES (,)
========================================
SYSC_3310_Final_Project.docx
project.pdf
Test_Table.docx
Timing_Diagram_Error_1_2_3_6.docx
UCM_TFTP.docx
UML_TFTP.png
test.txt


6. EXECUTION STEP INSTRUCTION (,)
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
    		7.1: Instructions will prompt and ask for what error handling the user would like to see.
    			 Please follow the instruction that is shown in the interface.
    			 (example step:
    			 		 Test Type : LOSE_PACKET(1), DELAY_PACKET(2), DUPLICATE_PACKET (3), INVALID_FORMAT(4), UNKNOWN_TID (5)
    			 		 	Selected: DELAY_PACKET(2)
    			 		 Enter block num ( >= 0): 
    			 		 	Selected: 5
    			 		 Packet type? RRQ(1), WRQ(2), DATA(3), ACK(4) (Note: Understand which packet type it is at the block location.
    			 		 												It's different between RRQ and WRQ)
    			 		 	Selected: DATA
    			 		 Delay(ms):
    			 		 	Selected: 500 (half a second)
    			 		 (Go back to "Step 2").
    		
    	- If (false), the packet will ignore ErrorSimulator. Client will send and receive directly to/from Server.
    	
    8. User Interface will jump back to the main four input commands (Go back to "Step 2").
    
    9. Close the User Interface Window.
    
END


7. UPDATE NOTES (,)
========================================
*************************************************************************************************
*************************************************************************************************
April 3rd, 2018
ITERATION 5

Setup Instruction
========================================
To start the program run through the UserInterface class, input is tied to your current tab (Client tab will send client input etc.)
Testing Classes - Make it runs independently( Without the Client-Server running since it makes its own Mock Server)
New tab that allows user to input client and server IP. With this, you can send packet through seperate computers instead of just local protocols.


Class files: Client, ErrorSimulator, Server, ThreadedConnection, TFTPConnection, User Interface
clientTest, errorSimulator, serverTest, tftpConnectionTest; .java

Class Updates
========================================
- Implement network connection through computers.
- Fix to validating packet if it's unknown or illegal.

TFTPConnection
========================================
- Modified method to handle error codes 4 and 5

UserInterface
========================================
- Polished the UI
- Added Icon
- Made adjustments to take inputs for custom server/client IP 


*************************************************************************************************
*************************************************************************************************
March 24th, 2018
ITERATION 4

Setup Instruction
========================================
To start the program run through the UserInterface class, input is tied to your current tab (Client tab will send client input etc.)
Testing Classes - Make it runs independently( Without the Client-Server running since it makes its own Mock Server)


Class files: Client, ErrorSimulator, Server, ThreadedConnection, TFTPConnection, User Interface
clientTest, errorSimulator, serverTest, tftpConnectionTest; .java

Class Updates 
========================================
- Error Simulation now checks for Error Code 4 and 5
- TFTPConnection and ThreadedConnection handles the error
- UserInterface has been updated


ErrorSimulator
========================================
- Method created to handle Error Code 4 IllegalTFTP Packet
- Method created to handle Error Code 5 Unknown Packet


TFTPConnection
========================================
- Modified method to handle error codes 4 and 5


UserInterface
========================================
- Polished the UI
- New interface to show Client, Error, Server transfering packets concurrently

Diagrams
========================================
- Added Timing Diagram for Error Code 4 and 5


*************************************************************************************************
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
- Updated UCM, UML, Timing Diagram (Different Scenario depending on errors and sorcerer's apprentice bug)



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

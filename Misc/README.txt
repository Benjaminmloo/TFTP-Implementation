/** READ ME FILE
*
* Project: Design and Implementation of file transfer system based on the TFTP
* System consist of TFTP client - error - server.
*
* @author: Loo Ben, Nguyen Andrew, Morrissette Eric, Palko Ben, Westeinde Johan
* 
*/

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
Loo Ben: Error Simulator, Server, UDPConnection
Nguyen Andrew: UML, UCM, READ_ME
Morrissette Eric: Client, Trello
Palko Ben: Server, Threaded Connection, Trello
Westeinde Johan:: Testing
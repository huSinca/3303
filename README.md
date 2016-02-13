# 3303
##Iteration 2

This project consists of three java files; Client.java, Server.java and ErrorSimulator.java.
The system is able to perform a simple TFTP file transfer, allowing a client to either read from or write to a file on the server.
For this iteration, error simulating for errors 4 and 5 have been accounted for.

The system works in two modes; normal mode and test mode. The way it determines which mode to execute in is through the "testMode"
variable in Client.java ('false' meaning it will execute in normal mode and 'true' meaning it will execute in test mode.

**In order to switch between normal and test modes, this testMode variable must be manually changed in Client.java.**

To set up, Server.java must be run first, then, if running in test mode, ErrorSimulator.java, and finally Client.java. If running
in normal mode, ErrorSimulator.java does not have to be run.
Client.java will prompt for a transfer type and a file name, and upon receiving both will initiate the transfer.
ErrorSimulator.java only needs to be run in test mode. In normal mode it does not do anything and can be ignored.
Server.java will prompt for a shutdown upon execution.
This has no effect the server's function, and should be ignored until shutdown is desired.

This program was originally based off of the Assignment 1 code written by Praveenen.

###Client.java
Connects to a server to transfer a file.
Type of file transfer (read or write) and the name of the transferred file are given by the user via GUI.
Upon receiving transfer type and file name, the client creates an appropriate request packet to send to the server.
Upon receiving an acknowledgement by the server, the client will begin the file transfer.
During the transfer, if the client receives an invalid packet, it will send an ERROR packet, letting the server know that
something went wrong with the last packet.
If the client receives a packet from a transfer ID other than the one it expects (such as from a completely different server than
the one it is currently performing a file transfer with), then it will send an ERROR packet back to that sender and continue
waiting for a packet from its intended sender.
If the client itself receives an ERROR packet, it will re-send its previous packet.
Upon completion of the transfer, the client prompts the user for the next file transfer information (mode and file name). This
continues until the client is told that no more files are to be transferred, at which point the client terminates.

###Server.java
Waits on port 69 for a request packet.
Once received, the server will verify that it is a valid read or write request.
If it is found to be valid, the server creates a new thread responsible for performing that particular file transfer.
This means that the server is able to support multiple transfers at a time.
The original server thread then goes back to waiting on port 69, until it receives a shutdown command from the user.
Similar to the client, the server will also send an appropriate ERROR if it receives an invalid packet or a packet from an unknown
source. It will also re-send its previous packet if it receives an ERROR packet, in the same way as the client.

###ErrorSimulator.java
ErrorSimulator.java is used to test the client and server for error handling. It does this by regulating the packet transfers
between the client and the server, and altering the packets so that they trigger errors on either side.
TFTP errors tested for are error 4 (illegal TFTP operation) and error 5 (unknown transfer ID).

-
###Iteration Responsibilities
Kari: Implemented error 04

Calvin: Implemented error 05, testing

Omar: UML Class Diagram, testing

Praveenen: UCM for error 05, testing

Dylan: UCM for error 04, testing, documentation

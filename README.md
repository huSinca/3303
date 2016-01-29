# 3303
##Iteration 1

This project consists of three java files; Client.java, Server.java and ErrorSimulator.java.
The system is able to perform a simple TFTP file transfer, allowing a client to either read from or write to a file on the server.
For this iteration, it is assumed that no errors occur.

To set up, Server.java must be run first, then Client.java.
ErrorSimulator.java does not need to be run.
Client.java will prompt for transfer type and file name, and upon receiving both will initiate the transfer.
Server.java will prompt for a shutdown upon execution.
This has no effect the server's function, and can be ignored until shutdown is desired.

This program was based off of the Assignment 1 code written by Praveenen.

###Client.java
Connects to a server to transfer a file.
Type of file transfer (read or write) and the name of the transferred file are given by the user via GUI.
Upon receiving transfer type and file name, the client creates an appropriate request packet to send to the server.
Upon completion of transfer, the client prompts the user for the next file transfer information.
This continues until the user tells the client that no more files are to be transferred, at which point the client terminates.

###Server.java
Waits on port 69 for a packet.
Once recieved, the server will verify that it is a valid read or write request.
If it is found to be valid, the server creates a new thread responsible for communicating with that client.
This means that the server is able to support multiple clients at a time.
The original server thread then goes back to waiting on port 69, until it receives a shutdown command from the user.

###ErrorSimulator.java
For this iteration of the project, ErrorSimulator.java merely acts as an intermediate host, meaning all it does is pass packets it receives from the client straight to the server, and vice versa.

-
####Iteration Responsibilities
Kari: Use Case Maps

Omar: UML Class Diagram

Praveenen: Read and write funcitonality

Dylan: Client-server connection functionality (iteration 0)

Calvin: Documentation

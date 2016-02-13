import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

public class ErrorSimulator {
	/**
	 *  This socket will be used to send and receive to and from the client
	 */	
	private DatagramSocket clientSocket;
	
	/**
	 * This socket will be used to send and receive to and from the server
	 */
	private DatagramSocket serverSocket;

	public ErrorSimulator() {
		try {
			clientSocket = new DatagramSocket(68);
			serverSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return the port the client socket uses
	 */	
	public int getClientPort() {
		return clientSocket.getLocalPort();
	}
	
	/**
	 * Creates and returns a randomized array of bytes. Used to generate invalid packets.
	 * @param packet the initial, unaltered packet
	 * @return a byte array to be fed into the receiver with random data;
	 */	
	private byte[] createFalsePacket(DatagramPacket packet){
		byte[] randomData = new byte[packet.getLength()];
		for(int i = 4; i < packet.getLength(); i++){
			Random RNG = new Random();
			randomData[i] = (byte) RNG.nextInt(9);
		}
		packet.setData(randomData);
		return packet.getData();
	}

	/**
	 * Print out the type of a given packet.
	 * @param packet the packet to print the info of
	 */
	private void packetInfo(DatagramPacket packet)
	{
		String opCode = "unknown";
		switch(packet.getData()[1]) {
		case (1): opCode = "RRQ"; break;
		case (2): opCode = "WRQ"; break;
		case (3): opCode = "DATA"; break;
		case (4): opCode = "ACK"; break;
		case (5): opCode = "ERROR"; break;
		}
		System.out.println("Packet is a/an " + opCode + " packet.");
	}
	
	/**
	 * This method is used to run the error simulator.
	 * The error simulator will test both the client and server for error handling. It will test how they handle
	 * error 4 (invalid TFTP operation) and error 5 (unknown transfer ID) by regulating the packet transfers
	 * between them, and periodically sending intentionally invalid packets.
	 */
	public void runSimulator() {
		byte[] b = new byte[516];
		byte[] serverReceival = new byte[516];
		byte[] clientReceival = new byte[516];
		//Packet for initial receive from client
		DatagramPacket receival = new DatagramPacket(b, b.length);
		//Packet for receiving from the server
		DatagramPacket receiveServer = new DatagramPacket(serverReceival, serverReceival.length);
		//Packet for sending read/write requests to the server
		DatagramPacket sendREQ;
		//Packet for sending packets to the client
		DatagramPacket sendClient;
		//Packet for receiving from the client
		DatagramPacket receiveClient;
		//Packet for sending to the server
		DatagramPacket sendServer;
		//Socket used for testing error 5
		DatagramSocket testSocket;
		
		System.out.println("ErrorSimulator running");
		try {
			//Forward initial request packet
			clientSocket.receive(receival);
			System.out.println("Received read/write request from client.");
			packetInfo(receival);
			sendREQ = new DatagramPacket(b, receival.getLength(), InetAddress.getLocalHost(), 69);
			System.out.println("Forwarding request to server.");
			serverSocket.send(sendREQ);
			
			//Forward initial request response
			serverSocket.receive(receiveServer);
			System.out.println("Received a packet from the server.");
			packetInfo(receiveServer);
			sendClient = new DatagramPacket(serverReceival, receiveServer.getLength(), InetAddress.getLocalHost(), receival.getPort());
			System.out.println("Forwarding to client.");
			clientSocket.send(sendClient);
			
			//Forward packet normally
			receiveClient = new DatagramPacket(clientReceival, clientReceival.length);
			clientSocket.receive(receiveClient);
			System.out.println("Received a packet from the client.");
			packetInfo(receiveClient);
			sendServer = new DatagramPacket(clientReceival, receiveClient.getLength(), InetAddress.getLocalHost(), receiveServer.getPort());
			System.out.println("Forwarding to server");
			serverSocket.send(sendServer);
			
			//Test the client for error 4 by randomizing the contents of the server's next packet
			System.out.println("\nNow testing client for error 4 (illegal TFTP operation)...");
			serverSocket.receive(receiveServer);
			System.out.println("Received a packet from the server.");
			packetInfo(receiveServer);
			System.out.println("Randomizing packet contents...");
			sendClient.setData(createFalsePacket(receiveServer));
			System.out.println("Forwarding to client");
			clientSocket.send(sendClient);
			
			//Forward client's ERROR to server
			clientSocket.receive(receiveClient);
			System.out.println("Received a packet from the client");
			packetInfo(receiveClient);
			sendServer.setData(clientReceival);
			System.out.println("Forwarding to server");
			serverSocket.send(sendServer);
			
			//Forward server's re-sent packet to client
			serverSocket.receive(receiveServer);
			System.out.println("Received a packet from the server.");
			packetInfo(receiveServer);
			sendClient.setData(serverReceival);
			System.out.println("Forwarding to client");
			clientSocket.send(sendClient);
			
			//Test the server for error 4 by randomizing the contents of the client's next packet
			System.out.println("\nNow testing server for error 4 (illegal TFTP operation)...");
			clientSocket.receive(receiveClient);
			System.out.println("Received a packet from the client");
			packetInfo(receiveClient);
			System.out.println("Randomizing packet contents...");
			sendServer.setData(createFalsePacket(receiveServer));
			
			System.out.println("Forwarding to server");
			serverSocket.send(sendServer);
			
			//Forward server's ERROR to client
			serverSocket.receive(receiveServer);
			System.out.println("Received a packet from the server");
			packetInfo(receiveServer);
			sendClient.setData(serverReceival);
			System.out.println("Forwarding to client");
			clientSocket.send(sendClient);
			
			//Forward client's re-sent packet to server
			clientSocket.receive(receiveClient);
			System.out.println("Received a packet from the client.");
			packetInfo(receiveClient);
			sendServer.setData(clientReceival);
			System.out.println("Forwarding to server");
			serverSocket.send(sendServer);
			
			//Test the client for error 5 by sending a packet from an unexpected TID
			serverSocket.receive(receiveServer);
			System.out.println("\nReceived a packet from the server.");
			packetInfo(receiveServer);
			System.out.println("Now testing client for error 5 (unknown transfer ID)...");
			testSocket = new DatagramSocket();
			System.out.println("Forwarding a packet to client from an unknown socket port");
			testSocket.send(sendClient);
			testSocket.receive(receiveClient);
			System.out.println("Received a packet from the client.");
			packetInfo(receiveClient);
			
			//Test the server for error 5 by sending a packet from an unexpected TID
			System.out.println("\nNow testing server for error 5 (unknown transfer ID)...");
			testSocket = new DatagramSocket();
			System.out.println("Forwarding a packet to server from an unknown socket port");
			testSocket.send(sendServer);
			testSocket.receive(receiveServer);
			System.out.println("Received a packet from the server.");
			packetInfo(receiveServer);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[])
	{
		ErrorSimulator es = new ErrorSimulator();
		es.runSimulator();
	}
}

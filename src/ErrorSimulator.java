import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.Scanner;

public class ErrorSimulator extends Thread {
	/**
	 *  This socket will be used to send and receive to and from the client
	 */	
	private DatagramSocket clientSocket;

	private DatagramSocket serverSocket;

	private DatagramSocket newClientSocket;

	private Errors errorType;

	private String packetToPerform;

	private int blockToPerform;

	private int client;

	private int server;

	public ErrorSimulator() {
		try {
			clientSocket = new DatagramSocket(68);
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
	private String packetInfo(DatagramPacket packet)
	{
		String opCode = "unknown";
		switch(packet.getData()[1]) {
		case (1): opCode = "RRQ"; break;
		case (2): opCode = "WRQ"; break;
		case (3): opCode = "DATA"; break;
		case (4): opCode = "ACK"; break;
		case (5): opCode = "ERROR"; break;
		}
		return opCode;
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
			serverSocket = new DatagramSocket();
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

	public void checkPacket(DatagramPacket p, int sendPort) {
		if (errorType != Errors.NORMAL_MODE) {
			String currentPacket = packetInfo(p);
			if (currentPacket.equals(packetToPerform) && p.getData()[3] == (byte) blockToPerform) {
				if (errorType.equals(Errors.INVALID_TID)) {
					simulateError5(p, sendPort);
				} else if (errorType.equals(Errors.ILLEGAL_TFTP_OP)) {
					simulateError4(p, sendPort);
				}
			}
		}
	}

	private void simulateError4(DatagramPacket p, int sendPort) {
		System.out.println("Invalid TFTP Operation Simulation (Error Code 4)");
		String first, second;
		DatagramSocket firstSocket, secondSocket;
		int firstPort, secondPort;
		if (sendPort == server) {
			first = "Server"; 
			firstSocket = serverSocket;
			firstPort = server;
			second = "Client";
			secondSocket = newClientSocket;
			secondPort = client;
		} else {
			first = "Client";
			firstSocket = newClientSocket;
			firstPort = client;
			second = "Server";
			secondSocket = serverSocket;
			secondPort = server;
		}
		
		byte[] b = createFalsePacket(p);
		try {
			System.out.println("Sending Invalid packet to " + first);
			DatagramPacket packet = new DatagramPacket(b, b.length, InetAddress.getLocalHost(), firstPort);
			firstSocket.send(packet);
			firstSocket.receive(packet);
			System.out.println("Received " + packetInfo(packet) + " from " + first);
			System.out.println("Sending " + packetInfo(packet) + " to " + second);
			p.setPort(secondPort);
			secondSocket.send(packet);
			//secondSocket.receive(packet);
			System.out.println("Received " + packetInfo(packet) + "from " + second);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private void simulateError5(DatagramPacket p, int sendPort) {
		System.out.println("Invalid TID Simulation (Error Code 5)");
		try {
			String first, second;
			if (sendPort == server) {
				first = "Server"; 
				second = "Client";
			} else {
				first = "Client";
				second = "Server";
			}
			DatagramSocket TIDErrorSocket = new DatagramSocket();
			DatagramPacket newPacket = new DatagramPacket(p.getData(), p.getLength(),
					InetAddress.getLocalHost(), sendPort);
			System.out.println("Sending to " + first + " using new Socket");
			TIDErrorSocket.send(newPacket);
			byte[] b = new byte[516];
			DatagramPacket receive = new DatagramPacket(b, 516);
			TIDErrorSocket.receive(receive);
			if (packetInfo(receive).equals("ERROR")) {
				System.out.println("Received a " + packetInfo(receive) + "packet from " + first);
			}
			if (sendPort == server) receive.setPort(client);
			else receive.setPort(server);
			TIDErrorSocket.send(receive);
			System.out.println("Sending Error Packet to " + second);						
			b = new byte[516];
			receive = new DatagramPacket(b, 516);
			TIDErrorSocket.receive(receive);
			System.out.println("Received a " + packetToPerform + "from " + second);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		byte[] b = new byte[516];
		DatagramPacket receival = new DatagramPacket(b, b.length);
		System.out.println("ErrorSimulator running for Client: " + getClientPort());
		try {
			serverSocket = new DatagramSocket();
			newClientSocket = new DatagramSocket();
			clientSocket.receive(receival);
			client = receival.getPort();
			DatagramPacket send = new DatagramPacket(b, receival.getLength(), InetAddress.getLocalHost(), 69);
			checkPacket(receival, server);
			serverSocket.send(send);
			while (true) {				
				byte[] serverReceival = new byte[516];
				DatagramPacket receiveFromServer = new DatagramPacket(serverReceival, serverReceival.length);
				serverSocket.receive(receiveFromServer);
				server = receiveFromServer.getPort();
				checkPacket(receiveFromServer, client);
				DatagramPacket sendClientPacket = new DatagramPacket(serverReceival, receiveFromServer.getLength(), InetAddress.getLocalHost(), receival.getPort());
				newClientSocket.send(sendClientPacket);

				byte[] clientReceival = new byte[516];
				DatagramPacket receiveClientPacket = new DatagramPacket(clientReceival, clientReceival.length);
				newClientSocket.receive(receiveClientPacket); 
				checkPacket(receiveClientPacket, server);
				DatagramPacket sendServer = new DatagramPacket(clientReceival, receiveClientPacket.getLength(), InetAddress.getLocalHost(), receiveFromServer.getPort());
				serverSocket.send(sendServer);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void displayPossibleCommands() {
		System.out.println("Possible Commands: ");
		for (Errors s : Errors.values()) System.out.println(s.toString());
	}

	public static void main(String args[])
	{
		ErrorSimulator es = new ErrorSimulator();
		es.start();
		while (true) {
			Scanner userInput = new Scanner(System.in);
			System.out.println("Enter New Command (Enter \"commands\" for list of commands):");
			String input = userInput.nextLine().toLowerCase();	
			if (input.equals("commands")) {
				es.displayPossibleCommands();
				continue;
			} else if (input.equals(Errors.NORMAL_MODE.toString())) {;
			es.errorType = Errors.NORMAL_MODE;
			} else if (input.equals(Errors.INVALID_TID.toString())) {
				es.errorType = Errors.INVALID_TID;
			} else if (input.equals(Errors.ILLEGAL_TFTP_OP.toString())) {
				es.errorType = Errors.ILLEGAL_TFTP_OP;
			} else {
				System.out.println("The command that you have entered is not valid. Enter \"commands\" to see all valid commands");
				continue;
			}

			if (es.errorType != Errors.NORMAL_MODE) {
				System.out.println("Enter Block Number to Perform Error On: ");
				es.blockToPerform = new Integer(userInput.nextLine().toLowerCase());
				System.out.println("Enter Packet to Perform Error On: (DATA or ACK)");
				es.packetToPerform = userInput.nextLine().toUpperCase();
				System.out.println(es.blockToPerform);
				System.out.println(es.packetToPerform);
			}
		}
	}
}

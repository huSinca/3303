import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ErrorSimulator {
	
	/**
	 * This Socket will be set to use port 68 and will be used to receive from the Client
	 */
	private DatagramSocket clientSocket;
	
	/**
	 * This socket will be used to send and receive to and from the server
	 */
	private DatagramSocket serverSocket;

	public ErrorSimulator() {
		try {
			clientSocket = new DatagramSocket();
			serverSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				runSimulator();
			}
		}).start();
	}
	
	public int getClientPort() {
		return clientSocket.getLocalPort();
	}

	/**
	 * This method is used to run the intermediate host
	 */
	public void runSimulator() {
		byte[] b = new byte[512];
		DatagramPacket receival = new DatagramPacket(b, b.length);
		try {
			System.out.println("Sending to Server #1");
			clientSocket.receive(receival);
			DatagramPacket send = new DatagramPacket(b, receival.getLength(), InetAddress.getLocalHost(), 69);
			serverSocket.send(send);
			while (true) {
				System.out.println("Receiving From Server");
				byte[] serverReceival = new byte[4];
				DatagramPacket receiveFromServer = new DatagramPacket(serverReceival, serverReceival.length);
				serverSocket.receive(receiveFromServer);
				
				System.out.println("Send to Client");
				DatagramPacket sendClientPacket = new DatagramPacket(serverReceival, serverReceival.length, InetAddress.getLocalHost(), receival.getPort());
				clientSocket.send(sendClientPacket);
				
				System.out.println("Sending To Server #2");
				clientSocket.receive(receival);
				System.out.println(receiveFromServer.getPort());
				DatagramPacket sendServer = new DatagramPacket(b, receival.getLength(), InetAddress.getLocalHost(), receiveFromServer.getPort());
				serverSocket.send(sendServer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main( String args[] ) {
		ErrorSimulator h = new ErrorSimulator();
		h.runSimulator();
	}

}

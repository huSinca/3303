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
		byte[] b = new byte[516];
		DatagramPacket receival = new DatagramPacket(b, b.length);
		try {
			clientSocket.receive(receival);
			DatagramPacket send = new DatagramPacket(b, receival.getLength(), InetAddress.getLocalHost(), 69);
			serverSocket.send(send);
			while (true) {				
				byte[] serverReceival = new byte[516];
				DatagramPacket receiveFromServer = new DatagramPacket(serverReceival, serverReceival.length);
				serverSocket.receive(receiveFromServer);
				
				DatagramPacket sendClientPacket = new DatagramPacket(serverReceival, receiveFromServer.getLength(), InetAddress.getLocalHost(), receival.getPort());
				clientSocket.send(sendClientPacket);
				
				byte[] clientReceival = new byte[516];
				DatagramPacket receiveClientPacket = new DatagramPacket(clientReceival, clientReceival.length);
				clientSocket.receive(receiveClientPacket);
				DatagramPacket sendServer = new DatagramPacket(clientReceival, receiveClientPacket.getLength(), InetAddress.getLocalHost(), receiveFromServer.getPort());
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

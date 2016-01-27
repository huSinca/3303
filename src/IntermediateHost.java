import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class IntermediateHost {
	
	/**
	 * This Socket will be set to use port 68 and will be used to receive from the Client
	 */
	private DatagramSocket receiveSocket;
	
	/**
	 * This socket will be used to send and receive to and from the server
	 */
	private DatagramSocket sendReceiveSocket;

	public IntermediateHost() {
		try {
			receiveSocket = new DatagramSocket(68);
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to run the intermediate host
	 */
	public void runHost() {
		byte[] b = new byte[100];
		DatagramPacket receival = new DatagramPacket(b, b.length);
		try {
			while (true) {
				receiveSocket.receive(receival);
				System.out.println("--------------------------------------");
				System.out.println("Received the following from client: ");
				Client.printByteArray(b, receival.getLength());
				
				DatagramPacket send = new DatagramPacket(b, receival.getLength(), InetAddress.getLocalHost(), 69);
				System.out.println("Sending the following data to server at port 69: ");
				Client.printByteArray(b, receival.getLength());
				sendReceiveSocket.send(send);
				
				byte[] serverReceival = new byte[4];
				DatagramPacket receiveFromServer = new DatagramPacket(serverReceival, serverReceival.length);
				sendReceiveSocket.receive(receiveFromServer);
				System.out.println("Received the following from Server: ");
				Client.printByteArray(serverReceival, serverReceival.length);
				
				DatagramPacket sendClientPacket = new DatagramPacket(serverReceival, serverReceival.length, InetAddress.getLocalHost(), receival.getPort());
				DatagramSocket tempSocket = new DatagramSocket();
				System.out.println("Sending the following response to Client: ");
				Client.printByteArray(serverReceival, serverReceival.length);
				tempSocket.send(sendClientPacket);
				tempSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main( String args[] ) {
		IntermediateHost h = new IntermediateHost();
		h.runHost();
	}

}

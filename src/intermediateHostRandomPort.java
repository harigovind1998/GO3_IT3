import java.net.DatagramPacket;
import java.net.DatagramSocket;
public class intermediateHostRandomPort extends Thread {
	DatagramPacket serverRecieve;
	DatagramSocket sendSocket;
	int clientPort;
	ComFunctions com;
	public intermediateHostRandomPort(DatagramPacket packet, int clientPort, DatagramSocket sendSocket) {
		serverRecieve = packet;
		this.clientPort = clientPort;
		this.sendSocket = sendSocket;
		com = new ComFunctions();
	}
	
	public void run() {
		DatagramSocket clientSend = com.startSocket();
		DatagramPacket send = com.createPacket(serverRecieve.getData(), clientPort);
		com.sendPacket(send, clientSend);
		DatagramPacket clientPacket = com.recievePacket(clientSend, 100);
		com.sendPacket(clientPacket, sendSocket);
		clientSend.close();
	}

}

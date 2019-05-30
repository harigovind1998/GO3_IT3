import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.lang.Math; 
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.io.FileOutputStream; 
import java.io.OutputStream; 
import java.util.Scanner;

public class Client {
	ComFunctions com;
	DatagramSocket sendRecieveSocket;
	private static JFrame frame = new JFrame();
	private static JTextArea area = new JTextArea();
	private static JScrollPane scroll;
	private static byte[] messageReceived;
	public static  Path f2path = Paths.get("./Client/returnTest2.txt");
	private int fileLength;
	private static byte[] rrq = {0,1};
	private static byte[] wrq = {0,2};
	private static int mode;
	private int interHostPort = 23;
	
	public Client(){
		com = new ComFunctions();
		sendRecieveSocket = com.startSocket();
		try {
			sendRecieveSocket.setSoTimeout(1000);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		frame.setSize(420, 440);
		area.setBounds(10, 10, 380, 380);
		scroll = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.setSize(400, 400);
		
		frame.getContentPane().add(scroll);
		frame.setLayout(null);
		frame.setVisible(true);
	}
	
	/**
	 * Sends a local file to the server, uses a similar logic as the server read method
	 * @param name
	 * @param format
	 */
	public void writeFile(String name, String format) {
		byte[] fileAsByteArr = com.readFileIntoArray("./Client/"+name);
		fileLength = fileAsByteArr.length;
		byte[] request = com.generateMessage(wrq, name, format);
		DatagramPacket sendPacket =  null;
		DatagramPacket requestPacket = com.createPacket(request, interHostPort); //creating the datagram, specifying the destination port and message
		DatagramPacket recievePacket = com.createPacket(com.UNKNOWNLEN);
		reqLoop:
		while(true) {			
			com.sendPacket(requestPacket, sendRecieveSocket);
			if (mode == 1) {
				com.verboseMode("Sent", requestPacket, area);
			}
			try {
				sendRecieveSocket.receive(recievePacket);
				if (mode == 1) {
					com.verboseMode("Recieved", recievePacket, area);
				}
				if(!com.CheckAck(recievePacket, 0)) {
					System.out.println("Not the correct ACK packet, resending");
				}else {
					break reqLoop;
				}
			} catch (Exception e) {
			// TODO: handle exception
				com.verboseMode("ReSending", requestPacket, area);
			}
		}
		
		byte[] fileBlock = null;
		byte [] msg =  null;
		int numOfBlocks = (int) Math.ceil(fileLength / 512);
		numOfBlocks ++;
		for(int i = 1; i < numOfBlocks; i++) {
			fileBlock = com.getBlock(i, fileAsByteArr);

			msg = com.generateDataPacket(com.intToByte(i), fileBlock);
	
			sendPacket = com.createPacket(msg, interHostPort); //creating the datagram, specifying the destination port and message

			recievePacket = com.createPacket(com.KNOWNLEN);
			sendLoop:
				while(true) {
					com.sendPacket(sendPacket, sendRecieveSocket);
					if(mode == 1) {
						com.verboseMode("Sent Packet:", sendPacket, area);
					}
					try {
						boolean temp = true;
						while(temp) {
							sendRecieveSocket.receive(recievePacket);
							if(com.CheckAck(recievePacket, i)) {
								temp = false;
								messageReceived = recievePacket.getData();
								if (mode == 1) {
									com.verboseMode("Received Packet:", recievePacket, area);
								}
								break sendLoop;
							}else {
								area.append("Wrong AckPacket Recieved, ignoring\n");
							}
							
						}
					} catch (Exception e) {
						// TODO: handle exception
						com.verboseMode("Preparing to resend packet:", sendPacket, area);
					}
				}
		}
		area.append("End of File reached!\n");
	}
	
	/**
	 * Reads a remote file from the server, uses a similar as the write method in the server
	 * @param name
	 * @param format
	 */
	public void readFile(String name, String format) {
		byte[] msg = com.generateMessage(rrq, name, format);
		byte[] blockNum =  new byte[2];
		
		File yourFile = new File("./Client/" + name);
		try {
			yourFile.createNewFile();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.exit(0);
		}
		
		f2path = Paths.get("./Client/" + name);
		DatagramPacket sendPacket = com.createPacket(msg, interHostPort); //creating the datagram, specifying the destination port and message
		
		DatagramPacket recievePacket =  com.createPacket(516);
		byte[] dataReceived = null;
		
		int expectedBlock = 1;
		
		outerloop:
		while(true) {
			sendLoop:
			while(true) {
				com.sendPacket(sendPacket, sendRecieveSocket);
				if (mode == 1) {
					com.verboseMode("Sent", sendPacket, area);
				}
				try {
					innerLoop:
					while(true) {
						sendRecieveSocket.receive(recievePacket);
						if (mode == 1) {
							com.verboseMode("Received Packet:", recievePacket, area);
						}
						messageReceived = recievePacket.getData();
						//Add check  to see if the packet is a data Packet
						blockNum[0] =  messageReceived[2];
						blockNum[1] = messageReceived[3];
						dataReceived = com.parseBlockData(messageReceived);
						byte[] ackMsg = com.generateAckMessage(blockNum);
						sendPacket = com.createPacket(ackMsg, interHostPort);
						if(com.intToByte(expectedBlock)[0] == blockNum[0]&& com.intToByte(expectedBlock)[1] == blockNum[1]) {
							expectedBlock++;
							try {
								Files.write(f2path, dataReceived, StandardOpenOption.APPEND);
							}catch (IOException e) {
								e.printStackTrace();
							}
							//System.arraycopy(dataReceived, 0, fileContent, 0, dataReceived.length);

							if(dataReceived[511] == (byte)0) {
								com.sendPacket(sendPacket, sendRecieveSocket);
								if (mode == 1) {
									com.verboseMode("Sent Packet:", sendPacket, area);
								}
								break outerloop;
							}
							break innerLoop;
						}else {
							area.append("Wrong Packet Recieved, ignoring\n");
						}
					}
					break sendLoop;
				} catch (Exception e) {
					// TODO: handle exception
					com.verboseMode("Preparing to resend packet:", sendPacket, area);
				}
				
			}			
		}
		area.append("End of File reached!\n");
		
	}
	
	public static void main(String[] args) {
		Client client = new Client();
		Scanner sc = new Scanner(System.in);
		System.out.println("Select Mode : Quiet [0], Verbose [1]");
		mode = sc.nextInt();
		sc.close();
		System.out.println(mode);

		//client.readFile("readTest.txt", "Ascii");
		
		client.writeFile("writeTest.txt", "Ascii");
		

	}
}

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
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Client {
	ComFunctions com;
	DatagramSocket sendReceiveSocket;
	private static JFrame frame = new JFrame();
	private static JTextArea area = new JTextArea();
	private static JScrollPane scroll;
	private static byte[] messageReceived;
	public static  Path f2path = Paths.get("./Client/returnTest2.txt");
	private int fileLength;
	private static byte[] rrq = {0,1};
	private static byte[] wrq = {0,2};
	private static int mode, simMode;
	private int interHostPort = 23;
	private  boolean TIDSet = false;
	/**
	 * The Client presents the user with a front end GUI that displays data being passed and received to and from the server. The client can either read a file from
	 * the server, making a local copy in the ./Client/ folder or write a file to the server, this will take a file in ./Client/ transfer it to the client that will
	 * create a copy in ./Server/ 
	 */
	public Client(){
		//If we arent simulating, we can bypass the errror simulator and create a connection directly with the server
		if((int)simMode == 1) {
			interHostPort = 69;
		}
		com = new ComFunctions();
		sendReceiveSocket = com.startSocket(); //Socket that is used to send and receive packets from the server/errorSimulator
		try {
			sendReceiveSocket.setSoTimeout(10000); //Setting a socket timeout for receive, used mostly re-send packets
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		//Making a GUI to display passed information
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
		
		byte[] msg =com.generateMessage(wrq, name, format);
		DatagramPacket sendPacket = com.createPacket(516);
		DatagramPacket recievePacket = com.createPacket(100);
		
		
		sendPacket = com.createPacket(msg, interHostPort); //creating the datagram, specifying the destination port and message
		
		byte[] fileAsByteArr = com.readFileIntoArray("./Client/"+name);
		int blockNum = 0;
		
		mainLoop:
		while(true){
			//Loop that is in charge of sending/re-sending the data packet
			outterSend:
				while(true) {
					com.sendPacket(sendPacket, sendReceiveSocket); //Send message
					if(mode == 1) {
						com.verboseMode("Sent Packet:", sendPacket, area);
					}
					try {
						//Loop that ensures that the incoming AckPackets are correct, if it isn't, it will loop back to listening until the correct Ack is received or the the socket receive times out
						innerSend:
							while(true) {
								sendReceiveSocket.receive(recievePacket);
								if(mode == 1) {
									com.verboseMode("Recieved Packet:", recievePacket,area);
								}
								
								if((int)simMode == 1 && !TIDSet) {
									interHostPort = recievePacket.getPort();
									TIDSet = true;
								}
								if(recievePacket.getPort() != interHostPort) {
									msg = com.generateErrMessage(new byte[] {0,5}, "");
									sendPacket = com.createPacket(msg, recievePacket.getPort());
									com.sendPacket(sendPacket, sendReceiveSocket); //Send message
									if(mode == 1) {
										com.verboseMode("Sent Packet:", sendPacket, area);
									}
									break innerSend;
								}
								if(com.getPacketType(recievePacket)== 4) {
									if(com.CheckAck(recievePacket, blockNum)){
										if(sendPacket.getData()[sendPacket.getLength() -1] == 0 && sendPacket.getData()[0] == 0 && sendPacket.getData()[1] == 3 ){ //Checks to see if the file has come to an end
											area.append("End of File reached!\n");
											break mainLoop;
										}	
											blockNum ++ ;
											msg = com.generateDataPacket(com.intToByte(blockNum), com.getBlock(blockNum, fileAsByteArr));
											sendPacket = com.createPacket(msg, interHostPort);
											break innerSend;
									}else {
										area.append("Wrong block recieved, continue waiting...\n");
									}
								}else if (com.getPacketType(recievePacket)==5){
									if(recievePacket.getData()[3]==4){
										area.append("Error Code 4 received. Terminating connection\n");
										break mainLoop;
									}else if(recievePacket.getData()[3]==5) {
										area.append("Connection with original server lost. Terminating connection");
										break mainLoop;
									}
								}else {
									msg = com.generateErrMessage(new  byte[] {0,4}, "");
									sendPacket =  com.createPacket(msg,interHostPort);
									com.sendPacket(sendPacket, sendReceiveSocket); //Send message
									if(mode == 1) {
										com.verboseMode("Sent Packet:", sendPacket, area);
										area.append("Terminating connection.\n");
									}
									break mainLoop;
								}
							}
						break outterSend; //DataPacket sent and the right AckReceived hence continues on to the next packet
					} catch (IOException e) {
						if(mode == 1) {
							com.verboseMode("Preparing to resend packet:", sendPacket, area);
						}
					}
				}
		}
		
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
			//Can use this to create I/O error packets
			e.printStackTrace();
		}
		f2path = Paths.get("./Client/" + name);
		DatagramPacket sendPacket = com.createPacket(msg, interHostPort); //creating the datagram, specifying the destination port and message
		
		DatagramPacket recievePacket =  com.createPacket(516);
		byte[] dataReceived = null;
		
		int expectedBlock = 1;
		
		outerloop:
		while(true) {
			com.sendPacket(sendPacket, sendReceiveSocket);
			if (mode == 1) {
				com.verboseMode("Sent", sendPacket, area);
			}
			innerLoop:
			while(true) {
				try {
					sendReceiveSocket.receive(recievePacket);
				}catch(Exception e) {
					com.verboseMode("Preparing to resend packet:", sendPacket, area);
					break innerLoop;
				}
				if (mode == 1) {
					com.verboseMode("Received Packet:", recievePacket, area);
				}
				if((int)simMode == 1 && !TIDSet) {
					interHostPort = recievePacket.getPort();
					TIDSet = true;
				}
				messageReceived = recievePacket.getData();
				//Add check  to see if the packet is a data Packet
				blockNum[0] =  messageReceived[2];
				blockNum[1] = messageReceived[3];
				dataReceived = com.parseBlockData(messageReceived);		
				if(com.intToByte(expectedBlock)[0] == blockNum[0]&& com.intToByte(expectedBlock)[1] == blockNum[1]) {
					byte[] ackMsg = com.generateAckMessage(blockNum);
					sendPacket = com.createPacket(ackMsg, interHostPort);
					expectedBlock++;
					try {
						Files.write(f2path, dataReceived, StandardOpenOption.APPEND);
					}catch (IOException e) {
						e.printStackTrace();
					}
					if(dataReceived[511] == (byte)0) {
						com.sendPacket(sendPacket, sendReceiveSocket);
					if (mode == 1) {
						com.verboseMode("Sent last Packet:", sendPacket, area);
					}
						break outerloop; //File transfer has completed
					}
					break innerLoop; //Right packet has been received
				}else {
						area.append("Wrong Packet Recieved, ignoring\n");
				}
			}
			
		}
		area.append("End of File reached!\n");	
	}
	
	public static void main(String[] args) {
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Select Mode : Quiet [0], Verbose [1]");
		mode = sc.nextInt();
		System.out.println("Error Simulation mode? Yes [0], No [1]");
		simMode= sc.nextInt();
		
		System.out.println("Select Operation: Read [0], Write[1]");
		int rwMode = sc.nextInt();
		System.out.println("Type in file name with file extension i.e '.txt'");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String fileName = null;
		try {
			fileName = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sc.close();
		Client client = new Client();
		if(rwMode == 0) {
			client.readFile(fileName, "Ascii");
		}else if (rwMode == 1) {
			client.writeFile(fileName, "Ascii");
		}

	}
}

package Server;

import java.io.*;
import java.net.*;
import java.util.*;

import AuxClass.Trame;

import java.nio.charset.StandardCharsets;

import Couche.Liaison;

public class QuoteServerThread extends Thread {

	protected DatagramSocket socket = null;
	protected BufferedReader in = null;
	protected boolean moreQuotes = true;
	Liaison liaison = new Liaison();
	List<Trame> receivedPackets = new ArrayList<>();
	Trame receivedtrame;
	Trame responseTrame;
	int errorCount = 0;

	public QuoteServerThread() throws IOException {
		this("QuoteServerThread");
	}

	public QuoteServerThread(String name) throws IOException {
		super(name);
		socket = new DatagramSocket(25002);

	}

	public void run() {

		while (moreQuotes) {
			try {
				byte[] buf = new byte[200];
				// receive request
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);

				receivedtrame = liaison.getTrame(new String(packet.getData()));

				if (liaison.validateTrameCRC(receivedtrame)) {
					 // Look for missing packet
                    int checkResponse;

                    // Create error
                    if (receivedtrame.getPacketNumberInt() == 60) {
                        checkResponse = liaison.checkForSkipedPacket(receivedPackets,
                                receivedtrame.getPacketNumberInt() + 1);
                    } else {
                        checkResponse = liaison.checkForSkipedPacket(receivedPackets,
                                receivedtrame.getPacketNumberInt());
                    }

                    switch (checkResponse) {
                    case 0:
                        System.out.println("Recu packet no: " + receivedtrame.getPacketNumberInt());

                        responseTrame = new Trame("0SUCCESS".getBytes());
                        break;
                    case 1:
                        System.out.println("Manque packet no: " + (receivedtrame.getPacketNumberInt()/*- 1 (� enlever ) */));
                        responseTrame = new Trame("1MISSINGPACKET".getBytes());
                        break;
                    default:
                        break;
                    }
                    receivedPackets.add(receivedtrame);
                }else {
					System.out.println("CRC du paquet no: " + new String(receivedtrame.getPacketNumber()) + " non valide!!!!");
					responseTrame = new Trame("2CRCERROR".getBytes());
				}
				
				buf = responseTrame.getTrame();
				//System.out.println(new String(buf));
				InetAddress address = packet.getAddress();
				int port = packet.getPort();
				packet = new DatagramPacket(buf, buf.length, address, port);
				socket.send(packet);
				
				if (receivedtrame.getPacketNumberInt() == receivedtrame.getPacketAmountInt()) {
                    moreQuotes = false;
                }
				
			} catch (IOException e) {
				e.printStackTrace();
				moreQuotes = false;
			}
		}
		socket.close();
		SaveFile();
	}
	
	private void SaveFile() {
        int numberOfPackets = receivedPackets.get(0).getPacketAmountInt();
        String dataToWrite;
        String nameOfFile = getFileName();
        PrintWriter writer;

        // approximate size of file to create: get size of first real packet * total
        // number
        int sizeInBytes = receivedPackets.get(1).getTrame().length * numberOfPackets;

        System.out.println(numberOfPackets + "  " + sizeInBytes);
        System.out.println(nameOfFile);

        File file = new File(nameOfFile + "Received.txt"); // Create a temporary file to reserve memory.
        long bytes = sizeInBytes; // number of bytes reserved.
        RandomAccessFile rf = null;
        try {
            rf = new RandomAccessFile(file, "rw"); // rw stands for open in read/write mode.
            rf.setLength(bytes); // This will cause java to "reserve" memory for your application by
                                // inflating/truncating the file to the specific size.

            for (int i = 1; i < numberOfPackets; i++) {
                rf.writeBytes(new String(receivedPackets.get(i).getData()));
            }
        } catch (IOException ex) {
            System.out.println(ex.toString());
        } finally {
            if (rf != null) {
                try {
                    rf.close();
                } catch (IOException ioex) {
                    System.out.println(ioex.toString());
                }
            }
        }
    }

    private String getFileName() {
        // get file name except for the extension
        String CompleteFileName = new String(receivedPackets.get(0).getData());
        String[] parts = CompleteFileName.split(".");
        String FileName = "";
        for (int i = 0; i < parts.length; i++) {

            FileName += parts[i];
        }
        return FileName;
    }

}
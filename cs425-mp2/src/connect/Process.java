package connect;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;

import message.Message;
import message.RegularMessage;
import util.Configuration;

public class Process {
	static DatagramSocket Socket;
	public static int ID; // use id to link to port
	public static String IP;
	public static int myPort;
	public static int messageID;
	public static DatagramChannel mychannel;
	public static int numProc; // number of processes
	public static ArrayList<String> send_msg;
	public static ArrayList<String> received;
	// public static boolean[][] ack;
	public static int delayTime;
	public static int dropRate;
	public static int[] recent;
	public static Queue<RegularMessage> my_queue;
	public static String orderingType = "";

	public static void main(String args[]) throws IOException,
			InterruptedException {
		// User determine id
		System.out.println("Enter the ID starting from 0 : ");
		Scanner scanner = new Scanner(System.in);
		ID = scanner.nextInt();

		// Get configuration values
		Configuration.getInstance();
		IP = Configuration.IP[ID];
		orderingType = Configuration.orderingType;
		numProc = Configuration.numProc;
		delayTime = Configuration.delayTime[ID];
		dropRate = Configuration.dropRate[ID];

		System.out
				.println(String
						.format("Process ID = %d\nIP = %s\nordering = %s\ndelay = %d ms\ndrop rate = %d\n",
								ID, IP, orderingType, delayTime, dropRate));

		messageID = 0;

		// ack = new boolean[num_proc][1000];
		send_msg = new ArrayList<String>();
		received = new ArrayList<String>();
		recent = new int[numProc];
		my_queue = new LinkedList<RegularMessage>();

		myPort = ID + 6000; // define every process's port by the ID
		mychannel = DatagramChannel.open();
		System.out.println(myPort);

		// mychannel.socket().bind(new
		// InetSocketAddress(InetAddress.getByName("localhost"),myPort));
		mychannel.socket().bind(new InetSocketAddress(IP, myPort));
		// set the channel to non-blocking
		mychannel.configureBlocking(false);
		Process_send send_thread = new Process_send();
		new Thread(send_thread).start();

		String mystr = null;
		while (true) {
			r_multicast_recv();
		}

	}

	public static boolean compare(int[] array1, int[] array2) {
		boolean flag = true;
		for (int i = 0; i < numProc; i++) {
			flag = flag && (array1[i] >= array2[i]);
		}
		return flag;
	}

	public static void r_multicast_recv() throws IOException {
		Message recv_msg = null;

		for (int i = 0; i < numProc; i++) {
			recv_msg = unicast_receive(i, recv_msg);
			if (recv_msg.isRegular()) {
				if (!received.contains(((RegularMessage) recv_msg).content)
						&& (recv_msg != null)) {
					received.add(((RegularMessage) recv_msg).content);
					// check if the message received is originated by this
					// process
					if (!send_msg.contains(((RegularMessage) recv_msg).content)) {
						b_multicast((RegularMessage) recv_msg);
					}
					if (recv_msg != null) {
						// if in received message in causal ordering
						if (compare(recent, ((RegularMessage) recv_msg).recent)) {
							recent[recv_msg.from] = recv_msg.messageID;
							System.out.println("Delivers "
									+ ((RegularMessage) recv_msg).content);
						} else
							my_queue.add((RegularMessage) recv_msg);
					}
					while ((my_queue.peek() != null)
							&& compare(recent, my_queue.peek().recent)) {
						recent[my_queue.peek().from] = my_queue.peek().messageID;
						System.out.println("Delivers "
								+ my_queue.poll().content);
					}
				}

			}
		}
	}

	public static void b_multicast(RegularMessage message) throws IOException {
		// b-multicast to group
		for (int i = 0; i < numProc; i++) {
			for (int j = 0; j < 10; j++) {
				// System.out.println("Send to "+ i);
				message.to = i;
				unicast_send(i, message);
			}
		}
	}

	public static Message unicast_receive(int sourceID, Message message)
			throws IOException {

		int sourcePort = 6000 + sourceID;

		ByteBuffer buffer = ByteBuffer.allocate(1000);
		while (mychannel.receive(buffer) == null) {

			// mychannel.receive(buffer);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		buffer.flip();
		ByteArrayInputStream in = new ByteArrayInputStream(buffer.array());
		ObjectInputStream is = new ObjectInputStream(in);

		try {
			message = (Message) is.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return message;

	}

	public static void unicast_send(int destID, RegularMessage message)
			throws IOException {
		Random rand = new Random();
		int rand_num = rand.nextInt(3);
		Random ano_rand = new Random();
		int ano_num = rand.nextInt(2 * delayTime + 1);
		if (rand_num == 0) {
			DatagramChannel channel;
			channel = DatagramChannel.open();
			int destPort = 6000 + destID;
			try {
				InetSocketAddress destAddress = new InetSocketAddress(
						InetAddress.getByName(IP), destPort);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(outputStream);
				os.writeObject(message);
				byte[] data = outputStream.toByteArray();
				ByteBuffer buffer = ByteBuffer.wrap(data);
				channel.connect(new InetSocketAddress(IP, destPort));
				// randomized dalay
				Thread.sleep(ano_num);
				int bytesend = channel.write(buffer);
				channel.disconnect();
				// System.out.println("send "+ bytesend + " bytes");
				channel.close();
				// Thread.sleep(2000);

			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

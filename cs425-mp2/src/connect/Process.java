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
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import message.Ack;
import message.Message;
import message.OrderMessage;
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
	public static boolean[][] ack;
	public static int delayTime = 0;
	public static int dropRate = 0;
	public static int[] recent;
	public static Queue<RegularMessage> my_queue;
	public static Queue<RegularMessage> input_queue;
	public static String orderingType = "";

	/****************** For total ordering ******************/
	public static int rg;
	public static int sg; // For sequencer(P0) only
	public static Hashtable<Integer, RegularMessage> holdBackQueue;

	public static void main(String args[]) throws IOException,
			InterruptedException {
		// User determine id
		Scanner scanner = new Scanner(System.in);
		do {
			System.out.println("Enter the ID starting from 0 (to 5): ");
			ID = scanner.nextInt();
		} while (ID < 0 || ID > 5);

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

		/****************** For total ordering ******************/
		// On initialization for group member
		rg = 0;
		// Use hashtable as hold-back queue to pair messageID(key) with
		// message(value)
		holdBackQueue = new Hashtable<Integer, RegularMessage>();
		// P0 should always be the sequencer
		// So B-multicast(g U {sequencer(g)}, <m, i>) is just B-multicast(g, <m,
		// i>)
		if (ID == 0) {
			// On initialization for sequencer
			sg = 0;
		}

		messageID = 0;
		// initialize all the acks to false
		ack = new boolean[numProc][1000];
		for (int i = 0; i < numProc; i++) {
			for (int j = 0; j < 1000; j++) {
				ack[i][j] = false;
			}
		}
		send_msg = new ArrayList<String>();
		received = new ArrayList<String>();
		recent = new int[numProc];
		for (int i = 0; i < numProc; i++)
			recent[i] = 0;
		my_queue = new LinkedList<RegularMessage>();
		input_queue = new LinkedList<RegularMessage>();
		myPort = ID + 6000; // define every process's port by the ID
		mychannel = DatagramChannel.open();
		System.out.println(myPort);

		// mychannel.socket().bind(new
		// InetSocketAddress(InetAddress.getByName("localhost"),myPort));
		mychannel.socket().bind(new InetSocketAddress(IP, myPort));
		// set the channel to non-blocking
		mychannel.configureBlocking(false);
		// Thread for reading user input
		ReadInput input_thread = new ReadInput();
		new Thread(input_thread).start();
		// Thread for initiate multicast
		Process_send send_thread = new Process_send();
		new Thread(send_thread).start();

		if (orderingType.equals("causal"))
			System.out.println("causal ordering");
		else
			System.out.println("total ordering");
		while (true) {
			if (orderingType.equals("causal")) {
				r_multicast_recv_causal();
			} else {
				r_multicast_recv_total();
			}
		}

	}

	public static boolean compare(int[] array1, int[] array2) {
		boolean flag = true;
		for (int i = 0; i < numProc; i++) {
			flag = flag && (array1[i] >= array2[i]);
		}
		return flag;
	}

	public static void r_multicast_recv_causal() throws IOException {
		Message recv_msg = null;

		for (int i = 0; i < numProc; i++) {
			recv_msg = unicast_receive(i, recv_msg);
			if (recv_msg.isRegular()) {
				if (!received.contains(((RegularMessage) recv_msg).content)
						&& (recv_msg != null)) {
					received.add(((RegularMessage) recv_msg).content);
					// check if the message received is originated by this
					// process
					// if (!send_msg.contains(((RegularMessage)
					// recv_msg).content)) {
					// b_multicast((RegularMessage) recv_msg);
					// }
					if (recv_msg != null) {
						// if in received message in Causal Ordering
						Lock lock = new ReentrantLock();
						lock.lock();
						if (compare(recent, ((RegularMessage) recv_msg).recent)) {
							recent[recv_msg.from] = recv_msg.messageID;
							System.out.println("Delivers "
									+ ((RegularMessage) recv_msg).content);
						} else
							my_queue.add((RegularMessage) recv_msg);
						lock.unlock();
					}
					while ((my_queue.peek() != null)
							&& compare(recent, my_queue.peek().recent)) {
						Lock lock = new ReentrantLock();
						lock.lock();
						recent[my_queue.peek().from] = my_queue.peek().messageID;
						System.out.println("Delivers "
								+ my_queue.poll().content);

						lock.unlock();
					}
				}
			}
		}
	}

	private static void r_multicast_recv_total() throws IOException {
		Message recv_msg = null;

		for (int j = 0; j < numProc; j++) {
			recv_msg = unicast_receive(j, recv_msg);
			if (recv_msg.isRegular()) {
				if (!received.contains(((RegularMessage) recv_msg).content)
						&& (recv_msg != null)) {
					received.add(((RegularMessage) recv_msg).content);
					// On B-deliver(<m, i>) with g = group(m)
					// Place <m, i> in hold-back queue;
					holdBackQueue.put(((RegularMessage) recv_msg).messageID,
							(RegularMessage) recv_msg);
				}
				// For sequencer e.g. process 0
				// On B-deliver(<m, i>)
				if (ID == 0) {
					// System.out.println("Sequencer working...");
					OrderMessageSend sendOrderThread = new OrderMessageSend(
							messageID, recv_msg.messageID, sg);
					new Thread(sendOrderThread).start();
					sg++;
				}
			}
			if (recv_msg.isOrderMessage()) {
				// On B-deliver( m order = <“order”, i, S>) with g = group(m)
				int i = ((OrderMessage) recv_msg).i;
				int S = ((OrderMessage) recv_msg).S;
				// wait until <m, i> in hold-back queue and S = rg
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// System.out.println("size=" + holdBackQueue.size());
					if (holdBackQueue.containsKey(i) && S == rg) {
						break;
					}
				}
				// Delete m from hold-back queue, deliver m, rg = S + 1
				String readyToDeliver = holdBackQueue.get(i).content;
				holdBackQueue.remove(i);
				System.out.println("Delivers " + readyToDeliver);
				rg = S + 1;
			}
		}
	}

	public static Message unicast_receive(int sourceID, Message message)
			throws IOException {

		int sourcePort = 6000 + sourceID;

		ByteBuffer buffer = ByteBuffer.allocate(1000);
		while (mychannel.receive(buffer) == null) {
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
		// if the message is regular message, send ack back to sender
		if (message.isRegular()) {
			Ack my_ack = new Ack(message.to, message.from, message.messageID);
			// System.out.println(String.format("sending ack to %d, for REGULAR msg %d",
			// my_ack.to, message.messageID));
			unicast_send_ack(message.from, my_ack);
		} else if (message.isOrderMessage()) {
			Ack my_ack = new Ack(message.to, message.from, message.messageID);
			// System.out.println(String.format("sending ack to %d, for ORDER msg %d",
			// my_ack.to, message.messageID));
			unicast_send_ack(message.from, my_ack);
		}
		// if the message is an ack, mark the ack array of the corresponding
		// message as true
		else if (message.isAck()) {
			// System.out.println(String.format("receiving ack for %d from %d",message.messageID,
			// message.from));
			ack[message.from][message.messageID] = true;
		}

		return message;
	}

	// unicast function for send ack
	public static void unicast_send_ack(int destID, Ack message)
			throws IOException {
		Random rand = new Random();
		// Delay in range [0, 2*mean delay]
		int randomDelay = rand.nextInt(2 * delayTime + 1);
		// Generate random number from 1 to 100
		// e.g. If drop rate = 10%, then a random number larger than 10 means
		// successfully send
		if (rand.nextInt(100) + 1 > dropRate) {
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
				Thread.sleep(randomDelay);
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

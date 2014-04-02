package connect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Random;

import message.Message;
import message.OrderMessage;

public class OrderMessageSend implements Runnable {

	private int messageID;
	private int i;
	private int S;

	public OrderMessageSend(int messageID, int i, int S) {
		this.messageID = messageID;
		this.i = i;
		this.S = S;
	}

	public void r_multicast_send(Message message) throws IOException {
		// this is the message i want to send
		synchronized (this) {
			b_multicast(message);
		}
	}

	public void b_multicast(Message message) throws IOException {
		// b-multicast to group
		for (int i = 0; i < Process.numProc; i++) {
			message.to = i;
			unicast_send(i, message);
		}
	}

	public void unicast_send(int destID, Message message) throws IOException {
		Random rand = new Random();
		// Delay in range [0, 2*mean delay]
		int randomDelay = rand.nextInt(2 * Process.delayTime + 1);
		// Generate random number from 1 to 100
		// e.g. If drop rate = 10%, then a random number larger than 10 means
		// successfully send

		DatagramChannel channel;
		channel = DatagramChannel.open();
		int destPort = 6000 + destID;
		try {
			if (rand.nextInt(100) + 1 > Process.dropRate) {
				InetSocketAddress destAddress = new InetSocketAddress(
						InetAddress.getByName(Process.IP), destPort);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(outputStream);
				os.writeObject(message);
				byte[] data = outputStream.toByteArray();
				ByteBuffer buffer = ByteBuffer.wrap(data);
			
				channel.connect(new InetSocketAddress(Process.IP, destPort));
				// randomized dalay
				Thread.sleep(randomDelay);
			
				int bytesend = channel.write(buffer);
				channel.disconnect();
//				System.out.println("send "+ bytesend + " bytes");
				channel.close();
				// Thread.sleep(2000);
			}
			// timeout for some time and then check if acknowledge has been
			// received
			Thread.sleep(3000);
			// if haven't received ack from the receiver, continue to send
			if (!Process.ack[destID][message.messageID]) {
//				System.out.println(String.format("resend msg %d to %d",
//						message.messageID, destID));
				unicast_send(destID, message);
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			OrderMessage om = new OrderMessage(0, 0, messageID);
			om.i = i;
			om.S = S;
			synchronized (this) {
				Process.messageID++;
			}
			// System.out.println("OM is made");
			r_multicast_send(om);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

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

import util.Configuration;
import message.Message;
import message.OrderMessage;

public class OrderMessageSend implements Runnable {

	public String content;

	public OrderMessageSend(String content) {
		this.content = content;
	}

	public void r_multicast_send(OrderMessage message) throws IOException {
		// this is the message i want to send
		synchronized (this) {
			b_multicast(message);
		}
	}

	public void b_multicast(OrderMessage message) throws IOException {
		// b-multicast to group
		for (int i = 0; i < Process.numProc; i++) {
			//System.out.println(String.format("sending to P%d, order=%d", i, ((OrderMessage) message).order));
			message.to = i;
			unicast_send(i, message);
		}
	}

	public void unicast_send(int destID, OrderMessage message) throws IOException {
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
				Configuration.getInstance();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(outputStream);
				os.writeObject(message);
				byte[] data = outputStream.toByteArray();
				ByteBuffer buffer = ByteBuffer.wrap(data);

				channel.connect( new InetSocketAddress(
						InetAddress.getByName(Configuration.IP[destID]), destPort));
				// randomized dalay
				Thread.sleep(randomDelay);

				int bytesend = channel.write(buffer);
				channel.disconnect();
				// System.out.println("send "+ bytesend + " bytes");
				channel.close();
				// Thread.sleep(2000);
			}
			// timeout for some time and then check if acknowledge has been
			// received
			Thread.sleep(3000);
			// if haven't received ack from the receiver, continue to send
			if (!Process.total_ack[destID][message.order]) {
				// System.out.println(String.format("resend msg %d to %d",
				// message.messageID, destID));
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
			// MessageID unused
			OrderMessage om = new OrderMessage(0, 0, 0);
			om.content = content;
			synchronized (this) {
				om.order = Process.order;
			}
			synchronized (this) {
				Process.order++;
			}
			r_multicast_send(om);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

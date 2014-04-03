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

import message.RegularMessage;

public class ProcessSendTotal implements Runnable {
	public ProcessSendTotal() {
		
	}

	public void unicast_send(int destID, RegularMessage message)
			throws IOException {
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
					channel.close();
					// Thread.sleep(2000);
				}
				//timeout for some time and then check if acknowledge has been received
				Thread.sleep(3000);
				//if haven't received ack from the receiver, continue to send
				if(!Process.ack[destID][message.messageID])
				{
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
		//if there are messages in the queue, send to sequencer e.g. P0
		while(true)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if(!Process.input_queue.isEmpty())
			{
				try {
					unicast_send(0, Process.input_queue.poll());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}

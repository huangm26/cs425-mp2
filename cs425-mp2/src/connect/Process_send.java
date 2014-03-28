package connect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import connect.Process;

public class Process_send implements Runnable{
	public Process_send()
	{
		
	}
	
	
	public void r_multicast_send(String message) throws IOException
	{
		
		//this is the message i want to send
		synchronized(this)
		{
			b_multicast(message);
			Process.send_msg.add(message);
		}
		
	}
	
	public void b_multicast(String message) throws IOException
	{
		//b-multicast to group
		for(int i = 0; i < Process.num_proc; i ++)
		{
			unicast_send(i,message);
		}
	}

	
	public void unicast_send(int destID, String message) throws IOException
	{
		DatagramChannel channel;
		channel = DatagramChannel.open();
		int destPort = 6000 + destID;
		try {
            InetSocketAddress destAddress = new InetSocketAddress(InetAddress.getByName("localhost"),destPort);
            ByteBuffer buffer =ByteBuffer.wrap(message.getBytes("UTF-8"));
            int bytesend = channel.send(buffer, destAddress);
            System.out.println("send "+ bytesend + " bytes");
            channel.close();
            Thread.sleep(2000);
 
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
		// TODO Auto-generated method stub

		String message = null;
		while(true)
		{
			/////
			//// input a message from stdio
			
			
			
			try {
				r_multicast_send(message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
}

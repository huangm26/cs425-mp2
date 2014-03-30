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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

import message.RegularMessage;
import connect.Process;

public class Process_send implements Runnable{
	public Process_send()
	{
		
	}
	
	
	public void r_multicast_send(RegularMessage message) throws IOException
	{
		
		//this is the message i want to send
		synchronized(this)
		{

			Process.send_msg.add(message.content);
			b_multicast(message);
		}
		
	}
	
	public void b_multicast(RegularMessage message) throws IOException
	{
		//b-multicast to group
		for(int i = 0; i < Process.num_proc; i ++)
		{
			message.to = i;
			unicast_send(i,message);
		}
	}

	
	public void unicast_send(int destID, RegularMessage message) throws IOException
	{
		Random rand = new Random();
		int rand_num = rand.nextInt(3);
//		if(rand_num == 0)
//		{
		DatagramChannel channel;
		channel = DatagramChannel.open();
		int destPort = 6000 + destID;
		try {
            InetSocketAddress destAddress = new InetSocketAddress(InetAddress.getByName("localhost"),destPort);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(outputStream);
            os.writeObject(message);
            byte[] data = outputStream.toByteArray();
            ByteBuffer buffer =ByteBuffer.wrap(data);
            channel.connect(new InetSocketAddress("localhost",destPort));
            int bytesend = channel.write(buffer);
            channel.disconnect();    
            //System.out.println("send "+ bytesend + " bytes");
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
//	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Let's chat!");
		while(true) {
		String content = null;
		Scanner scanner = new Scanner(System.in);
		content = scanner.nextLine();
//		message = "From "+ Process.ID + " mID";
		/*if(Process.ID == 0)
		{
			content = "1111111";
		}
		else if(Process.ID == 1)
		{
			content = "222222";
		}	else if (Process.ID == 2)
		{
			content = "333333";
		}	else if(Process.ID == 3)
		{
			content = "444444";
		}	else if(Process.ID == 4)
		{
			content = "555555";
		}	else
		{
			content = "666666";
		}*/
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		//get current date time with Date()
		Date date = new Date();
		content = content + " " + dateFormat.format(date);
		RegularMessage message = new RegularMessage(Process.ID, 0, Process.messageID, content);
		Process.messageID ++;
		
//		while(true)
//		{
			/////
			//// input a message from stdio
			
			

		try {
			Thread.sleep(30000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			r_multicast_send(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
//		}
		
	}
	
}


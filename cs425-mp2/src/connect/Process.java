package connect;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import javax.swing.Spring;

import message.Message;
import message.RegularMessage;


public class Process{
	static DatagramSocket Socket;
	public static int ID;		//use id to link to port
	public static int myPort;
	public static int messageID;
	public static DatagramChannel mychannel;
	public static int num_proc;	//number of processes
	public static ArrayList<String> send_msg;
	public static ArrayList<String> received;	
	public static boolean[][] ack;
	public static void main( String args[]) throws IOException, InterruptedException
	{
		messageID = 0;
		System.out.println("Enter the ID starting from 0 : ");
		Scanner scanner = new Scanner(System.in);
		ID = scanner.nextInt();
		ack = new boolean[num_proc][1000];
		
		num_proc = 6;
		send_msg = new ArrayList<String>();
		received = new ArrayList<String>();
		
		myPort = ID + 6000;				//define every process's port by the ID
//		myPort = 6000;
		mychannel = DatagramChannel.open();
		System.out.println(myPort);
		
//		mychannel.socket().bind(new InetSocketAddress(InetAddress.getByName("localhost"),myPort));
		mychannel.socket().bind(new InetSocketAddress("localhost",myPort));
		//set the channel to non-blocking
		mychannel.configureBlocking(false);
		Process_send send_thread = new Process_send();
		new Thread(send_thread).start();
		
		String mystr = null;
		while(true)
		{
			r_multicast_recv();
		}
		
	}
	
	
	
	public static void r_multicast_recv() throws IOException
	{
		Message recv_msg = null;

		for(int i = 0; i < num_proc; i++)
		{
			recv_msg = unicast_receive(i, recv_msg);
			if(recv_msg.isRegular())
			{
				if(!received.contains(((RegularMessage)recv_msg).content) && (recv_msg != null))
				{
					received.add(((RegularMessage)recv_msg).content);
					//check if the message received is originated by this process
					if(!send_msg.contains(((RegularMessage)recv_msg).content))
					{
						b_multicast((RegularMessage)recv_msg);
					}
					if(recv_msg != null)
					{
						System.out.println("Delivers " + ((RegularMessage)recv_msg).content);
					}
				}

			}
		}
	}
	
	public static void b_multicast(RegularMessage message) throws IOException
	{
		//b-multicast to group
		for(int i = 0; i < num_proc; i ++)
		{
			System.out.println("Send to "+ i);
			message.to = i;
			unicast_send(i,message);
		}
	}
	
	public static Message unicast_receive(int sourceID, Message message) throws IOException
	{
		
		int sourcePort = 6000 + sourceID;

		ByteBuffer buffer =ByteBuffer.allocate(1000);
//		mychannel.socket().connect(new InetSocketAddress("localhost",6000));


		while(mychannel.receive(buffer)==null){ 
		
			//listen to the connections from certain address
			
//			mychannel.receive(buffer);
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
//		message = Charset.forName("UTF-8").newDecoder().decode(buffer).toString();
//			System.out.println("receiving message " +Charset.forName("UTF-8").newDecoder().decode(buffer).toString());
//		mychannel.disconnect();
		
		//send ack
		return message;

	}
	
	public static void unicast_send(int destID, RegularMessage message) throws IOException
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
//		}
	}
}

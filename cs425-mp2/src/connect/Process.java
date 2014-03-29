package connect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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


public class Process{
	static DatagramSocket Socket;
	public static int ID;		//use id to link to port
	public static int myPort;
	public static DatagramChannel mychannel;
	public static int num_proc;	//number of processes
	public static ArrayList<String> send_msg;
	public static ArrayList<String> received;	public static void main( String args[]) throws IOException, InterruptedException
	{
		System.out.println("Enter the ID starting from 0 : ");
		Scanner scanner = new Scanner(System.in);
		ID = scanner.nextInt();
		
		Thread.sleep(5000);
		
		num_proc = 2;
		send_msg = new ArrayList<String>();
		received = new ArrayList<String>();
		
		myPort = ID + 6000;				//define every process's port by the ID
//		myPort = 6000;
		mychannel = DatagramChannel.open();
		System.out.println(myPort);
		mychannel.socket().bind(new InetSocketAddress(InetAddress.getByName("localhost"),myPort));
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
		String recv_msg = null;

		for(int i = 0; i < num_proc; i++)
		{
			recv_msg = unicast_receive(i, recv_msg);
			if(!received.contains(recv_msg))
			{
				received.add(recv_msg);
				//check if the message received is originated by this process
				if(!send_msg.contains(recv_msg))
				{
					b_multicast(recv_msg);
				}
				
				System.out.println("Delivers " + recv_msg);
			}

		}
	}
	
	public static void b_multicast(String message) throws IOException
	{
		//b-multicast to group
		for(int i = 0; i < num_proc; i ++)
		{
			unicast_send(i,message);
		}
	}
	
	public static String unicast_receive(int sourceID, String message) throws IOException
	{
		
		int sourcePort = 6000 + sourceID;

		ByteBuffer buffer =ByteBuffer.allocate(100);
		while(mychannel.receive(buffer)==null){ 
			//set the channel to non-blocking
			mychannel.configureBlocking(false);
		
			//listen to the connections from certain address
			mychannel.connect(new InetSocketAddress(InetAddress.getByName("localhost"),sourcePort));
			
			mychannel.receive(buffer);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		buffer.flip();
		message = Charset.forName("UTF-8").newDecoder().decode(buffer).toString();
//			System.out.println("receiving message " +Charset.forName("UTF-8").newDecoder().decode(buffer).toString());
		mychannel.disconnect();
		return message;

	}
	
	public static void unicast_send(int destID, String message) throws IOException
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
		
	}
}

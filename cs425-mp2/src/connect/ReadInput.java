package connect;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import message.RegularMessage;

public class ReadInput  implements Runnable{
	
	
	//this is the thread for reading input from stdin and put the inputted message into input_queue
	@Override
	public void run() {
		// TODO Auto-generated method stub

		System.out.println("Let's chat!");
		while(true) {
			String content = null;
			Scanner scanner = new Scanner(System.in);
			content = scanner.nextLine();
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			//get current date time with Date()
			Date date = new Date();
			content = content + " " + "MessageID " + Process.messageID;
			content = content + " " + dateFormat.format(date);
			RegularMessage message = new RegularMessage(Process.ID, 0, Process.messageID, content);
			Process.messageID ++;
			Lock lock = new ReentrantLock();
			lock.lock();
			for (int i = 0; i < Process.numProc; i++) {
				message.recent[i] = Process.recent[i];
			}
			lock.unlock();
			Process.input_queue.add(message);
		}
	}
}
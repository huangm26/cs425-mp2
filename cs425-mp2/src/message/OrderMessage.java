package message;

public class OrderMessage extends Message {

	public int i;
	public int S;
	
	public OrderMessage(int from, int to, int messageID) {
		super(from, to, messageID);
	}

}

package message;

public class OrderMessage extends Message {

	public String content;
	public int order;
	
	public OrderMessage(int from, int to, int messageID) {
		super(from, to, messageID);
	}

}

package message;

public class RegularMessage extends Message{

	public String content;
	public RegularMessage(int from, int to, int messageID, String content) {
		super(from, to, messageID);
		// TODO Auto-generated constructor stub
		this.content = content;
	}

}

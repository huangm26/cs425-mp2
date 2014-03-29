package message;

import java.io.Serializable;

public abstract class Message implements Serializable {
	
	public int from;
	public int to;
	public int messageID;
	
	public Message(int from, int to, int messageID) {
		this.to = to;
		this.from = from;
		this.messageID = messageID;
	}

	public boolean isAck() {
		return this instanceof Ack;
	}

	public boolean isRegular() {
		return this instanceof RegularMessage;
	}
}
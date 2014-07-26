package net.iizs.genius.server;

public abstract class AbstractResponse {
	private String msg;
	
	public AbstractResponse(String msg) {
		this.msg = msg;
	}

	public String getMessage() {
		return msg;
	}

	public void setMessage(String msg) {
		this.msg = msg;
	}
	
}

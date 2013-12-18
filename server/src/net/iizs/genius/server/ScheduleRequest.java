package net.iizs.genius.server;

public class ScheduleRequest {
	private String command;
	private long delay;
	
	public ScheduleRequest(String command, long delay) {
		this.command = command;
		this.delay = delay;
	}

	public String getCommand() {
		return command;
	}

	public long getDelay() {
		return delay;
	}

}

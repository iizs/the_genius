package net.iizs.genius.server;

import io.netty.channel.Channel;

public class Player {
	private String nickname_;
	private Channel channel_;
	
	public Player(String n, Channel c) {
		nickname_ = n;
		channel_ = c;
	}
	
	public Player(String n) {
		// bot player
		nickname_ = n;
		channel_ = null;
	}
	
	public boolean isBot() {
		return ( channel_ == null );
	}
	
	public void becomeBot() {
		channel_ = null;
	}
	
	public Channel getChannel() {
		return channel_;
	}
	
	public String getNickname() {
		return nickname_;
	}

}

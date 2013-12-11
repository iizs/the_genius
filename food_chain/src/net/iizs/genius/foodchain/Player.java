package net.iizs.genius.foodchain;

import io.netty.channel.Channel;

public class Player {
	private String nickname;
	private Channel channel;
	
	public Player(String n, Channel c) {
		nickname = n;
		channel = c;
	}
	
	public Player(String n) {
		// bot player
		nickname = n;
		channel = null;
	}
	
	public boolean isBot() {
		return ( channel == null );
	}
	
	public void becomeBot() {
		channel = null;
	}
	
	public Channel getChannel() {
		return channel;
	}

}

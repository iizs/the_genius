package net.iizs.genius.foodchain;

import io.netty.channel.Channel;

public class Player {
	private String nickname_;
	private Channel channel_;
	private Character character_;
	
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
	
	public void setCharacter(Character c) {
		character_ = c;
	}
	
	public Character getCharacter() {
		return character_;
	}

}

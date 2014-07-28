package net.iizs.genius.server;

import io.netty.channel.Channel;

public class Player {
	private String id_;
	private String nickname_;
	private Channel channel_;
	
	// 인자 없이 생성되는 것을 막기 위한 private 생성자.
	private Player() {
	
	}

	
	public Player(String id, String n, Channel c) {
		id_ = id;
		nickname_ = n;
		channel_ = c;
	}
	
	public Player(String id) {
		// bot player
		id_ = id;
		nickname_ = id;
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

	public void setChannel(Channel c) {
		channel_ = c;
	}
	
	public String getNickname() {
		return nickname_;
	}
	
	public void setNickname(String n) {
		nickname_ = n;
	}


	public String getId() {
		return id_;
	}


	public void setId(String id) {
		this.id_ = id;
	}


	@Override
	public String toString() {
		return "Player [" + nickname_ + "(" + id_ + ")]";
	}

	
}

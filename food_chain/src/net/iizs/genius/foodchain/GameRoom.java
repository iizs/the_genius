package net.iizs.genius.foodchain;

import io.netty.channel.ChannelHandlerContext;

public class GameRoom {
	private AbstractGameRoomState state_;

 	
	public GameRoom() {
		state_ = new WaitingState();
	}
	
	public void setName(String n) {
		state_.setName(n);
	}
	
	public String getName() {
		return state_.getName();
	}
	
	public void join(String nickname, ChannelHandlerContext ctx) throws Exception {
		state_.join(nickname, ctx);
	}
	
	public void broadcast(String msg) {
		state_.broadcast(msg);
	}
	
	public void chat(String nickname, String msg) {
		state_.chat(nickname, msg);
	}
	
	public void quit(String nickname) throws Exception {
		state_.quit(nickname);
	}
	
	public void userCommand(String nickname, String req) throws Exception {
    	state_.userCommand(nickname, req);
	}

	public void printUsageSimple(String nickname) throws Exception {
		state_.printUsageSimple(nickname);
	}
	


}

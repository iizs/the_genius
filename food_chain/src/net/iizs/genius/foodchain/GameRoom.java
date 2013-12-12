package net.iizs.genius.foodchain;

import io.netty.channel.ChannelHandlerContext;

public class GameRoom {
	private AbstractGameRoomState state;

 	
	public GameRoom() {
		state = new WaitingState();
	}
	
	public void setName(String n) {
		state.setName(n);
	}
	
	public void join(String nickname, ChannelHandlerContext ctx) throws Exception {
		state.join(nickname, ctx);
	}
	
	public void broadcast(String msg) {
		state.broadcast(msg);
	}
	
	public void chat(String nickname, String msg) {
		state.chat(nickname, msg);
	}
	
	public void whisper(String nickname, String to, String msg) throws Exception {
		state.whisper(nickname, to, msg);
	}
	
	public void quit(String nickname) throws Exception {
		state.quit(nickname);
	}
	
	public void userCommand(String nickname, String req) throws Exception {
    	state.userCommand(nickname, req);
	}

	public void printUsageSimple(String nickname) throws Exception {
		state.printUsageSimple(nickname);
	}
	


}

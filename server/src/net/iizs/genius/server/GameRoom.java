package net.iizs.genius.server;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.iizs.genius.server.foodchain.FoodChainWaitState;
import io.netty.channel.ChannelHandlerContext;

public class GameRoom implements Comparable<GameRoom> {
	private GeniusServerHandler server_;
	private AbstractGameRoomState state_;

 	
	public GameRoom(GeniusServerHandler server) {
		server_ = server;
		state_ = new FoodChainWaitState();
	}
	
	public void setName(String n) {
		state_.setName(n);
	}
	
	public String getName() {
		return state_.getName();
	}
	
	public String getGamePackageName() {
		return state_.getClass().getPackage().getName();
	}
	
	public void join(String nickname, ChannelHandlerContext ctx) throws Exception {
		state_.join(nickname, ctx);
	}
	
	public void broadcast(String msg) {
		state_.broadcast(msg);
	}
	
	public void chat(String nickname, String msg) throws Exception {
		state_.chat(nickname, msg);
	}
	
	public void quit(String nickname) throws Exception {
		state_.quit(nickname);
	}
	
	public void userCommand(String nickname, String req) throws Exception {
    	state_ = state_.userCommand(nickname, req);
	}

	public void printUsageSimple(String nickname) throws Exception {
		state_.printUsageSimple(nickname);
	}
	
	public ConcurrentLinkedQueue<ScheduleRequest> getJobQueue() {
		return state_.getJobQueue();
	}

	@Override
	public String toString() {
		return "[" + getName() + "] " + server_.getMessage( getGamePackageName() );
	}

	@Override
	public int compareTo(GameRoom o) {
		return getName().compareTo(o.getName());
	}


}

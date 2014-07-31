package net.iizs.genius.server;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.iizs.genius.server.foodchain.FoodChainWaitState;

public class GameRoom implements Comparable<GameRoom> {
	private GeniusServerHandler server_;
	private AbstractGameRoomState state_;

	private GameRoom(GeniusServerHandler server, AbstractGameRoomState state) {
		server_ = server;
		state_ = state;
	}
	
	public void setName(String n) {
		state_.setName(n);
	}
	
	public String getName() {
		return state_.getName();
	}
	
	public String getGameId() {
		return state_.getClass().getPackage().getName().substring( 
				this.getClass().getPackage().getName().length() + 1 );
	}
	
	public void join(Player p) throws Exception {
		state_.join(p);
	}
	
	public void broadcast(String msg) {
		state_.broadcast(msg);
	}
	
	public void chat(Player p, String msg) throws Exception {
		state_.chat(p, msg);
	}
	
	public void quit(Player p) throws Exception {
		state_.quit(p);
	}
	
	public void printUsage(Player p) throws Exception {
		state_.printUsage(p);
	}
	
	public void userCommand(Player p, String[] cmds) throws Exception {
    	state_ = state_.userCommand(p, cmds);
	}

	public ConcurrentLinkedQueue<ScheduleRequest> getJobQueue() {
		return state_.getJobQueue();
	}

	@Override
	public String toString() {
		return "[" + getName() + "] " + server_.getMessage( getGameId() );
	}

	@Override
	public int compareTo(GameRoom o) {
		return getName().compareTo(o.getName());
	}

	public static GameRoom getInstance(GeniusServerHandler server, String gameid) throws GeniusServerException {
		if ( gameid.equals( "foodchain" ) ) {
			return new GameRoom(server, new FoodChainWaitState(server));
		}
		
		throw new NotSupportedGameException( server.getMessage( "eInvalidGameId", gameid ) );
	}

}

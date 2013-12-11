package net.iizs.genius.foodchain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import static net.iizs.genius.foodchain.Constants.*;

public class GameRoom {
	private GameRoomState state;
	private ChannelGroup cgAllPlayers;
	private ConcurrentMap<String, Player> players;
	private String name;
 	
	public GameRoom() {
		state = new WaitingState(this);
		cgAllPlayers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		players = new ConcurrentHashMap<>();
	}
	
	public void setName(String n) {
		name = n;
	}
	
	public void join(String nickname, ChannelHandlerContext ctx) throws Exception {
		if ( ! ( state instanceof WaitingState ) ) {
			throw new GeniusServerException( name + "번 게임방에 들어갈 수 없습니다; 게임이 진행중입니다." );
		}
		
		Player p = new Player( nickname, ctx.channel() );
		if ( players.putIfAbsent( nickname, p ) != null ) {
			throw new GeniusServerException( name + "번 게임방에 들어갈 수 없습니다; 같은 이름의 플레이어가 존재합니다." );
		}
		cgAllPlayers.add( ctx.channel() );
		broadcast( "[" + nickname + "]님이 들어왔습니다." );		
	}
	
	public void broadcast(String msg) {
        for (Channel c: cgAllPlayers) {
        	c.writeAndFlush( "===== " + msg + " =====" + NEWLINE);
        }
	}
	
	public void chat(String nickname, String msg) {
        for (Channel c: cgAllPlayers) {
        	c.writeAndFlush("[" + nickname + "] " + msg + NEWLINE);
        }
	}
	
	public void whisper(String nickname, String to, String msg) {
		// TODO impl
	}
	
	public Player getPlayer(String nickname) throws Exception {
		Player p = players.get(nickname);
		if ( p == null ) {
			throw new GeniusServerException("[" + nickname + "]님은 존재하지 않습니다.");
		}
		return p;
	}
	
	public void quit(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		cgAllPlayers.remove( p.getChannel() );
		broadcast("[" + nickname + "]님이 나갔습니다.");
		
		if ( state instanceof WaitingState ) {
			// 깨끗하게 퇴장
			players.remove( nickname );
		} else {
			// bot 으로 변경
			p.becomeBot();
			broadcast("[" + nickname + "]님을 대신해서 봇이 게임을 진행합니다.");
		}
	}
	
	public void userCommand(String nickname, String req) throws Exception {
    	String cmds[] = req.split("\\s+");
    	String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/quit") ) {
    		quit(nickname);
    		throw new QuitGameRoomException();
    	} else if ( cmd.equals("/start") ) {
    		//TODO impl
    		throw new GeniusServerException("Not implemented yet");
    	} else if ( cmd.equals("/add_bot") ) {
    		//TODO impl
    		throw new GeniusServerException("Not implemented yet");
    	} else {
    		state.userCommand(nickname, cmds);
    	}
	}
	
	public void printUsageSimple(String nickname) throws Exception {
		state.printUsageSimple(nickname);
	}
	

}

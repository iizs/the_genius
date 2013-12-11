package net.iizs.genius.foodchain;

import java.util.Iterator;
import java.util.Set;
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
	
	public void whisper(String nickname, String to, String msg) throws Exception {
		Player p = getPlayer(to);
		
		p.getChannel().writeAndFlush(">>> [" + nickname + "]님의 귓속말: " + msg + NEWLINE);
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
    	String cmds[] = req.split("\\s+", 3);
    	String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/quit") ) {
    		quit(nickname);
    		throw new QuitGameRoomException();
    	} else if ( cmd.equals("/to") ) {
    		whisper(nickname, cmds[1], cmds[2]);
    	} else if ( cmd.equals("/info") ) {
    		showInfo( nickname );
    	} else if ( cmd.equals("/start") ) {
    		//TODO impl
    		throw new GeniusServerException("Not implemented yet");
    	} else if ( cmd.equals("/add_bot") ) {    		
    		String botName;
    		try {
    			botName = cmds[1];
    		} catch ( ArrayIndexOutOfBoundsException e ) {
    			long appendix = ( System.currentTimeMillis() % ONE_DAY_MILLI ) / 1000;
    			botName = nickname + "_" + Long.toString(appendix); 
    		}    		
    		addBot( botName );
    	} else {
    		state.userCommand(nickname, cmds);
    	}
	}
	
	private void addBot(String botName) throws Exception {
		Player p = new Player( botName );
		if ( players.putIfAbsent( botName, p ) != null ) {
			throw new GeniusServerException( "봇을 생성할 수 없습니다; 같은 이름의 플레이어가 존재합니다." );
		}
		broadcast("봇 [" + botName + "]이 생성되었습니다.");
	}
	
	public void printUsageSimple(String nickname) throws Exception {
		state.printUsageSimple(nickname);
	}
	
	public void showInfo(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		p.getChannel().write( "> 방 번호: " + name + NEWLINE );
		p.getChannel().write( "> 플레이어" + NEWLINE );
		
    	Set<String> playerNames = players.keySet();
    	Iterator<String> iter = playerNames.iterator();
    	while ( iter.hasNext() ) {    		
    		Player i = getPlayer(iter.next());
    		
    		p.getChannel().write("> [" + i.getNickname() + "]");
    		if ( i.isBot() ) {
    			p.getChannel().write( " (Bot)");
    		}
    		p.getChannel().write( NEWLINE );
    	}
    	
    	p.getChannel().flush();
	}

}

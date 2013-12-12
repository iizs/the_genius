package net.iizs.genius.foodchain;

import static net.iizs.genius.foodchain.Constants.*;

import java.util.Iterator;
import java.util.Set;

import io.netty.channel.ChannelHandlerContext;

public class WaitingState extends AbstractGameRoomState {

	public void userCommand(String nickname, String[] cmds) throws Exception {
		printUsageSimple(nickname);
	}
	
	public void printUsageSimple(String nickname) throws Exception {
		getPlayer(nickname).getChannel().writeAndFlush(ROOM_USAGE_SIMPLE + NEWLINE);
	}
	
	public void join(String nickname, ChannelHandlerContext ctx) throws Exception {		
		Player p = new Player( nickname, ctx.channel() );
		if ( players_.putIfAbsent( nickname, p ) != null ) {
			throw new GeniusServerException( name_ + "번 게임방에 들어갈 수 없습니다; 같은 이름의 플레이어가 존재합니다." );
		}
		cgAllPlayers_.add( ctx.channel() );
		broadcast( "[" + nickname + "]님이 들어왔습니다." );		
	}
	
	public void quit(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		cgAllPlayers_.remove( p.getChannel() );
		broadcast("[" + nickname + "]님이 나갔습니다.");
	
		// 깨끗하게 퇴장
		players_.remove( nickname );
	}
	
	private void addBot(String botName) throws Exception {
		Player p = new Player( botName );
		if ( players_.putIfAbsent( botName, p ) != null ) {
			throw new GeniusServerException( "봇을 생성할 수 없습니다; 같은 이름의 플레이어가 존재합니다." );
		}
		broadcast("봇 [" + botName + "]이 생성되었습니다.");
	}
	
	private void removeBot() throws Exception {
		for ( Player p: players_.values() ) {
			if ( p.isBot() ) {
				players_.remove(p.getNickname());
				broadcast("봇 [" + p.getNickname() + "]이 제거되었습니다.");
				break;
			}
		}
	}
	
	public void showInfo(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		p.getChannel().write( "> 방 번호: " + name_ + NEWLINE );
		p.getChannel().write( "> 플레이어" + NEWLINE );
		
    	Set<String> playerNames = players_.keySet();
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
	
	public AbstractGameRoomState userCommand(String nickname, String req) throws Exception {
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
    		if ( players_.size() < Character.values().length ) {
    			throw new GeniusServerException("참가자가 부족합니다. 봇을 추가하거나, 다른 플레이어의 입장을 기다려주세요.");
    		}
    		return new InitState(this);
    	} else if ( cmd.equals("/add_bot") ) {    		
    		String botName;
    		try {
    			botName = cmds[1];
    		} catch ( ArrayIndexOutOfBoundsException e ) {
    			long appendix = ( System.currentTimeMillis() % ONE_DAY_MILLI ) / 1000;
    			botName = nickname + "_" + Long.toString(appendix); 
    		}    		
    		addBot( botName );
    	} else if ( cmd.equals("/del_bot") ) {
    		removeBot();
    	} else {
    		printUsageSimple(nickname);
    	}
    	
    	return this;
	}
	

}

package net.iizs.genius.server.foodchain;

import static net.iizs.genius.server.foodchain.FoodChainConstants.*;
import static net.iizs.genius.server.Constants.NEWLINE;

import java.util.Iterator;
import java.util.Set;

import net.iizs.genius.server.GeniusServerException;
import net.iizs.genius.server.NoBotFoundException;
import net.iizs.genius.server.Player;
import net.iizs.genius.server.QuitGameRoomException;
import io.netty.channel.ChannelHandlerContext;

public class FoodChainWaitState extends AbstractFoodChainState {
	
	public FoodChainWaitState() {
		super();
	}

	public FoodChainWaitState(AbstractFoodChainState c) {
		super(c);
	}

	public void printUsageSimple(String nickname) throws Exception {
		getPlayer(nickname).getChannel().writeAndFlush(WAIT_USAGE_SIMPLE + NEWLINE);
	}
	
	public void join(String nickname, ChannelHandlerContext ctx) throws Exception {		
		FoodChainPlayer p = new FoodChainPlayer( nickname, ctx.channel() );
		if ( getAllPlayers().putIfAbsent( nickname, p ) != null ) {
			throw new GeniusServerException( getName() + "번 게임방에 들어갈 수 없습니다; 같은 이름의 플레이어가 존재합니다." );
		}
		getAllPlayersChannelGroup().add( ctx.channel() );
		broadcast( "[" + nickname + "]님이 들어왔습니다." );		
	}
	
	public void quit(String nickname) throws Exception {
		FoodChainPlayer p = getFoodChainPlayer(nickname);
		
		getAllPlayersChannelGroup().remove( p.getChannel() );
		broadcast("[" + nickname + "]님이 나갔습니다.");
	
		// 깨끗하게 퇴장
		getAllPlayers().remove( nickname );
	}
	
	private void addBot(String botName) throws Exception {
		FoodChainPlayer p = new FoodChainPlayer( botName );
		if ( getAllPlayers().putIfAbsent( botName, p ) != null ) {
			throw new GeniusServerException( "봇을 생성할 수 없습니다; 같은 이름의 플레이어가 존재합니다." );
		}
		broadcast("봇 [" + botName + "]이 생성되었습니다.");
	}
	
	private void removeBot() throws Exception {
		for ( Player p: getAllPlayers().values() ) {
			if ( p.isBot() ) {
				getAllPlayers().remove(p.getNickname());
				broadcast("봇 [" + p.getNickname() + "]이 제거되었습니다.");
				return;
			}
		}
		throw new NoBotFoundException("더 이상 제거할 봇이 없습니다.");
	}
	
	public void showInfo(String nickname) throws Exception {
		FoodChainPlayer p = getFoodChainPlayer(nickname);
		
		p.getChannel().write( "> 방 번호: " + getName() + NEWLINE );
		p.getChannel().write( "> 플레이어" + NEWLINE );
		
    	Set<String> playerNames = getAllPlayers().keySet();
    	Iterator<String> iter = playerNames.iterator();
    	while ( iter.hasNext() ) {    		
    		FoodChainPlayer i = getFoodChainPlayer(iter.next());
    		
    		p.getChannel().write("> [" + i.getNickname() + "]");
    		if ( i.isBot() ) {
    			p.getChannel().write( " (Bot)");
    		}
    		p.getChannel().write( NEWLINE );
    	}
    	
    	p.getChannel().flush();
	}
	
	public synchronized AbstractFoodChainState userCommand(String nickname, String req) throws Exception {
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
    		if ( getAllPlayers().size() < FoodChainCharacter.values().length ) {
    			throw new GeniusServerException("플레이어가 부족합니다. 봇을 추가하거나, 다른 플레이어의 입장을 기다려주세요.");
    		}
    		
    		while ( getAllPlayers().size() > FoodChainCharacter.values().length ) {
    			try {
    				removeBot();
    			} catch ( NoBotFoundException e ) {
    				throw new NoBotFoundException("플레이어가 너무 많습니다.");
    			}
    		}
    		
    		return new FoodChainInitState(this);
    	} else if ( cmd.equals("/add_bot") || cmd.equals("/add") ) {    		
    		String botName;
    		try {
    			botName = cmds[1];
    		} catch ( ArrayIndexOutOfBoundsException e ) {
    			long appendix = ( System.currentTimeMillis() % ONE_DAY_MILLI ) / 1000;
    			botName = nickname + "_" + Long.toString(appendix); 
    		}    		
    		addBot( botName );
    	} else if ( cmd.equals("/del_bot") || cmd.equals("/del") ) {
    		removeBot();
    	} else {
    		printUsageSimple(nickname);
    	}
    	
    	return this;
	}
	

}

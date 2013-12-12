package net.iizs.genius.foodchain;

import static net.iizs.genius.foodchain.Constants.NEWLINE;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractGameRoomState {
	protected ChannelGroup cgAllPlayers;
	protected ConcurrentMap<String, Player> players;
	protected String name;
	
	public AbstractGameRoomState() {
		name = "";
		cgAllPlayers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		players = new ConcurrentHashMap<>();
	}
	
	public AbstractGameRoomState(AbstractGameRoomState c) {
		name = c.name;
		cgAllPlayers = c.cgAllPlayers;
		players = c.players;
	}
	
	protected Player getPlayer(String nickname) throws Exception {
		Player p = players.get(nickname);
		if ( p == null ) {
			throw new GeniusServerException("[" + nickname + "]님은 존재하지 않습니다.");
		}
		return p;
	}
	
	public void setName(String n) {
		name = n;
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
		Player me = getPlayer(nickname);
		
		if ( p.isBot() ) {
			throw new GeniusServerException("[" + to + "]님은 봇입니다.");
		}
		
		p.getChannel().writeAndFlush(">>> [" + nickname + "]님의 귓속말: " + msg + NEWLINE);
		me.getChannel().writeAndFlush( ">>> [" + to + "]님께 귓속말을 보냈습니다." + NEWLINE);
	}
	
	public void join(String nickname, ChannelHandlerContext ctx) throws Exception {
		throw new GeniusServerException( name + "번 게임방에 들어갈 수 없습니다; 게임이 진행중입니다." );
	}
	
	public void quit(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		cgAllPlayers.remove( p.getChannel() );
		broadcast("[" + nickname + "]님이 나갔습니다.");
		
		p.becomeBot();
		broadcast("[" + nickname + "]님을 대신해서 봇이 게임을 진행합니다.");
	}
	
	public abstract void userCommand(String nickname, String req) throws Exception;
	public abstract void printUsageSimple(String nickname) throws Exception;
	public abstract void showInfo(String nickname) throws Exception;
}

package net.iizs.genius.server;

import static net.iizs.genius.server.Constants.NEWLINE;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import net.iizs.genius.server.GeniusServerException;
import net.iizs.genius.server.ScheduleRequest;
import net.iizs.genius.server.Util;


public abstract class AbstractGameRoomState {
	private ChannelGroup cgAllPlayers_;
	private ConcurrentMap<String, Player> players_;
	private String name_;
	private String adminPassword_;
	private ConcurrentLinkedQueue<ScheduleRequest> jobQueue_;
	
	public AbstractGameRoomState() {
		name_ = "";
		cgAllPlayers_ = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		players_ = new ConcurrentHashMap<String, Player>();
		adminPassword_ = Util.generatePassword();
		jobQueue_ = new ConcurrentLinkedQueue<ScheduleRequest>();
	}
	
	public AbstractGameRoomState(AbstractGameRoomState c) {
		name_ = c.name_;
		cgAllPlayers_ = c.cgAllPlayers_;
		players_ = c.players_;
		adminPassword_ = c.adminPassword_;
		jobQueue_ = c.jobQueue_;
	}
	
	protected Player getPlayer(String nickname) throws Exception {
		Player p = players_.get(nickname);
		if ( p == null ) {
			throw new GeniusServerException("[" + nickname + "]님은 존재하지 않습니다.");
		}
		return p;
	}
	
	public void setName(String n) {
		name_ = n;
	}

	public String getName() {
		return name_;
	}
	
	public String getAdminPassword() {
		return adminPassword_;
	}
	
	public void broadcast(String msg) {
        for (Channel c: cgAllPlayers_) {
        	c.writeAndFlush( "===== " + msg + NEWLINE);
        }
	}
	
	public void chat(String nickname, String msg) throws Exception {
        for (Channel c: cgAllPlayers_) {
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

	public ConcurrentLinkedQueue<ScheduleRequest> getJobQueue() {
		return jobQueue_;
	}
	
	public abstract void quit(String nickname) throws Exception;
	public abstract void join(String nickname, ChannelHandlerContext ctx) throws Exception;
	public abstract AbstractGameRoomState userCommand(String nickname, String req) throws Exception;
	public abstract void printUsageSimple(String nickname) throws Exception;
	public abstract void showInfo(String nickname) throws Exception;
	
	protected ChannelGroup getAllPlayersChannelGroup() {
		return cgAllPlayers_;
	}
	
	protected ConcurrentMap<String, Player> getAllPlayers() {
		return players_;
	}
	
}

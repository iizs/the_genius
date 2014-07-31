package net.iizs.genius.server;

import io.netty.channel.Channel;
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
	private GeniusServerHandler server_;
	
	public AbstractGameRoomState(GeniusServerHandler server) {
		name_ = "";
		cgAllPlayers_ = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		players_ = new ConcurrentHashMap<String, Player>();
		adminPassword_ = Util.generatePassword();
		jobQueue_ = new ConcurrentLinkedQueue<ScheduleRequest>();
		server_ = server;
	}
	
	public AbstractGameRoomState(AbstractGameRoomState c) {
		name_ = c.name_;
		cgAllPlayers_ = c.cgAllPlayers_;
		players_ = c.players_;
		adminPassword_ = c.adminPassword_;
		jobQueue_ = c.jobQueue_;
		server_ = c.server_;
	}
	
	protected GeniusServerHandler getServer() {
		return server_;
	}
	
	protected Player getPlayer(String id) throws Exception {
		Player p = players_.get(id);
		if ( p == null ) {
			throw new GeniusServerException( server_.getMessage("eUserNotFound", id ) );
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
        	c.writeAndFlush( server_.getFormatter().formatGameRoomMessage(msg) );
        }
	}
	
	public void chat(Player p, String msg) throws Exception {
        for (Channel c: cgAllPlayers_) {
        	c.writeAndFlush( server_.getFormatter().formatChatMessage(p.getId(), msg) );
        }
	}
	
	public void whisper(Player p, String to, String msg) throws Exception {
		Player toPlayer = getPlayer(to);
		//Player me = getPlayer(nickname);
		
		if ( ! toPlayer.isBot() ) {
			toPlayer.getChannel().writeAndFlush( server_.getFormatter().formatWhisperMessage(p.getId(), msg ) );
		}
		
		p.getChannel().writeAndFlush( 
				server_.getFormatter().formatResponseMessage( 
						new SimpleResponse( server_.getMessage( "sentWhisper", to ) ) ) );
	}

	public ConcurrentLinkedQueue<ScheduleRequest> getJobQueue() {
		return jobQueue_;
	}
	
	public abstract void quit(Player p) throws Exception;
	public abstract void join(Player p) throws Exception;
	public abstract void surrender(Player p) throws Exception;
	public abstract void seat(Player p) throws Exception;
	public abstract void stand(Player p) throws Exception;
	public abstract AbstractGameRoomState userCommand(Player p, String[] cmds) throws Exception;
	public abstract void printUsage(Player p) throws Exception;
	//public abstract void showInfo(Player p) throws Exception;
	
	protected ChannelGroup getAllPlayersChannelGroup() {
		return cgAllPlayers_;
	}
	
	protected ConcurrentMap<String, Player> getAllPlayers() {
		return players_;
	}
	
}

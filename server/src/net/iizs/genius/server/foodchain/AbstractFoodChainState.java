package net.iizs.genius.server.foodchain;

import static net.iizs.genius.server.foodchain.Constants.NEWLINE;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import net.iizs.genius.server.GeniusServerException;
import net.iizs.genius.server.ScheduleRequest;
import net.iizs.genius.server.Util;

public abstract class AbstractFoodChainState {
	protected ChannelGroup cgAllPlayers_;
	protected ConcurrentMap<String, Player> players_;
	protected String name_;
	protected String adminPassword_;
	protected ConcurrentLinkedQueue<ScheduleRequest> jobQueue_;
	
	protected int round_;
	protected int kills_;
	protected Map<Area,List<Player>> minimap_;
	protected Set<Player> herbivores_;
	protected Map<Character,Player> charmap_;
	
	public AbstractFoodChainState() {
		name_ = "";
		cgAllPlayers_ = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		players_ = new ConcurrentHashMap<String, Player>();
		adminPassword_ = Util.generatePassword();
		jobQueue_ = new ConcurrentLinkedQueue<ScheduleRequest>();
	}
	
	public AbstractFoodChainState(AbstractFoodChainState c) {
		name_ = c.name_;
		cgAllPlayers_ = c.cgAllPlayers_;
		players_ = c.players_;
		round_ = c.round_;
		minimap_ = c.minimap_;
		adminPassword_ = c.adminPassword_;
		jobQueue_ = c.jobQueue_;
		herbivores_ = c.herbivores_;
		charmap_ = c.charmap_;
		kills_ = c.kills_;
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
	
	public void join(String nickname, ChannelHandlerContext ctx) throws Exception {
		throw new GeniusServerException( name_ + "번 게임방에 들어갈 수 없습니다; 게임이 진행중입니다." );
	}
	
	public void quit(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		cgAllPlayers_.remove( p.getChannel() );
		broadcast("[" + nickname + "]님이 나갔습니다.");
		
		p.becomeBot();
		broadcast("[" + nickname + "]님을 대신해서 봇이 게임을 진행합니다.");
	}

	public ConcurrentLinkedQueue<ScheduleRequest> getJobQueue() {
		return jobQueue_;
	}
	
	public abstract AbstractFoodChainState userCommand(String nickname, String req) throws Exception;
	public abstract void printUsageSimple(String nickname) throws Exception;
	public abstract void showInfo(String nickname) throws Exception;
}

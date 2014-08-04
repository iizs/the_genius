package net.iizs.genius.server;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.UnsupportedEncodingException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

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
	private ResourceBundle messages_;
	
	public AbstractGameRoomState(GeniusServerHandler server) {
		name_ = "";
		cgAllPlayers_ = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		players_ = new ConcurrentHashMap<String, Player>();
		adminPassword_ = Util.generatePassword();
		jobQueue_ = new ConcurrentLinkedQueue<ScheduleRequest>();
		server_ = server;
		messages_ = null;
	}
	
	public AbstractGameRoomState(GeniusServerHandler server, String mBundleKey) {
		this(server);
		setMessageBundle(mBundleKey);
	}
	
	public AbstractGameRoomState(AbstractGameRoomState c) {
		name_ = c.name_;
		cgAllPlayers_ = c.cgAllPlayers_;
		players_ = c.players_;
		adminPassword_ = c.adminPassword_;
		jobQueue_ = c.jobQueue_;
		server_ = c.server_;
		messages_ = c.messages_;
	}
	
	private GeniusServerHandler getServer() {
		return server_;
	}
	
	protected Player getPlayer(String id) throws Exception {
		Player p = players_.get(id);
		if ( p == null ) {
			throw new GeniusServerException( server_.getMessage("eUserNotFound", id ) );
		}
		return p;
	}
	
	public void setMessageBundle(String key) {
		messages_ = ResourceBundle.getBundle(key);
	}
	
    public String getMessage(String key) {
    	if ( messages_ == null ) {
    		return getServer().getMessage(key);
    	}
    	
    	try {
			return new String( messages_.getString( key ).getBytes("ISO-8859-1"), "UTF-8" );
		} catch (UnsupportedEncodingException e) {
			getServer().getLogger().warning( e.getMessage() );
			return messages_.getString( key );
		} catch (MissingResourceException e) {
			return getServer().getMessage(key);
		}
    }
    
    public String getMessage(String key, Object ... args ) {
    	if ( messages_ == null ) {
    		return getServer().getMessage(key, args);
    	}
    	
    	try {
	    	String s = new String( messages_.getString( key ).getBytes("ISO-8859-1"), "UTF-8" );
	    	return String.format(s, args);
    	} catch (UnsupportedEncodingException e) {
    		getServer().getLogger().warning( e.getMessage() );
			return messages_.getString( key );
    	} catch (MissingResourceException e) {
    		return getServer().getMessage(key, args);
		}
    }
    
    public void backToLobby(Player p) {
    	getServer().enterLobby(p);
    }
    
    public Logger getLogger() {
    	return getServer().getLogger();
    }
    
    public ServerMessageFormatter getFormatter() {
    	return getServer().getFormatter();
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
		
		if ( toPlayer == null ) {
			throw new GeniusServerException( getMessage("eUserNotFound", to) );
		}
		
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
	public abstract void printUsage(Player p) throws Exception;
	public abstract void showInfo(Player p) throws Exception;
	
	public AbstractGameRoomState userCommand(Player p, String[] cmds) throws Exception {
		String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/to") ) {
    		whisper(p, cmds[1], cmds[2]);
    	} else if ( cmd.equals("/info") ) {
    		showInfo( p );
    	} else if ( cmd.equals("/seat") ) {
    		seat( p );
    	} else if ( cmd.equals("/quit") || cmd.equals("/leave") ) {
    		quit(p);
    		backToLobby(p);
    	} else if ( cmd.equals("/surrender") || cmd.equals("/gg") ) {
    		surrender(p);
    	} else if ( cmd.equals("/stand") || cmd.equals("/watch") ) {
    		stand(p);
    	}	
    	
    	return this;
	}
	
	protected ChannelGroup getAllPlayersChannelGroup() {
		return cgAllPlayers_;
	}
	
	protected ConcurrentMap<String, Player> getAllPlayers() {
		return players_;
	}
	
}

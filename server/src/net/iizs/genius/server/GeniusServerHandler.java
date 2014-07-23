package net.iizs.genius.server;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import static net.iizs.genius.server.Constants.*;

public class GeniusServerHandler extends SimpleChannelInboundHandler<String> {

	static final ChannelGroup cgAllUsers_ = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	static final ChannelGroup cgLobby_ = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	static final ConcurrentMap<String, GameRoom> allGameRooms_ = new ConcurrentHashMap<String, GameRoom>();
	static final Timer jobScheduler = new Timer();
	
	GameRoom myGame_ = null;
	String nickname_;
	ResourceBundle messages_;
	
    private static final Logger logger_ = Logger.getLogger(GeniusServerHandler.class.getName());

    private class ScheduledJob extends TimerTask {
    	private GameRoom room_;
    	private String cmd_;
    	private String nick_;
    	
    	public ScheduledJob(GameRoom r, String n, String c) {
    		room_ = r;
    		nick_ = n;
    		cmd_ = c;
    	}

		@Override
		public void run() {
			try {
				logger_.info("[" + nick_ + "](admin)@" + room_.getName() + " " + cmd_);
				room_.userCommand(nick_, cmd_);
			} catch ( Exception e ) {
				logger_.warning("[" + nick_ + "](admin)@" + room_.getName() + " " + cmd_);
				
			}
			
		}
    }
    
    private String getMessage(String key) throws UnsupportedEncodingException {
    	return new String( messages_.getString( key ).getBytes("ISO-8859-1"), "UTF-8" );
    }
    
    private String getMessage(String key, Object ... args ) throws UnsupportedEncodingException {
    	String s = new String( messages_.getString( key ).getBytes("ISO-8859-1"), "UTF-8" );
    	return String.format(s, args);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	messages_ = ResourceBundle.getBundle("i18n.ServerMessages");
        // Send greeting for a new connection.
        ctx.write( getMessage("greetings") + NEWLINE );
        //ctx.write("It is " + new Date() + " now.\r\n");
        ctx.flush();
        
        cgAllUsers_.add(ctx.channel());
        cgLobby_.add(ctx.channel());
        
        myGame_ = null;
        nickname_ = Util.generateNickname();
        lobbyBroadcast( getMessage("enterLobby", nickname_ ) );
        
        logger_.info("[" + nickname_ + "] logged in");
    }
    
    @Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		if ( myGame_ == null ) {
			lobbyBroadcast("[" + nickname_ + "]님이 로비에서 나갔습니다.");
		} else {
			myGame_.quit(nickname_);
		}
		logger_.info("[" + nickname_ + "] disconnected");
	}

	private void setNickname(ChannelHandlerContext ctx, String request) throws Exception {
    	String old = nickname_;
    	nickname_ = request;
    	lobbyBroadcast("[" + old + "]님이 닉네임을 [" + nickname_ + "]으로 변경했습니다.");
    }
    
    private void listGameRooms(ChannelHandlerContext ctx) throws Exception {
    	Set<String> roomnames = allGameRooms_.keySet();
    	Iterator<String> iter = roomnames.iterator();
    	while ( iter.hasNext() ) {
    		String name = iter.next();
    		
    		ctx.channel().write("[" + name + "]" + NEWLINE);
    	}
    	
    	ctx.channel().flush();
    }
    
    private void joinGameRoom(ChannelHandlerContext ctx, String key) throws Exception {
    	GameRoom room = allGameRooms_.get(key);
    	
    	if ( room == null ) {
    		throw new GeniusServerException(key + "번 게임방은 존재하지 않습니다.");
    	}
    	
    	room.join(nickname_, ctx);
    	myGame_ = room;
    	cgLobby_.remove( ctx.channel() );
    	lobbyBroadcast("[" + nickname_ + "]님이 " + key + "번 게임방에 들어갔습니다.");
    }
    
    private void createGameRoom(ChannelHandlerContext ctx) throws Exception {
    	int i = 1;
    	GameRoom room = new GameRoom();
    	
    	while ( allGameRooms_.putIfAbsent( Integer.toString(i), room ) != null ) {
    		++i;
    	}
    	room.setName( Integer.toString(i) );
    	
    	joinGameRoom( ctx, Integer.toString(i) );
    }
       
    private void lobbyBroadcast(String msg) {
        for (Channel c: cgLobby_) {
        	c.writeAndFlush( "===== " + msg + " =====" + NEWLINE);
        }
    }
    
    private void lobbyCommand(ChannelHandlerContext ctx, String request) throws Exception {
    	String cmds[] = request.split("\\s+");
    	String cmd = cmds[0].toLowerCase();
    	
    	logger_.info("[" + nickname_ + "] " + request);
    	
    	if ( cmd.equals("/nickname") || cmd.equals("/nick") ) {
    		setNickname( ctx, cmds[1] );
    	} else if ( cmd.equals("/list") ) {
    		listGameRooms(ctx);
    	} else if ( cmd.equals("/join") ) {
    		if ( cmds.length >= 2 ) {
    			joinGameRoom(ctx, cmds[1]);
    		}
    	} else if ( cmd.equals("/create") ) {
    		createGameRoom(ctx);
    	} else if ( cmd.equals("/bye") ) {
    		ctx.close();
    	} else {
    		lobbyUsageSimple(ctx);
    	}
    }
    
    private void lobbyUsageSimple(ChannelHandlerContext ctx) {
    	ctx.channel().writeAndFlush(LOBBY_USAGE_SIMPLE + NEWLINE);
    }

    public static String stringToHex(String s) {
        String result = "";

        for (int i = 0; i < s.length(); i++) {
          result += String.format("%02X ", (int) s.charAt(i));
        }

        return result;
      }
    
    @Override
    public void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {

    	if( myGame_ == null ) {
    		if ( request.isEmpty() ) {
    			// 로비 도움말
    			lobbyUsageSimple(ctx);
    		} else {
	    		if ( request.charAt(0) == '/' ) {
	    			// 로비 명령어
	    			try {
	    				lobbyCommand(ctx, request);
	    			} catch ( GeniusServerException e ) {
	    				ctx.channel().writeAndFlush( e.getMessage() + NEWLINE );
	    			}
	    		} else {
	    			// 로비 채팅
	                for (Channel c: cgLobby_) {
	                	c.writeAndFlush("[" + nickname_ + "] " + request + NEWLINE);
	                	/*
	                    if (c != ctx.channel()) {
	                        c.writeAndFlush("[" + nickname + "] " + request + NEWLINE);
	                    } else {
	                        c.writeAndFlush("[" + nickname + "] " + request + NEWLINE);
	                    }
	                    */
	                }
	    		}
    		}
    	} else {
    		if ( request.isEmpty() ) {
    			// 게임방 도움말
    			myGame_.printUsageSimple(nickname_);
    		} else {
	    		if ( request.charAt(0) == '/' ) {
	    			// 게임방 명령어
	    			try {
	    				logger_.info("[" + nickname_ + "]@" + myGame_.getName() + " " + request);
	    				myGame_.userCommand(nickname_, request);
	    				while ( ! myGame_.getJobQueue().isEmpty() ) {
	    					ScheduleRequest req = myGame_.getJobQueue().poll();
	    					ScheduledJob job = new ScheduledJob(myGame_, nickname_, req.getCommand());
	    					jobScheduler.schedule(job, req.getDelay());
	    				}
	    			} catch ( QuitGameRoomException q ) {
	    				myGame_ = null;
	    				cgLobby_.add(ctx.channel());
	    				lobbyBroadcast( "[" + nickname_ + "]님이 로비에 들어왔습니다" );
	    			} catch ( GeniusServerException e ) {
	    				ctx.channel().writeAndFlush( e.getMessage() + NEWLINE );
	    			}
	    		} else {
	    			// 게임방 채팅
	    			try {
	    				myGame_.chat( nickname_, request );
	    			} catch ( GeniusServerException e ) {
	    				ctx.channel().writeAndFlush( e.getMessage() + NEWLINE );
	    			}
	    		}
    		}
    	}
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger_.log(
                Level.WARNING,
                "Unexpected exception from downstream.", cause);
        ctx.close();
    }
}

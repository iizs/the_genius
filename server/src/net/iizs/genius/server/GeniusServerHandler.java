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
	static final ConcurrentMap<String, Player> cachedPlayers_ = new ConcurrentHashMap<String, Player>();
	static final Timer jobScheduler = new Timer();
	
	ResourceBundle messages_;
	ServerMessageFormatter formatter_;
	
	Player player_ = null;
	GameRoom currentGame_ = null;
	
	String nickname_;
	
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
    
    private String[] parseCommand(String s) {
    	// TODO 따옴표 인자 처리
    	// TODO newline 처리
    	String cmds[] = s.split("\\s+");
    	cmds[0] = cmds[0].toLowerCase();
    	return cmds;
    }
    
    private void loginCommand(ChannelHandlerContext ctx, String request) throws Exception {
    	String cmds[] = parseCommand(request);
		String cmd = cmds[0];
    	if ( cmd.equals("/login") ) {
    		// TODO login
    		if ( cmds.length >= 3 ) { // /login <id> <password>
    			// TODO auth
    			player_ = cachedPlayers_.get( cmds[1] );
    			if ( player_ == null ) {
    				player_ = new Player( cmds[1] );
    				cachedPlayers_.put( cmds[1], player_ );
    			}
    			ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("enterLobby", player_.getNickname() ) ) ) );
    			ctx.flush();
    		} else {
    			// TODO 사실은 salt 를 반환해야 하는 시점이다. 현재는 usage로 땜빵해둔다.
    			ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("usageLogin") ) ) );
    			ctx.flush();
    		}
    	} else {
    		ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("eLoginRequired") ) ) );
			ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("usageLogin") ) ) );
			ctx.flush();
    	}
    }

    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	messages_ = ResourceBundle.getBundle("i18n.ServerMessages");
    	formatter_ = ServerMessageFormatter.getInstance("TextFormatter");
        
    	// Send greeting for a new connection.
        ctx.write( getMessage("greetings") + NEWLINE );
        ctx.flush();
        
        // login 정보가 없으므로 player_, currentGame_ 를 초기화한다.
        player_ = null;
        currentGame_ = null;
        
        // TDOO 새로운 스펙에 맞도록 수정 필요
        
        //cgAllUsers_.add(ctx.channel());
        //cgLobby_.add(ctx.channel());
        
        //myGame_ = null;
        //nickname_ = Util.generateNickname();
        //lobbyBroadcast( getMessage("enterLobby", nickname_ ) );
        
        //logger_.info("[" + nickname_ + "] logged in");
        
    }
    
    @Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		if ( currentGame_ == null ) {
			lobbyBroadcast("[" + nickname_ + "]님이 로비에서 나갔습니다.");
		} else {
			currentGame_.quit(nickname_);
		}
		logger_.info("[" + nickname_ + "] disconnected");
		// TODO 새로운 스펙에 맞도록 수정 필요
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
    	currentGame_ = room;
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
    	if ( request.length() == 0 ) {
    		return;
    	}
    	
    	if ( player_ == null ) {
    		// login 단계
    		if ( request.charAt(0) == '/' ) {
    			try {
    				loginCommand(ctx, request);
    			} catch ( GeniusServerException e ) {
    				ctx.channel().writeAndFlush( formatter_.formatErrorMessage( e.getMessage() ) );
    			}
    		} else {
    			ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("eLoginRequired") ) ) );
    			ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("usageLogin") ) ) );
    	        ctx.flush();
    		}
    	} else if( currentGame_ == null ) { // player_ != null 
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
    			currentGame_.printUsageSimple(nickname_);
    		} else {
	    		if ( request.charAt(0) == '/' ) {
	    			// 게임방 명령어
	    			try {
	    				logger_.info("[" + nickname_ + "]@" + currentGame_.getName() + " " + request);
	    				currentGame_.userCommand(nickname_, request);
	    				while ( ! currentGame_.getJobQueue().isEmpty() ) {
	    					ScheduleRequest req = currentGame_.getJobQueue().poll();
	    					ScheduledJob job = new ScheduledJob(currentGame_, nickname_, req.getCommand());
	    					jobScheduler.schedule(job, req.getDelay());
	    				}
	    			} catch ( QuitGameRoomException q ) {
	    				currentGame_ = null;
	    				cgLobby_.add(ctx.channel());
	    				lobbyBroadcast( "[" + nickname_ + "]님이 로비에 들어왔습니다" );
	    			} catch ( GeniusServerException e ) {
	    				ctx.channel().writeAndFlush( e.getMessage() + NEWLINE );
	    			}
	    		} else {
	    			// 게임방 채팅
	    			try {
	    				currentGame_.chat( nickname_, request );
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

package net.iizs.genius.server;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
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
    
    public String getMessage(String key) {
    	try {
			return new String( messages_.getString( key ).getBytes("ISO-8859-1"), "UTF-8" );
		} catch (UnsupportedEncodingException e) {
			logger_.warning( e.getMessage() );
			return messages_.getString( key );
		} catch (MissingResourceException e) {
			logger_.warning( e.getMessage() );
			return key;
		}
    }
    
    public String getMessage(String key, Object ... args ) {
    	try {
	    	String s = new String( messages_.getString( key ).getBytes("ISO-8859-1"), "UTF-8" );
	    	return String.format(s, args);
    	} catch (UnsupportedEncodingException e) {
			logger_.warning( e.getMessage() );
			return messages_.getString( key );
    	} catch (MissingResourceException e) {
			logger_.warning( e.getMessage() );
			return key;
		}
    }
    
    private String[] parseCommand(String s) {
    	// TODO 따옴표 인자 처리
    	// TODO newline 처리
    	String cmds[] = s.split("\\s+");
    	cmds[0] = cmds[0].toLowerCase();
    	return cmds;
    }
    
    
    private void loginUsageSimple(ChannelHandlerContext ctx) throws Exception {
    	ctx.writeAndFlush( formatter_.formatResponseMessage( new SimpleResponse( getMessage("usageLogin") ) ) );
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
    			player_.setChannel(ctx.channel());
    			
    			// TODO 진행중인 게임이 있었다면, 자동으로 참가하게 하는 것은 어떨까?
    	        
    	        cgAllUsers_.add(ctx.channel());
    	        cgLobby_.add(ctx.channel());
    			
    	        lobbyBroadcast( getMessage("enterLobby", player_.getId(), player_.getNickname() ) );
    	        
    	        logger_.info(player_.toString() + " @ " + player_.getChannel().toString() + " logged in");
    		} else {
    			// TODO 사실은 salt 를 반환해야 하는 시점이다. 현재는 usage로 땜빵해둔다.
    			ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("usageLogin") ) ) );
    			ctx.flush();
    		}
    	} else {
    		ctx.write( formatter_.formatErrorMessage( getMessage("eLoginRequired") ) );
			ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("usageLogin") ) ) );
			ctx.flush();
    	}
    }

    private void lobbyBroadcast(String msg) {
        for (Channel c: cgLobby_) {
        	c.writeAndFlush( formatter_.formatLobbyMessage(msg) );
        }
    }
    


	private void setNickname(ChannelHandlerContext ctx, String request) throws Exception {
		// TODO
    	String old = nickname_;
    	nickname_ = request;
    	lobbyBroadcast("[" + old + "]님이 닉네임을 [" + nickname_ + "]으로 변경했습니다.");
    }
    
    private void listGameRooms(ChannelHandlerContext ctx, String[] cmds) throws Exception {
    	List<GameRoom> roomlist = Collections.list(Collections.enumeration(allGameRooms_.values()));
    	Collections.sort(roomlist);
    	
    	String game_id = "all";
    	int start_num = 1;
    	int display_cnt = 20;
    	
    	if ( cmds.length >= 2 ) {
    		game_id = cmds[1];
    	}
    	
    	if ( cmds.length >= 3 ) {
    		try {
    			start_num = Integer.parseInt( cmds[2] );
    		} catch ( NumberFormatException e ) {
    			ctx.writeAndFlush( 
    					formatter_.formatResponseMessage( 
    							new SimpleResponse( 
    									getMessage("wIntegerRequiredUseDefault", 
    												cmds[2], start_num ) ) ) );
    		}
    	}
    	
    	if ( cmds.length >= 4 ) {
    		try {
    			display_cnt = Integer.parseInt( cmds[3] );
    		} catch ( NumberFormatException e ) {
    			ctx.writeAndFlush( 
    					formatter_.formatResponseMessage( 
    							new SimpleResponse( 
    									getMessage("wIntegerRequiredUseDefault", 
    												cmds[3], display_cnt ) ) ) );
    		}
    	}
    	
    	Iterator<GameRoom> iter = roomlist.iterator();
    	ListResponse<GameRoom> resp = new ListResponse<>("");
    	
    	while ( iter.hasNext() ) {
    		GameRoom room = iter.next();
    		
    		if ( game_id.equals("all") 
    				|| game_id.equals( room.getGameId() ) ) {
    			logger_.info(game_id + " " + room.getGameId() + " "  + start_num + " " + display_cnt  +  " " + resp.size());
    			if ( start_num > 1 ) {
    				--start_num;
    			} else {
    				resp.add(room);
    				if ( resp.size() >= display_cnt ) {
        				break;
        			}
    			}
    		}
    	}
    	
    	if ( resp.size() > 0 ) {
	    	ctx.channel().write( formatter_.formatResponseMessage(resp) );
	    	ctx.channel().flush();
    	} else {
    		ctx.writeAndFlush( 
					formatter_.formatErrorMessage( getMessage("eNoGameRoomsFound" ) ) );
    	}
    }
    
    private void joinGameRoom(ChannelHandlerContext ctx, String key) throws Exception {
    	// TODO
    	GameRoom room = allGameRooms_.get(key);
    	
    	if ( room == null ) {
    		throw new GeniusServerException(key + "번 게임방은 존재하지 않습니다.");
    	}
    	
    	room.join(nickname_, ctx);
    	currentGame_ = room;
    	cgLobby_.remove( ctx.channel() );
    	lobbyBroadcast("[" + nickname_ + "]님이 " + key + "번 게임방에 들어갔습니다.");
    }
    
    private void createGameRoom(ChannelHandlerContext ctx, String[] cmds) throws Exception {
    	// TODO
    	int i = 1;
    	
    	if ( cmds.length < 2 ) {
    		ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("usageCreate" ) ) ) );
    		ctx.write( formatter_.formatResponseMessage( new SimpleResponse( getMessage("usageGamdIds" ) ) ) );
    		ctx.flush();
    		throw new InvalidArgumentsException( getMessage( "eMissingArgs", "<game_id>") );
    	}
    	
    	GameRoom room = GameRoom.getInstance(this, cmds[1] );
    	
    	while ( allGameRooms_.putIfAbsent( Integer.toString(i), room ) != null ) {
    		++i;
    	}
    	room.setName( Integer.toString(i) );
    	
    	//joinGameRoom( ctx, Integer.toString(i) );
    }
       

    private boolean commonCommand(ChannelHandlerContext ctx, String cmds[]) throws Exception {
    	// TODO 공통 명령들 처리
    	return false;
    }

    private boolean lobbyCommand(ChannelHandlerContext ctx, String cmds[]) throws Exception {
    	// TODO
		String cmd = cmds[0];
    	boolean processed = false;
    	
    	
    	if ( cmd.equals("/list") || cmd.equals("/l") ) {
    		listGameRooms(ctx, cmds);
    		processed = true;
    	} else if ( cmd.equals("/join") ) {
    		if ( cmds.length >= 2 ) {
    			joinGameRoom(ctx, cmds[1]);
    			processed = true;
    		}
    	} else if ( cmd.equals("/create") || cmd.equals("/c") ) {
    		createGameRoom(ctx, cmds);
    		processed = true;
    	} else if ( cmd.equals("/logout") ) {
    		ctx.close();
    		processed = true;
    	}
    	
    	return processed;
    }
    
    private void lobbyUsageSimple(ChannelHandlerContext ctx) {
    	// TODO
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
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	messages_ = ResourceBundle.getBundle("i18n.ServerMessages");
    	formatter_ = ServerMessageFormatter.getInstance("TextFormatter")
    				.registerCustomFormatter(ListResponse.class, new ListResponseFormatter());
        
    	// Send greeting for a new connection.
        ctx.writeAndFlush( formatter_.formatResponseMessage( new SimpleResponse( getMessage("greetings") ) ) );
        
        // login 정보가 없으므로 player_, currentGame_ 를 초기화한다.
        player_ = null;
        currentGame_ = null; 
    }
    
    @Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	// TODO
		super.channelInactive(ctx);
		if ( currentGame_ == null ) {
			lobbyBroadcast("[" + nickname_ + "]님이 로비에서 나갔습니다.");
		} else {
			currentGame_.quit(nickname_);
		}
		logger_.info("[" + nickname_ + "] disconnected");
		// TODO 새로운 스펙에 맞도록 수정 필요
	}

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
    	// TODO
    	if ( player_ == null ) {
    		// login 단계
    		if ( request.isEmpty() ) {
    			// 로비 도움말
    			loginUsageSimple(ctx);
    		} else {
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
    		}
    	} else if( currentGame_ == null ) { // player_ != null 
    		if ( request.isEmpty() ) {
    			// 로비 도움말
    			lobbyUsageSimple(ctx);
    		} else {
	    		if ( request.charAt(0) == '/' ) {
	    			// 로비 명령어
	    			try {
    					logger_.info( player_.toString() + " " + request);
    					
	    				String cmds[] = parseCommand(request);
	    				if ( commonCommand( ctx, cmds ) == false 
	    						&& lobbyCommand(ctx, cmds) == false ) {
	    					lobbyUsageSimple(ctx);
	    				}
	    			} catch ( GeniusServerException e ) {
	    				ctx.channel().writeAndFlush( formatter_.formatErrorMessage( e.getMessage() ) );
	    			}
	    		} else {
	    			// 로비 채팅
	                for (Channel c: cgLobby_) {
	                	c.writeAndFlush( formatter_.formatChatMessage(player_.getId(), request) );
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

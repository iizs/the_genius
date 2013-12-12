package net.iizs.genius.foodchain;

import java.util.Iterator;
import java.util.Set;
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

import static net.iizs.genius.foodchain.Constants.*;

public class FoodChainServerHandler extends SimpleChannelInboundHandler<String> {

	static final ChannelGroup cgAllUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	static final ChannelGroup cgLobby = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	static final ConcurrentMap<String, GameRoom> allGameRooms = new ConcurrentHashMap<>();
	
	GameRoom myGame = null;
	String nickname;
	
    private static final Logger logger = Logger.getLogger(FoodChainServerHandler.class.getName());

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Send greeting for a new connection.
        ctx.write(
                "먹이사슬 게임에 오신 것을 환영합니다." + NEWLINE);
        //ctx.write("It is " + new Date() + " now.\r\n");
        ctx.flush();
        
        cgAllUsers.add(ctx.channel());
        cgLobby.add(ctx.channel());
        
        myGame = null;
        nickname = ctx.channel().remoteAddress().toString();
        lobbyBroadcast( "[" + nickname + "]님이 로비에 들어왔습니다" );
        
        logger.info("[" + nickname + "] logged in");
    }
    
    @Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		if ( myGame == null ) {
			lobbyBroadcast("[" + nickname + "]님이 로비에서 나갔습니다.");
		} else {
			myGame.quit(nickname);
		}
		logger.info("[" + nickname + "] disconnected");
	}

	private void setNickname(ChannelHandlerContext ctx, String request) throws Exception {
    	String old = nickname;
    	nickname = request;
    	lobbyBroadcast("[" + old + "]님이 닉네임을 [" + nickname + "]으로 변경했습니다.");
    }
    
    private void listGameRooms(ChannelHandlerContext ctx) throws Exception {
    	Set<String> roomnames = allGameRooms.keySet();
    	Iterator<String> iter = roomnames.iterator();
    	while ( iter.hasNext() ) {
    		String name = iter.next();
    		
    		ctx.channel().write("[" + name + "]" + NEWLINE);
    	}
    	
    	ctx.channel().flush();
    }
    
    private void joinGameRoom(ChannelHandlerContext ctx, String key) throws Exception {
    	GameRoom room = allGameRooms.get(key);
    	
    	if ( room == null ) {
    		throw new GeniusServerException(key + "번 게임방은 존재하지 않습니다.");
    	}
    	
    	room.join(nickname, ctx);
    	myGame = room;
    	cgLobby.remove( ctx.channel() );
    	lobbyBroadcast("[" + nickname + "]님이 " + key + "번 게임방에 들어갔습니다.");
    }
    
    private void createGameRoom(ChannelHandlerContext ctx) throws Exception {
    	int i = 1;
    	GameRoom room = new GameRoom();
    	
    	while ( allGameRooms.putIfAbsent( Integer.toString(i), room ) != null ) {
    		++i;
    	}
    	room.setName( Integer.toString(i) );
    	
    	joinGameRoom( ctx, Integer.toString(i) );
    }
       
    private void lobbyBroadcast(String msg) {
        for (Channel c: cgLobby) {
        	c.writeAndFlush( "===== " + msg + " =====" + NEWLINE);
        }
    }
    
    private void lobbyCommand(ChannelHandlerContext ctx, String request) throws Exception {
    	String cmds[] = request.split("\\s+");
    	String cmd = cmds[0].toLowerCase();
    	
    	logger.info("[" + nickname + "] " + request);
    	
    	if ( cmd.equals("/nickname") || cmd.equals("/nick") ) {
    		setNickname( ctx, cmds[1] );
    	} else if ( cmd.equals("/list") ) {
    		listGameRooms(ctx);
    	} else if ( cmd.equals("/join") ) {
    		joinGameRoom(ctx, cmds[1]);
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

    	if( myGame == null ) {
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
	                for (Channel c: cgLobby) {
	                	c.writeAndFlush("[" + nickname + "] " + request + NEWLINE);
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
    			myGame.printUsageSimple(nickname);
    		} else {
	    		if ( request.charAt(0) == '/' ) {
	    			// 게임방 명령어
	    			try {
	    				logger.info("[" + nickname + "]@" + myGame.getName() + " " + request);
	    				myGame.userCommand(nickname, request);
	    			} catch ( QuitGameRoomException q ) {
	    				myGame = null;
	    				cgLobby.add(ctx.channel());
	    				lobbyBroadcast( "[" + nickname + "]님이 로비에 들어왔습니다" );
	    			} catch ( GeniusServerException e ) {
	    				ctx.channel().writeAndFlush( e.getMessage() + NEWLINE );
	    			}
	    		} else {
	    			// 게임방 채팅
	    			myGame.chat( nickname, request );
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
        logger.log(
                Level.WARNING,
                "Unexpected exception from downstream.", cause);
        ctx.close();
    }
}

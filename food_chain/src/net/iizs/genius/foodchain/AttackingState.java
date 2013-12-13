package net.iizs.genius.foodchain;

import static net.iizs.genius.foodchain.Constants.*;
import io.netty.channel.Channel;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class AttackingState extends AbstractGameRoomState {
	private static final Logger logger_ = Logger.getLogger(FoodChainServerHandler.class.getName());

	private boolean forceProceed_;
	
	public AttackingState(AbstractGameRoomState cl) {
		super(cl);

		forceProceed_ = false;
		
		// 플레이어 이동. 진행 동의 초기화
		for ( Player p: players_.values() ) {
			if ( p.isAlive() ) {
				minimap_.get(p.getCurrentArea()).remove(p);
				p.setCurrentArea(p.getMoves().get(round_ - 1));
				minimap_.get(p.getCurrentArea()).add(p);
				
				p.setPassed(false);
			}
		}
		
		jobQueue_.add(new ScheduleRequest("/admin " + adminPassword_ + " end " + Integer.toString(round_)
						, ATTACK_TIME_LIMIT_SECOND * 1000));
		
		broadcast( round_ + " 라운드가 시작되었습니다.");
		broadcast("플레이어들의 이동결과는 다음과 같습니다.");
		broadcast("");
		broadcast( Area.PLAINS.getName() + ": " + minimap_.get(Area.PLAINS).toString() );
		broadcast( Area.WOODS.getName() + ": " + minimap_.get(Area.WOODS).toString() );
		broadcast( Area.SKY.getName() + ": " + minimap_.get(Area.SKY).toString() );
		broadcast( Area.RIVER.getName() + ": " + minimap_.get(Area.RIVER).toString() );
		broadcast("");
		broadcast("각 플레이어들은 /a [닉네임] 명령으로 같은 지역에 있는 플레이어를 공격할 수 있습니다.");
		broadcast("공격 결과는 즉시 공개됩니다. 제한 시간은 " + ( ATTACK_TIME_LIMIT_SECOND / 60 ) + "분 입니다.");
		broadcast("공격 결과 판정 순서는 *입력 순서대로* 입니다.");
		broadcast("더 이상 공격을 하지 않으려 한다면 /next 명령을 입력해주세요.");
		broadcast("모든 플레이어가 /pass 명령을 입력한다면, " + ( ATTACK_TIME_LIMIT_SECOND / 60 ) + "분이 지나지 않아도 다음 라운드로 진행됩니다.");
	}
	
	private AbstractGameRoomState proceed() throws Exception {
		boolean flag = true;
		
		for ( Player p: players_.values() ) {
			if ( p.isBot() ) continue; 
			
			if ( p.isAlive() ) {
				if ( p.getPassed() != true ) {
					flag = false;
					break;
				}
			}
		}
		
		if ( flag == true || forceProceed_ == true ) {
			// 다음 라운드로 진행
			// TODO impl
			broadcast( round_ + " 라운드가 종료되었습니다.");
			
			if ( round_ < MAX_GAME_ROUND ) {				
				return new MovingState(this);
			} else {
				// 게임 종료
				// TODO impl
				broadcast("게임이 종료되었습니다.");
				return new WaitingState(this);
			}
		}
		
		return this;
	}

	@Override
	public synchronized AbstractGameRoomState userCommand(String nickname, String req)
			throws Exception {
		String cmds[] = req.split("\\s+", 3);
    	String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/a") ) {
    		Player p = getPlayer(nickname);
    		
    		if ( cmds.length < 2 ) {
    			throw new GeniusServerException("공격대상을 지정해야 합니다.");
    		}

    		// TODO impl
    	} else if ( cmd.equals("/pass") ) {
    		Player p = getPlayer(nickname);
    		
    		p.setPassed(true);
    		p.getChannel().writeAndFlush(">>> 다음 라운드로 진행해도 좋다고 설정하셨습니다." + NEWLINE );    		
    	} else if ( cmd.equals("/admin") ) {
    		cmds = req.split("\\s+");
        	
    		if ( cmds.length < 4 ) {
    			throw new GeniusServerException("/admin [관리자암호] end [라운드]");
    		}

    		if ( ! cmds[1].equals(adminPassword_) ) {
    			throw new GeniusServerException("관리자 암호 오류");
    		}
    		
    		if ( cmds[2].toLowerCase().equals("end") ) {
	    		if ( Integer.parseInt( cmds[3] ) != round_ ) {
	    			throw new GeniusServerException("진행중인 라운드가 아닙니다.");
	    		}
	    		forceProceed_ = true;
    		}
    		
    	} else if ( cmd.equals("/to") ) {
    		whisper(nickname, cmds[1], cmds[2]);
    	} else if ( cmd.equals("/info") ) {
    		showInfo( nickname );
    	} else {
    		printUsageSimple(nickname);
    	}
    	
    	return proceed();
	}

	@Override
	public void printUsageSimple(String nickname) throws Exception {
		getPlayer(nickname).getChannel().writeAndFlush(ATTACK_USAGE_SIMPLE + NEWLINE);
	}

	@Override
	public void showInfo(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		p.getChannel().write( "> 방 번호: " + name_ + NEWLINE );
		p.getChannel().write( "> 관리자암호: " + adminPassword_ + NEWLINE );
		p.getChannel().write( "> 플레이어" + NEWLINE );
		
    	Set<String> playerNames = players_.keySet();
    	Iterator<String> iter = playerNames.iterator();
    	while ( iter.hasNext() ) {    		
    		Player i = getPlayer(iter.next());
    		
    		p.getChannel().write("> [" + i.getNickname() + "]");
    		if ( i.isBot() ) {
    			p.getChannel().write( " (Bot)");
    		}
    		p.getChannel().write(": ");
    		p.getChannel().write( i.getMoves().subList(0, round_ - 1).toString() );
    		p.getChannel().write(": " + ( i.isAlive() ? "생존" : "죽음" ) );
    		p.getChannel().write( NEWLINE );
    	}
    	
    	p.getChannel().write( Area.PLAINS.getName() + ": " + minimap_.get(Area.PLAINS).toString() + NEWLINE );
    	p.getChannel().write( Area.WOODS.getName() + ": " + minimap_.get(Area.WOODS).toString() + NEWLINE );
    	p.getChannel().write( Area.SKY.getName() + ": " + minimap_.get(Area.SKY).toString() + NEWLINE );
		p.getChannel().write( Area.RIVER.getName() + ": " + minimap_.get(Area.RIVER).toString() + NEWLINE  );
    	
    	p.getChannel().flush();
	}
	
	@Override
	public void chat(String nickname, String msg) throws Exception {
		Player p = getPlayer(nickname);
        for (Player to: minimap_.get(p.getCurrentArea())) {
        	if ( to.isBot() ) continue;
        	to.getChannel().writeAndFlush("[" + nickname + "] " + msg + NEWLINE);
        }
	}
	
	@Override
	public void whisper(String nickname, String to, String msg) throws Exception {
		Player p = getPlayer(to);
		Player me = getPlayer(nickname);
		
		if ( p.isBot() ) {
			throw new GeniusServerException("[" + to + "]님은 봇입니다.");
		}
		
		if ( ! p.getCurrentArea().equals(me.getCurrentArea() ) ) {
			throw new GeniusServerException("다른 지역에 있는 플레이어에게는 귓속말을 보낼 수 없습니다.");
		}
		
		p.getChannel().writeAndFlush(">>> [" + nickname + "]님의 귓속말: " + msg + NEWLINE);
		me.getChannel().writeAndFlush( ">>> [" + to + "]님께 귓속말을 보냈습니다." + NEWLINE);
	}

}

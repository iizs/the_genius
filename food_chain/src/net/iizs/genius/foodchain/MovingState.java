package net.iizs.genius.foodchain;

import static net.iizs.genius.foodchain.Constants.*;

import java.util.Iterator;
import java.util.Set;

public class MovingState extends AbstractGameRoomState {
	public MovingState(AbstractGameRoomState cl) {
		super(cl);
		
		++round_;
		
		broadcast("각 플레이어들은 /move [지역] 명령으로 " + round_ + " 라운드에 이동할 지역을 선택해주세요.");
		broadcast("주 서식지로 이동하셔야만 하는 분들도 반드시 이동 명령을 내려주세요.");
		broadcast("다만, 다른 곳으로 이동하도록 설정하셔도 강제로 주 서식지로 이동합니다.");
		broadcast("이동 결과는 모든 플레이어가 각자의 명령을 실행한 다음 동시에 공개 됩니다.");
	}
	
	private AbstractGameRoomState proceed() throws Exception {
		boolean flag = true;
		
		for ( Player p: players_.values() ) {
			if ( p.isBot() ) continue;
			if ( ! p.isAlive() ) continue;
			
			if ( p.getMoves().size() < round_ ) {
				flag = false;
				break;
			}
		}
		
		if ( flag == true ) {
			// 봇들의 선택을 랜덤으로 추가
			for ( Player p: players_.values() ) {
				if ( p.isBot() ) {
					p.addMove(round_, p.getCharacter().getHabitat());
				}
			}
			
			// 2라운드 이후부터는 저건에 맞는 플레이어를 주 서식지로 강제 이주 
			if ( round_ > 1 ) {
				for ( Player p: players_.values() ) {
					if ( ! p.isAlive() ) continue;
					// 이동 기록은 0-base 이므로 - 2를 해야 한다.
					if ( ! p.getMoves().get(round_ - 2).equals( p.getCharacter().getHabitat() ) ) {
						p.addMove(round_, p.getCharacter().getHabitat() );
					}
				}
			}
			
			for ( Player p: players_.values() ) {
				if ( ! p.isAlive() ) continue;
				broadcast( "[" + p.getNickname() + "]님은 '" + p.getMoves().get( round_ - 1 ).getName() + "'으로 이동했습니다.");
			}
			
			return new AttackingState(this);
		}
		
		return this;
	}

	@Override
	public AbstractGameRoomState userCommand(String nickname, String req)
			throws Exception {
		String cmds[] = req.split("\\s+", 3);
    	String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/move") ) {
    		Player p = getPlayer(nickname);
    		
    		if ( cmds.length < 2 ) {
    			throw new GeniusServerException("이동지역을 지정해야 합니다.");
    		}
    		
    		p.addMove(round_, Area.getAreaOf(cmds[1]));
    		p.getChannel().writeAndFlush(">>> '" + cmds[1] + "'로 이동하기로 설정하셨습니다." + NEWLINE );
    		
    		
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
		getPlayer(nickname).getChannel().writeAndFlush(MOVE_USAGE_SIMPLE + NEWLINE);
	}

	@Override
	public void showInfo(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		p.getChannel().write( "> 방 번호: " + name_ + NEWLINE );
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
    	
    	p.getChannel().flush();
	}

}

package net.iizs.genius.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static net.iizs.genius.server.Constants.*;

public class InitState extends AbstractGameRoomState {

	public InitState(AbstractGameRoomState cl) {
		super(cl);
		
		round_ = 0;
		kills_ = 0;
		minimap_ = new HashMap<Area,List<Player>>();
		for ( Area a : Area.values() ) {
			minimap_.put(a, new ArrayList<Player>());
		}
		herbivores_ = new HashSet<Player>();
		charmap_ = new HashMap<Character,Player>();
		
		List<Character> chars = Arrays.asList( Character.values() );
		Collections.shuffle( chars );
		List<Player> players = new ArrayList<Player>( players_.values() );
		
		for ( int i=0; i < chars.size(); ++i ) {
			Player p = players.get(i);
			Character c = chars.get(i);
			
			p.reset();
			p.setCharacter( c );
			p.setCurrentArea( Area.HALL );
			minimap_.get( Area.HALL ).add(p);
			charmap_.put(c, p);
			
			if ( c.equals(Character.MALLARD)
					|| c.equals(Character.RABBIT)
					|| c.equals(Character.DEER)
					|| c.equals(Character.OTTER) ) {
				herbivores_.add( p );
			}
			
			if ( ! p.isBot() ) {
				p.getChannel().writeAndFlush(">>> 당신은 '" + c.getName() + "' 입니다." + NEWLINE );
				p.getChannel().writeAndFlush(">>> 주서식지: " + c.getHabitat().getName() + NEWLINE );
				p.getChannel().writeAndFlush(">>> 이동가능: 들, 숲, 강" + ( c.isFlyable() ? ", 하늘" : "" ) + NEWLINE );
				p.getChannel().writeAndFlush(">>> 승리조건: " + c.winningCondition() + NEWLINE );
				p.getChannel().writeAndFlush(">>> 패배조건: " + c.losingCondition() + NEWLINE );
				p.getChannel().writeAndFlush(">>> 특이사항: " + c.note() + NEWLINE );
			}
		}
		
		broadcast("각 플레이어들은 /peep [닉네임] 명령으로 엿보기를 실행해주세요.");
		broadcast("2회 엿보기가 가능한 플레이어들은 /peep 명령을 두 번 실행하시면 됩니다.");
		broadcast("'까마귀'는 /select [동물이름] 명령으로 우승자를 지목해 주세요.");
		broadcast("'카멜레온'은 /select [동물이름] 명령으로 위장할 동물을 선택해주세요.");
		broadcast("엿보기 결과는 모든 플레이어가 각자의 명령을 실행한 다음 동시에 공개됩니다.");
		
	}
	
	private AbstractGameRoomState proceed() throws Exception {
		boolean flag = true;
		
		for ( Player p: players_.values() ) {
			if ( p.isBot() ) continue;
			Character c = p.getCharacter();
			if ( c.equals(Character.CROW) || c.equals(Character.CHAMELEON) ) {
				if ( p.getSelection() == null ) {
					flag = false;
					break;
				}
			}
			if ( c.getPeepingCount() != p.getPeeps().size() ) {
				flag = false;
				break;
			}
		}
		
		if ( flag == true ) {
			// 봇들의 선택을 랜덤으로 추가
			for ( Player p: players_.values() ) {
				if ( p.isBot() ) {
					Character c = p.getCharacter();
					if ( c.equals(Character.CROW) ) {
						p.setSelection(Character.LION);
					}
					
					if ( c.equals(Character.CHAMELEON) ) {
						p.setSelection(Character.SNAKE);
					}
					
					// 봇의 peep 은 추가하지 않아도 진행에 문제는 없다.
				}
			}
			
			// 각 사용자에게 peep 결과를 반환
			for ( Player p: players_.values() ) {
				if ( ! p.isBot() ) {
					for ( String n : p.getPeeps() ) {
						Character c = getPlayer(n).getCharacter();
						
						if ( c.equals(Character.CHAMELEON) ) {
							c = getPlayer(n).getSelection();
						}
						
						p.getChannel().writeAndFlush(">>> [" + n + "]님은 '" + c.getName() + "' 입니다." + NEWLINE );
					}
				}
			}
			
			return new MovingState(this);
		}
		return this;
	}

	@Override
	public synchronized AbstractGameRoomState userCommand(String nickname, String req) throws Exception {
		String cmds[] = req.split("\\s+", 3);
    	String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/peep") ) {
    		Player p = getPlayer(nickname);
    		if ( cmds.length < 2 ) {
    			throw new GeniusServerException("플레이어 닉네임을 지정해야 합니다.");
    		}
    		
    		p.addPeep(getPlayer(cmds[1]).getNickname());
    		
    		p.getChannel().writeAndFlush(">>> [" + cmds[1] + "]님을 엿보기로 설정하셨습니다." + NEWLINE );
    		
    		if ( p.getCharacter().getPeepingCount() != p.getPeeps().size() ) {
    			p.getChannel().writeAndFlush(">>> 엿보기 횟수가 " + ( p.getCharacter().getPeepingCount() - p.getPeeps().size() ) + "회 남아 있습니다." + NEWLINE );
    		} else {
    			p.getChannel().writeAndFlush(">>> 엿보기 설정이 완료되었습니다;" + p.getPeeps().toString() + NEWLINE );
    		}
    		
    	} else if ( cmd.equals("/select") ) {
    		Player p = getPlayer(nickname);
    		Character c = p.getCharacter();
    		
    		if ( cmds.length < 2 ) {
    			throw new GeniusServerException("동물이름을 지정해야 합니다.");
    		}
    		
    		if ( c.equals(Character.CROW) ) {
    			p.setSelection( Character.getCharacterOf( cmds[1] ) );
    			p.getChannel().writeAndFlush(">>> '" + cmds[1] + "'을 우승자로 예상하셨습니다." + NEWLINE );
    		} else if ( c.equals(Character.CHAMELEON) ) {
    			p.setSelection( Character.getCharacterOf( cmds[1] ) );
    			p.getChannel().writeAndFlush(">>> '" + cmds[1] + "'로 위장하셨습니다." + NEWLINE );
    		} else {
    			throw new GeniusServerException("'" + c.getName() + "'는 이 명령을 실행할 수 없습니다.");
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
		getPlayer(nickname).getChannel().writeAndFlush(INIT_USAGE_SIMPLE + NEWLINE);
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
    		p.getChannel().write( NEWLINE );
    	}
    	
    	p.getChannel().flush();
	}

}

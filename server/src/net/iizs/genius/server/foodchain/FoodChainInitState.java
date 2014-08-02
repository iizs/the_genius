package net.iizs.genius.server.foodchain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.iizs.genius.server.GeniusServerException;
import net.iizs.genius.server.Player;
import net.iizs.genius.server.SimpleResponse;
import static net.iizs.genius.server.Constants.NEWLINE;

public class FoodChainInitState extends AbstractFoodChainState {

	public FoodChainInitState(AbstractFoodChainState cl) {
		// TODO 
		super(cl);
		
		round_ = 0;
		kills_ = 0;
		minimap_ = new HashMap<FoodChainArea,List<FoodChainPlayer>>();
		for ( FoodChainArea a : FoodChainArea.values() ) {
			minimap_.put(a, new ArrayList<FoodChainPlayer>());
		}
		herbivores_ = new HashSet<FoodChainPlayer>();
		charmap_ = new HashMap<FoodChainCharacter,FoodChainPlayer>();
		
		List<FoodChainCharacter> chars = Arrays.asList( FoodChainCharacter.values() );
		Collections.shuffle( chars );
		List<Player> players = new ArrayList<Player>( getAllPlayers().values() );
		
		for ( int i=0; i < chars.size(); ++i ) {
			FoodChainPlayer p = (FoodChainPlayer) players.get(i);
			FoodChainCharacter c = chars.get(i);
			
			p.reset();
			p.setCharacter( c );
			p.setCurrentArea( FoodChainArea.HALL );
			minimap_.get( FoodChainArea.HALL ).add(p);
			charmap_.put(c, p);
			
			if ( c.equals(FoodChainCharacter.MALLARD)
					|| c.equals(FoodChainCharacter.RABBIT)
					|| c.equals(FoodChainCharacter.DEER)
					|| c.equals(FoodChainCharacter.OTTER) ) {
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
	
	private AbstractFoodChainState proceed() throws Exception {
		boolean flag = true;
		
		for ( Player ap: getAllPlayers().values() ) {
			FoodChainPlayer p = (FoodChainPlayer) ap;
			if ( p.isBot() ) continue;
			FoodChainCharacter c = p.getCharacter();
			if ( c.equals(FoodChainCharacter.CROW) || c.equals(FoodChainCharacter.CHAMELEON) ) {
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
			for ( Player ap: getAllPlayers().values() ) {
				FoodChainPlayer p = (FoodChainPlayer) ap;
				if ( p.isBot() ) {
					FoodChainCharacter c = p.getCharacter();
					if ( c.equals(FoodChainCharacter.CROW) ) {
						p.setSelection(FoodChainCharacter.LION);
					}
					
					if ( c.equals(FoodChainCharacter.CHAMELEON) ) {
						p.setSelection(FoodChainCharacter.SNAKE);
					}
					
					// 봇의 peep 은 추가하지 않아도 진행에 문제는 없다.
				}
			}
			
			// 각 사용자에게 peep 결과를 반환
			for ( Player ap: getAllPlayers().values() ) {
				FoodChainPlayer p = (FoodChainPlayer) ap;
				if ( ! p.isBot() ) {
					for ( String n : p.getPeeps() ) {
						FoodChainCharacter c = getFoodChainPlayer(n).getCharacter();
						
						if ( c.equals(FoodChainCharacter.CHAMELEON) ) {
							c = getFoodChainPlayer(n).getSelection();
						}
						
						p.getChannel().writeAndFlush( getFormatter().formatResponseMessage(
								new SimpleResponse( getMessage("peepResult", n, c.getName()))) );
					}
				}
			}
			
			return new FoodChainMoveState(this);
		}
		return this;
	}

	@Override
	public synchronized AbstractFoodChainState userCommand(Player player, String[] cmds) throws Exception {
		//String cmds[] = req.split("\\s+", 3);
    	String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/peep") ) {
    		FoodChainPlayer p = getFoodChainPlayer(player.getId());
    		if ( cmds.length < 2 ) {
    			printUsage( player );
    		} else {
    			try {
    				p.addPeep( getPlayer(cmds[1]).getId());
    			} catch ( NoMorePeepAllowedException e ) {
    				throw new GeniusServerException( getMessage( "eNoMorePeepAllowed", p.getPeeps().toString() ) );
    			} catch ( CannotPeepYourselfException e ) {
    				throw new GeniusServerException( getMessage( "eCannotPeepYourself", p.getPeeps().toString() ) );
    			}
	    		
	    		p.getChannel().writeAndFlush( getFormatter().formatResponseMessage(
	    				new SimpleResponse( getMessage( "peepSet", cmds[1] ) ) ) );
	    		
	    		if ( p.getCharacter().getPeepingCount() != p.getPeeps().size() ) {
	    			p.getChannel().writeAndFlush( getFormatter().formatResponseMessage(
		    				new SimpleResponse( 
		    						getMessage( "peepRemains", 
		    								p.getCharacter().getPeepingCount() - p.getPeeps().size() ) ) ) );
	    		} else {
	    			p.getChannel().writeAndFlush( getFormatter().formatResponseMessage(
		    				new SimpleResponse( getMessage( "peepComplete", p.getPeeps().toString() ) ) ) );
	    		}
    		}
    		
    	} else if ( cmd.equals("/select") ) {
    		FoodChainPlayer p = getFoodChainPlayer(player.getId());
    		FoodChainCharacter c = p.getCharacter();
    		
    		if ( cmds.length < 2 ) {
    			printUsage( player );
    		} else {	
	    		if ( c.equals(FoodChainCharacter.CROW) ) {
	    			p.setSelection( FoodChainCharacter.getCharacterOf( cmds[1] ) );
	    			p.getChannel().writeAndFlush(getFormatter().formatResponseMessage(
		    				new SimpleResponse( getMessage( "winnerSelected", cmds[1] ) ) ) );
	    		} else if ( c.equals(FoodChainCharacter.CHAMELEON) ) {
	    			p.setSelection( FoodChainCharacter.getCharacterOf( cmds[1] ) );
	    			p.getChannel().writeAndFlush( getFormatter().formatResponseMessage(
		    				new SimpleResponse( getMessage( "disguiseSelected", cmds[1] ) ) ) );
	    		} else {
	    			throw new GeniusServerException( getMessage("eSelectionNotAllowed", c.getName() ) );
	    		}
    		}
    	} else if ( cmd.equals("/to") ) {
    		whisper(player, cmds[1], cmds[2]);
    	} else if ( cmd.equals("/info") ) {
    		showInfo( player );
    	} else {
    		printUsage(player);
    	}
    	
    	return proceed();
	}

	@Override
	public void printUsage(Player p) throws Exception {
		p.getChannel().writeAndFlush(getFormatter().formatResponseMessage(
				new SimpleResponse(getMessage("usageInitStateSimple"))));
	}

	public void showInfo(Player player) throws Exception {
		// TODO
		FoodChainPlayer p = getFoodChainPlayer(player.getId());
		
		p.getChannel().write( "> 방 번호: " + getName() + NEWLINE );
		p.getChannel().write( "> 플레이어" + NEWLINE );
		
    	Set<String> playerNames = getAllPlayers().keySet();
    	Iterator<String> iter = playerNames.iterator();
    	while ( iter.hasNext() ) {    		
    		FoodChainPlayer i = getFoodChainPlayer(iter.next());
    		
    		p.getChannel().write("> [" + i.getId() + "]");
    		if ( i.isBot() ) {
    			p.getChannel().write( " (Bot)");
    		}
    		p.getChannel().write( NEWLINE );
    	}
    	
    	p.getChannel().flush();
	}

}

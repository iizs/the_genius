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
import net.iizs.genius.server.KeyValueResponse;
import net.iizs.genius.server.Player;
import net.iizs.genius.server.SimpleResponse;

public class FoodChainInitState extends AbstractFoodChainState {

	public FoodChainInitState(AbstractFoodChainState cl) {
		super(cl);
		
		round_ = 0;
		kills_ = 0;
		minimap_ = new HashMap<FoodChainArea,List<FoodChainPlayer>>();
		areaNameMap_ = new HashMap<String,FoodChainArea>();
		for ( FoodChainArea a : FoodChainArea.values() ) {
			minimap_.put(a, new ArrayList<FoodChainPlayer>());
			areaNameMap_.put( getName(a), a );
		}
		
		herbivores_ = new HashSet<FoodChainPlayer>();
		charmap_ = new HashMap<FoodChainCharacter,FoodChainPlayer>();
		charNameMap_ = new HashMap<String, FoodChainCharacter>();
		
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
			charNameMap_.put( getName(c), c );
			
			if ( c.equals(FoodChainCharacter.MALLARD)
					|| c.equals(FoodChainCharacter.RABBIT)
					|| c.equals(FoodChainCharacter.DEER)
					|| c.equals(FoodChainCharacter.OTTER) ) {
				herbivores_.add( p );
			}
			
			if ( ! p.isBot() ) {
				p.getChannel().writeAndFlush( getFormatter().formatGameRoomMessage( 
						getMessage( "characterGuide"
								, getName(c)
								, getMessage( c.getHabitat().getId() )
								, ( c.isFlyable() ? ", " + getName( FoodChainArea.SKY ) : "" )
								, getMessage( c.winningConditionMessageId() )
								, getMessage( c.losingConditionMessageId() )
								, getMessage( c.noteMessageId() )
								) ) );
			}
		}
		
		broadcast( getMessage("initStateGuide") );	
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
								new SimpleResponse( getMessage("peepResult", n, getName(c)))) );
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
	    			p.setSelection( getCharacterOf( cmds[1] ) );
	    			p.getChannel().writeAndFlush(getFormatter().formatResponseMessage(
		    				new SimpleResponse( getMessage( "winnerSelected", cmds[1] ) ) ) );
	    		} else if ( c.equals(FoodChainCharacter.CHAMELEON) ) {
	    			p.setSelection( getCharacterOf( cmds[1] ) );
	    			p.getChannel().writeAndFlush( getFormatter().formatResponseMessage(
		    				new SimpleResponse( getMessage( "disguiseSelected", cmds[1] ) ) ) );
	    		} else {
	    			throw new GeniusServerException( getMessage("eSelectionNotAllowed", getName(c) ) );
	    		}
    		}
    	} else if ( cmd.equals("/to") ) {
    		whisper(player, cmds[1], cmds[2]);
    	} else if ( cmd.equals("/info") ) {
    		showInfo( player );
    	} 
    	
    	return proceed();
	}

	@Override
	public void printUsage(Player p) throws Exception {
		p.getChannel().writeAndFlush(getFormatter().formatResponseMessage(
				new SimpleResponse(getMessage("usageInitStateSimple"))));
	}

	@Override
	public void showInfo(Player player) throws Exception {
		KeyValueResponse<String, String> resp = new KeyValueResponse<>("");
		
		resp.put( getMessage("iRoomName"), getName() );
		
		Set<String> playerNames = getAllPlayers().keySet();
    	int cntP = 1;
    	int cntB = 1;
    	Iterator<String> iter = playerNames.iterator();
    	while ( iter.hasNext() ) {
    		FoodChainPlayer p = getFoodChainPlayer(iter.next());
    		if ( ! p.isBot() ) {
    			resp.put( getMessage("iPlayerN", cntP), p.getId() );
    			++cntP;
    		}
    	}
    	
    	iter = playerNames.iterator();
    	while ( iter.hasNext() ) {
    		FoodChainPlayer p = getFoodChainPlayer(iter.next());
    		if ( p.isBot() ) {
    			resp.put( getMessage("iBotN", cntB), p.getId() );
    			++cntB;
    		}
    	}
    	
		player.getChannel().writeAndFlush( getFormatter().formatResponseMessage(resp));
	}

}

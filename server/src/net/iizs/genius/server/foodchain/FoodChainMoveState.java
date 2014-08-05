package net.iizs.genius.server.foodchain;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.iizs.genius.server.GeniusServerException;
import net.iizs.genius.server.KeyValueResponse;
import net.iizs.genius.server.Player;
import net.iizs.genius.server.SimpleResponse;

public class FoodChainMoveState extends AbstractFoodChainState {
	public FoodChainMoveState(AbstractFoodChainState cl) {
		super(cl);
		
		++round_;
		
		broadcast( getMessage("moveStateGuide", round_ ) );
	}
	
	private AbstractFoodChainState proceed() throws Exception {
		boolean flag = true;
		
		for ( Player ap: getAllPlayers().values() ) {
			FoodChainPlayer p = (FoodChainPlayer) ap;
			if ( p.isBot() ) continue;
			if ( ! p.isAlive() ) continue;
			
			if ( p.getMoves().size() < round_ ) {
				flag = false;
				break;
			}
		}
		
		if ( flag == true ) {
			// 봇들의 선택을 랜덤으로 추가
			for ( Player ap: getAllPlayers().values() ) {
				FoodChainPlayer p = (FoodChainPlayer) ap;
				if ( p.isBot() && p.isAlive() ) {
					p.addMove(round_, p.getCharacter().getHabitat());
				}
			}
			
			// 2라운드 이후부터는 저건에 맞는 플레이어를 주 서식지로 강제 이주 
			if ( round_ > 1 ) {
				for ( Player ap: getAllPlayers().values() ) {
					FoodChainPlayer p = (FoodChainPlayer) ap;
					if ( ! p.isAlive() ) continue;
					// 이동 기록은 0-base 이므로 - 2를 해야 한다.
					if ( ! p.getMoves().get(round_ - 2).equals( p.getCharacter().getHabitat() ) ) {
						p.addMove(round_, p.getCharacter().getHabitat() );
					}
				}
			}
			
			for ( Player ap: getAllPlayers().values() ) {
				FoodChainPlayer p = (FoodChainPlayer) ap;
				if ( ! p.isAlive() ) continue;
				broadcast( getMessage( "moveResult", p.getId(), getName( p.getMoves().get( round_ - 1 ) ) ) );
			}
			
			return new FoodChainAttackState(this);
		}
		
		return this;
	}

	@Override
	public synchronized AbstractFoodChainState userCommand(Player player, String[] cmds)
			throws Exception {
    	String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/move") ) {
    		FoodChainPlayer p = getFoodChainPlayer(player.getId());
    		
    		if ( cmds.length < 2 ) {
    			printUsage(player);
    		} else {
    			if ( ! p.isAlive() ) {
    				throw new GeniusServerException( getMessage("eYouAreDead") );
    			}
    		
	    		if ( cmds[1].equals( getName( FoodChainArea.HALL ) ) ) {
	    			throw new GeniusServerException( getMessage( "eCannotMoveToHall" ) );
	    		}
	    		
	    		if ( cmds[1].equals( getName( FoodChainArea.SKY ) ) && ( ! p.getCharacter().isFlyable() ) ) {
	    			throw new GeniusServerException( getMessage( "eCannotMoveToSky" ) );
	    		}
	    		
	    		p.addMove(round_, getAreaOf(cmds[1]));
	    		p.getChannel().writeAndFlush( getFormatter().formatResponseMessage(
	    				new SimpleResponse( getMessage("moveSet", cmds[1] ) ) ) );
    		}
    	} else if ( cmd.equals("/to") ) {
    		whisper(player, cmds[1], cmds[2]);
    	} else if ( cmd.equals("/info") ) {
    		showInfo( player );
    	} 
    	
    	return proceed();
	}

	@Override
	public void printUsage(Player player) throws Exception {
		player.getChannel().writeAndFlush(getFormatter().formatResponseMessage(
				new SimpleResponse(getMessage("usageMoveStateSimple"))));
	}
	
	private String movesToString(List<FoodChainArea> moves) {
		String s = "[";
		for ( int i = 0; i < round_ - 1 && i < moves.size() ; ++i ) {
			if ( i != 0 ) {
				s += "->";
			}
			s += getName(moves.get(i));
		}
		s += "]";
		return s;
	}

	@Override
	public void showInfo(Player player) throws Exception {
		KeyValueResponse<String, String> resp = new KeyValueResponse<>("");
		
		resp.put( getMessage("iRoomName"), getName() );
		resp.put( getMessage("iRound"), Integer.toString(round_) );
		
		Set<String> playerNames = getAllPlayers().keySet();
    	int cntP = 1;
    	int cntB = 1;
    	Iterator<String> iter = playerNames.iterator();
    	while ( iter.hasNext() ) {
    		FoodChainPlayer p = getFoodChainPlayer(iter.next());
    		if ( ! p.isBot() ) {
    			String s = p.getId() 
    					+ "(" + ( p.isAlive() ? getMessage("iLive") : getMessage("iDead") ) + ")"
    					+ movesToString(p.getMoves()) ;
    			resp.put( getMessage("iPlayerN", cntP), s );
    			++cntP;
    		}
    	}
    	
    	iter = playerNames.iterator();
    	while ( iter.hasNext() ) {
    		FoodChainPlayer p = getFoodChainPlayer(iter.next());
    		if ( p.isBot() ) {
    			String s = p.getId() 
    					+ "(" + ( p.isAlive() ? getMessage("iLive") : getMessage("iDead") ) + ")"
    					+ movesToString(p.getMoves()) ;
    			resp.put( getMessage("iBotN", cntB), s );
    			++cntB;
    		}
    	}
    	
    	for ( FoodChainArea area : minimap_.keySet() ) {
    		if ( area != FoodChainArea.HALL ) {
    			resp.put( getName( area ), minimap_.get(area).toString() );
    		}
    	}
    	
		player.getChannel().writeAndFlush( getFormatter().formatResponseMessage(resp));
	}
	
	@Override
	public void chat(Player player, String msg) throws Exception {
		FoodChainPlayer p = getFoodChainPlayer(player.getId());
        for (FoodChainPlayer to: minimap_.get(p.getCurrentArea())) {
        	if ( to.isBot() ) continue;
        	to.getChannel().writeAndFlush( getFormatter().formatChatMessage(p.getId(), msg) );
        }
	}
	
	@Override
	public void whisper(Player player, String to, String msg) throws Exception {
		FoodChainPlayer p = getFoodChainPlayer(to);
		FoodChainPlayer me = getFoodChainPlayer(player.getId());
		
		if ( ! p.getCurrentArea().equals(me.getCurrentArea() ) ) {
			throw new GeniusServerException( getMessage( "eCannotWhisperToOtherArea" ) );
		}
		
		super.whisper(player, to, msg);
	}

}

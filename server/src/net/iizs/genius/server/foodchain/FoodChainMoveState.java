package net.iizs.genius.server.foodchain;

import static net.iizs.genius.server.Constants.NEWLINE;

import java.util.Iterator;
import java.util.Set;

import net.iizs.genius.server.GeniusServerException;
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
				if ( p.isBot() ) {
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
				broadcast( getMessage( "moveResult", p.getId(), p.getMoves().get( round_ - 1 ).getName() ) );
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
    		
	    		if ( cmds[1].equals(FoodChainArea.HALL.getName()) ) {
	    			throw new GeniusServerException( getMessage( "eCannotMoveToHall" ) );
	    		}
	    		
	    		if ( cmds[1].equals(FoodChainArea.SKY.getName()) && ( ! p.getCharacter().isFlyable() ) ) {
	    			throw new GeniusServerException( getMessage( "eCannotMoveToSky" ) );
	    		}
	    		
	    		p.addMove(round_, FoodChainArea.getAreaOf(cmds[1]));
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

	public void showInfo(Player player) throws Exception {
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
    		p.getChannel().write(": ");
    		p.getChannel().write( i.getMoves().subList(0, round_ - 1).toString() );
    		p.getChannel().write(": " + ( i.isAlive() ? "생존" : "죽음" ) );
    		p.getChannel().write( NEWLINE );
    	}
    	
    	p.getChannel().write( FoodChainArea.PLAINS.getName() + ": " + minimap_.get(FoodChainArea.PLAINS).toString() + NEWLINE );
    	p.getChannel().write( FoodChainArea.WOODS.getName() + ": " + minimap_.get(FoodChainArea.WOODS).toString() + NEWLINE );
    	p.getChannel().write( FoodChainArea.SKY.getName() + ": " + minimap_.get(FoodChainArea.SKY).toString() + NEWLINE );
		p.getChannel().write( FoodChainArea.RIVER.getName() + ": " + minimap_.get(FoodChainArea.RIVER).toString() + NEWLINE  );
    	
    	p.getChannel().flush();
	}
	
	@Override
	public void chat(Player player, String msg) throws Exception {
		FoodChainPlayer p = getFoodChainPlayer(player.getId());
        for (FoodChainPlayer to: minimap_.get(p.getCurrentArea())) {
        	if ( to.isBot() ) continue;
        	to.getChannel().writeAndFlush("[" + player.getId() + "] " + msg + NEWLINE);
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

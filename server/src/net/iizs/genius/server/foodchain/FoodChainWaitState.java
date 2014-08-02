package net.iizs.genius.server.foodchain;

import static net.iizs.genius.server.Constants.NEWLINE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import net.iizs.genius.server.GeniusServerException;
import net.iizs.genius.server.GeniusServerHandler;
import net.iizs.genius.server.NoBotFoundException;
import net.iizs.genius.server.Player;
import net.iizs.genius.server.SimpleResponse;

public class FoodChainWaitState extends AbstractFoodChainState {
	
	public FoodChainWaitState(GeniusServerHandler server) {
		super(server);
	}

	public FoodChainWaitState(AbstractFoodChainState c) {
		super(c);
	}

	@Override
	public void printUsage(Player p) throws Exception {
		p.getChannel().writeAndFlush(getFormatter().formatResponseMessage(
				new SimpleResponse(getMessage("usageWaitStateSimple"))));
	}
	
	@Override
	public void join(Player player) throws Exception {		
		FoodChainPlayer p = new FoodChainPlayer( player.getId(), player.getChannel() );
		if ( getAllPlayers().putIfAbsent( p.getId(), p ) != null ) {
			// TODO userid 기능이 제대로 동작한다면 불가능한 상황이다. 
			// 하지만 현재는 그렇지 않으니 방어해 둔다. 
			throw new GeniusServerException( 
					getMessage("eJoinFailed", getName() ) + "; " + getMessage("eUserIdInUse") );
		}
		getAllPlayersChannelGroup().add( p.getChannel() );
		broadcast( getMessage("join", p.getId()) );		
	}
	
	@Override
	public void quit(Player player) throws Exception {
		FoodChainPlayer p = getFoodChainPlayer(player.getId());

		getAllPlayersChannelGroup().remove( p.getChannel() );
		broadcast( getMessage("quit", p.getId()) );
	
		// 깨끗하게 퇴장
		getAllPlayers().remove( p.getId() );
	}
	
	private void addBot() throws Exception {
		String botNames[] = getMessage("botNames").split(",");
		ArrayList<String> botNameList = new ArrayList<>( Arrays.asList(botNames));
		Random rand = new Random();
		
		int pivot = rand.nextInt(botNames.length);
		botNameList.addAll( botNameList.subList(0, pivot) );
		botNameList = new ArrayList<>( botNameList.subList(pivot, botNameList.size() ) );
		
		String createdBotName = null;
		for ( String botName: botNameList ) {
			FoodChainPlayer p = new FoodChainPlayer( botName );
			if ( getAllPlayers().putIfAbsent( botName, p ) == null ) {
				createdBotName = botName;
				break;
			}
		}
		
		if ( createdBotName != null ) {
			broadcast( getMessage("botAdded", createdBotName) );
		} else {
			throw new GeniusServerException( getMessage( "eAddBotFailed" ) );
		}
		
	}
	
	private void removeBot() throws Exception {
		for ( Player p: getAllPlayers().values() ) {
			if ( p.isBot() ) {
				getAllPlayers().remove(p.getId());
				broadcast( getMessage("botRemoved", p.getId()) );
				return;
			}
		}
		throw new NoBotFoundException( getMessage( "eNoBotFound" ) );
	}
	
	@Override
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
	
	@Override
	public synchronized AbstractFoodChainState userCommand(Player p, String[] cmds) throws Exception {
    	super.userCommand(p, cmds);
    	
		String cmd = cmds[0].toLowerCase();
    	
    	if ( cmd.equals("/start") ) {
    		if ( getAllPlayers().size() < FoodChainCharacter.values().length ) {
    			throw new GeniusServerException( getMessage("eNotEnoughPlayers") );
    		}
    		
    		while ( getAllPlayers().size() > FoodChainCharacter.values().length ) {
    			try {
    				removeBot();
    			} catch ( NoBotFoundException e ) {
    				throw new NoBotFoundException( getMessage("eTooManyPlayers") );
    			}
    		}
    		
    		return new FoodChainInitState(this);
    	} else if ( cmd.equals("/add_bot") || cmd.equals("/add") ) {    		    		
    		addBot();
    	} else if ( cmd.equals("/del_bot") || cmd.equals("/del") ) {
    		removeBot();
    	}
    	
    	return this;
	}

}

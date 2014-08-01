package net.iizs.genius.server.foodchain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.iizs.genius.server.AbstractGameRoomState;
import net.iizs.genius.server.GeniusServerException;
import net.iizs.genius.server.GeniusServerHandler;
import net.iizs.genius.server.Player;

public abstract class AbstractFoodChainState extends AbstractGameRoomState {
	
	protected int round_;
	protected int kills_;
	protected Map<FoodChainArea,List<FoodChainPlayer>> minimap_;
	protected Set<FoodChainPlayer> herbivores_;
	protected Map<FoodChainCharacter,FoodChainPlayer> charmap_;
	
	public AbstractFoodChainState(GeniusServerHandler server) {
		super(server);
	}
	
	public AbstractFoodChainState(AbstractFoodChainState c) {
		super(c);
		round_ = c.round_;
		minimap_ = c.minimap_;
		herbivores_ = c.herbivores_;
		charmap_ = c.charmap_;
		kills_ = c.kills_;
	}
	
	@Override
	public void join(Player p) throws Exception {
		throw new GeniusServerException( getMessage("eJoinFailed", getName() )
				+ "; "  + getMessage("eGameIsPlaying") );
	}
	
	// 이 게임은 방에서 나가는 순간 게임을 포기하는 것으로 간주한다.
	@Override
	public void quit(Player p) throws Exception {
		this.surrender(p);
	}
	
	// 이 게임은 포기하는 순간 방에서도 퇴장당하게 된다. 
	@Override
	public void surrender(Player p) throws Exception {
		getAllPlayersChannelGroup().remove( p.getChannel() );
		broadcast( getMessage("exitGameRoom", p.getId()) );
		
		getPlayer(p.getId()).becomeBot();
		broadcast( getMessage("botReplacesPlayer", p.getId()) );
	}
	
	@Override
	public void seat(Player p) throws Exception {
		if ( getPlayer(p.getId())  != null ) {
			throw new GeniusServerException( getMessage("eAlreadySeated", getName() ) );
		}
		
		throw new GeniusServerException( getMessage("eSeatFailed", getName() )
				+ "; "  + getMessage("eGameIsPlaying") );
	}

	@Override
	public void stand(Player p) throws Exception {
		throw new GeniusServerException( getMessage("eStandNotAllowed", getName() ) );
	}

	protected FoodChainPlayer getFoodChainPlayer(String n) throws Exception {
		return (FoodChainPlayer) getPlayer(n);
	}

}

package net.iizs.genius.server.foodchain;

import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.iizs.genius.server.AbstractGameRoomState;
import net.iizs.genius.server.GeniusServerException;
import net.iizs.genius.server.Player;

public abstract class AbstractFoodChainState extends AbstractGameRoomState {
	
	protected int round_;
	protected int kills_;
	protected Map<FoodChainArea,List<FoodChainPlayer>> minimap_;
	protected Set<FoodChainPlayer> herbivores_;
	protected Map<FoodChainCharacter,FoodChainPlayer> charmap_;
	
	public AbstractFoodChainState() {
		super();
	}
	
	public AbstractFoodChainState(AbstractFoodChainState c) {
		super(c);
		round_ = c.round_;
		minimap_ = c.minimap_;
		herbivores_ = c.herbivores_;
		charmap_ = c.charmap_;
		kills_ = c.kills_;
	}
	
	public void join(String nickname, ChannelHandlerContext ctx) throws Exception {
		throw new GeniusServerException( getName() + "번 게임방에 들어갈 수 없습니다; 게임이 진행중입니다." );
	}
	
	public void quit(String nickname) throws Exception {
		Player p = getPlayer(nickname);
		
		getAllPlayersChannelGroup().remove( p.getChannel() );
		broadcast("[" + nickname + "]님이 나갔습니다.");
		
		p.becomeBot();
		broadcast("[" + nickname + "]님을 대신해서 봇이 게임을 진행합니다.");
	}
	
	protected FoodChainPlayer getFoodChainPlayer(String n) throws Exception {
		return (FoodChainPlayer) getPlayer(n);
	}

}

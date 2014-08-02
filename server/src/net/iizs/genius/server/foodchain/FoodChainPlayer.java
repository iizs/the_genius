package net.iizs.genius.server.foodchain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.iizs.genius.server.Player;
import io.netty.channel.Channel;

public class FoodChainPlayer extends Player {

	private boolean alive_;
	private FoodChainCharacter character_;
	private FoodChainCharacter selection_; // 까마귀, 카멜레온의 선택
	private Set<String> peeps_;
	private List<FoodChainArea> movingHistory_;
	private FoodChainArea currentArea_;
	private boolean passed_;
	private Set<Integer> roundAte_;
	
	public FoodChainPlayer(String n, Channel c) {
		super(n, n, c);
		
		peeps_ = new HashSet<String>();
		movingHistory_ = new ArrayList<FoodChainArea>();
		roundAte_ = new HashSet<Integer>();
	}
	
	public FoodChainPlayer(String n) {
		// bot player
		super(n);
		
		peeps_ = new HashSet<String>();
		movingHistory_ = new ArrayList<FoodChainArea>();
		roundAte_ = new HashSet<Integer>();
	}
	
	public void reset() {
		alive_ = true;
		character_ = null;
		selection_ = null;
		peeps_ = new HashSet<String>();
		movingHistory_ = new ArrayList<FoodChainArea>();
		roundAte_ = new HashSet<Integer>();
	}
	

	
	public void setCharacter(FoodChainCharacter c) {
		character_ = c;
	}
	
	public FoodChainCharacter getCharacter() {
		return character_;
	}
	
	public void setSelection(FoodChainCharacter c) {
		selection_ = c;
	}

	public FoodChainCharacter getSelection() {
		return selection_;
	}
	
	public void addPeep(String nick) throws Exception {
		if ( peeps_.size() == character_.getPeepingCount() ) {
			throw new NoMorePeepAllowedException();
		}
		if ( nick.equals(getId()) ) {
			throw new CannotPeepYourselfException();
		}
		peeps_.add(nick);
	}
	
	public Set<String> getPeeps() {
		return peeps_;
	}
	
	// round 는 1-base 이므로 index 사용시 주의 할 것
	public void addMove(int round, FoodChainArea a) {
		if ( movingHistory_.size() < round ) {
			movingHistory_.add(a);
		} else {
			movingHistory_.set(round - 1, a);
		}
	}
	
	public List<FoodChainArea> getMoves() {
		return movingHistory_;
	}
	
	public boolean isAlive() {
		return alive_;
	}
	
	public void setCurrentArea(FoodChainArea a) {
		currentArea_ = a;
	}
	
	public FoodChainArea getCurrentArea() {
		return currentArea_;
	}

	@Override
	public String toString() {
		return getId();
	}	
	
	public void setPassed(boolean b) {
		passed_ = b;
	}
	
	public boolean getPassed() {
		return passed_;
	}
	
	public void eat(FoodChainPlayer p, int round) {
		roundAte_.add( Integer.valueOf(round) );
	}
	
	public Set<Integer> getRoundsAte() {
		return roundAte_;
	}
	
	public void kill() {
		alive_ = false;
	}
}

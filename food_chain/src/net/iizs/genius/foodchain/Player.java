package net.iizs.genius.foodchain;

import java.util.HashSet;
import java.util.Set;

import io.netty.channel.Channel;

public class Player {
	private String nickname_;
	private Channel channel_;
	private Character character_;
	private Character selection_; // 까마귀, 카멜레온의 선택
	private Set<String> peeps_;
	
	public Player(String n, Channel c) {
		nickname_ = n;
		channel_ = c;
		peeps_ = new HashSet<>();
	}
	
	public Player(String n) {
		// bot player
		nickname_ = n;
		channel_ = null;
		peeps_ = new HashSet<>();
	}
	
	public boolean isBot() {
		return ( channel_ == null );
	}
	
	public void becomeBot() {
		channel_ = null;
	}
	
	public Channel getChannel() {
		return channel_;
	}
	
	public String getNickname() {
		return nickname_;
	}
	
	public void setCharacter(Character c) {
		character_ = c;
	}
	
	public Character getCharacter() {
		return character_;
	}
	
	public void setSelection(Character c) {
		selection_ = c;
	}

	public Character getSelection() {
		return selection_;
	}
	
	public void addPeep(String nick) throws Exception {
		if ( peeps_.size() == character_.getPeepingCount() ) {
			throw new GeniusServerException("더 이상 엿보기를 실행할 수 없습니다; " + peeps_.toString() );
		}
		if ( nick.equals(nickname_) ) {
			throw new GeniusServerException("자기 자신을 엿볼 수는 없습니다; " + peeps_.toString() );
		}
		peeps_.add(nick);
	}
	
	public Set<String> getPeeps() {
		return peeps_;
	}
}

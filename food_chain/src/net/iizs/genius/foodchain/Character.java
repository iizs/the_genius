package net.iizs.genius.foodchain;

import static net.iizs.genius.foodchain.Area.*;

public enum Character {
	LION("사자", 5, PLAINS, false, 1),
	CROCODILE("악어", 4, RIVER, false, 1),
	EAGLE("독수리", 3, SKY, true, 1),
	HYENA("하이에나", 2, PLAINS, false, 1),
	RABBIT("토끼", 1, WOODS, false, 1),
	MALLARD("청둥오리", 1, SKY, true, 1),
	DEER("사슴", 1, PLAINS, false, 1),
	OTTER("수달", 1, RIVER, false, 1),
	CHAMELEON("카멜레온", 1, WOODS, false, 1),
	RAT("쥐", 1, WOODS, false, 2),
	EGYPTIAN_PLOVER("악어새", 1, RIVER, true, 2),
	CROW("까마귀", 1, SKY, true, 2),
	SNAME("뱀", 0, WOODS, false, 1);
	
	private final int rank_;
	private final Area habitat_;
	private final String name_;
	private final boolean flyable_;
	private final int peepCount_;
	private Character(String n, int r, Area h, boolean f, int p) {
		rank_ = r;
		habitat_ = h;
		name_ = n;
		flyable_ = f;
		peepCount_ = p;
	}
	
	public int getRank() { return rank_; }
	public Area getHabitat() { return habitat_; }
	public String getName() { return name_; }
	public boolean isFlyable() { return flyable_; }
	public int getPeepingCount() { return peepCount_; }
}

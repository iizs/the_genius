package net.iizs.genius.server.foodchain;

import static net.iizs.genius.server.foodchain.FoodChainArea.*;

public enum FoodChainCharacter {
	LION("charLion", 5, PLAINS, false, 1) {
		@Override
		public String losingConditionMessageId() {
			return "condStarvation1";
		}
	},
	CROCODILE("charCrocodile", 4, RIVER, false, 1) {
		@Override
		public String losingConditionMessageId() {
			return "condStarvation2";
		}
	},
	EAGLE("charEagle", 3, SKY, true, 1) {
		@Override
		public String losingConditionMessageId() {
			return "condStarvation2";
		}
	},
	HYENA("charHyena", 2, PLAINS, false, 1) {
		@Override
		public String winningConditionMessageId() {
			return "condDeathOfLion";
		}
		@Override
		public String losingConditionMessageId() {
			return "condSurviveOfLion";
		}
		@Override
		public String noteMessageId() {
			return "condStarvation3";
		}
	},
	RABBIT("charRabbit", 1, WOODS, false, 1) {
		@Override
		public String noteMessageId() {
			return "noteHerbivores";
		}
	},
	MALLARD("charMallard", 1, SKY, true, 1) {
		@Override
		public String noteMessageId() {
			return "noteHerbivores";
		}
	},
	DEER("charDeer", 1, PLAINS, false, 1) {
		@Override
		public String noteMessageId() {
			return "noteHerbivores";
		}
	},
	OTTER("charOtter", 1, RIVER, false, 1) {
		@Override
		public String noteMessageId() {
			return "noteHerbivores";
		}
	},
	CHAMELEON("charChameleon", 1, WOODS, false, 1) {
		@Override
		public String noteMessageId() {
			return "noteChameleon";
		}
	},
	RAT("charRat", 1, WOODS, false, 2) {
		@Override
		public String winningConditionMessageId() {
			return "condSurviveOfLion";
		}
		@Override
		public String losingConditionMessageId() {
			return "condDeathOfLion";
		}
		@Override
		public String noteMessageId() {
			return "notePeep2";
		}
	},
	EGYPTIAN_PLOVER("charEgyptianPlover", 1, RIVER, true, 2) {
		@Override
		public String winningConditionMessageId() {
			return "condSurviveOfCrocodile";
		}
		@Override
		public String losingConditionMessageId() {
			return "condDeathOfCrocodile";
		}
		@Override
		public String noteMessageId() {
			return "notePeep2";
		}
	},
	CROW("charCrow", 1, SKY, true, 2) {
		@Override
		public String winningConditionMessageId() {
			return "condGuessingSuccess";
		}
		@Override
		public String losingConditionMessageId() {
			return "condGuessingFailure";
		}
		@Override
		public String noteMessageId() {
			return "notePeep2";
		}
	},
	SNAKE("charSnake", 0, WOODS, false, 1) {
		@Override
		public String winningConditionMessageId() {
			return "condDeathOf9OrMore";
		}
		@Override
		public String losingConditionMessageId() {
			return "condDeathOf8OrLess";
		}
		@Override
		public String noteMessageId() {
			return "noteReflect";
		}
	};
	
	private final int rank_;
	private final FoodChainArea habitat_;
	private final String id_;
	private final boolean flyable_;
	private final int peepCount_;
	
	private FoodChainCharacter(String id, int r, FoodChainArea h, boolean f, int p) {
		rank_ = r;
		habitat_ = h;
		id_ = id;
		flyable_ = f;
		peepCount_ = p;
	}
	
	public int getRank() { return rank_; }
	public FoodChainArea getHabitat() { return habitat_; }
	public String getId() { return id_; }
	public boolean isFlyable() { return flyable_; }
	public int getPeepingCount() { return peepCount_; }
	
	public String winningConditionMessageId() { return "condDefaultWin"; }
	public String losingConditionMessageId() { return "condDefaultLose"; }
	public String noteMessageId() { return "noteDefault"; }
}

package net.iizs.genius.server.foodchain;

import static net.iizs.genius.server.foodchain.FoodChainArea.*;
import net.iizs.genius.server.GeniusServerException;

public enum FoodChainCharacter {
	LION("사자", 5, PLAINS, false, 1) {
		@Override
		public String losingCondition() {
			return "한 라운드 굶으면 패배 (죽음)";
		}
	},
	CROCODILE("악어", 4, RIVER, false, 1) {
		@Override
		public String losingCondition() {
			return "총 4라운드 중 두 라운드 굶으면 패배 (죽음)";
		}
	},
	EAGLE("독수리", 3, SKY, true, 1) {
		@Override
		public String losingCondition() {
			return "총 4라운드 중 두 라운드 굶으면 패배 (죽음)";
		}
	},
	HYENA("하이에나", 2, PLAINS, false, 1) {
		@Override
		public String winningCondition() {
			return "사자의 죽음";
		}
		@Override
		public String losingCondition() {
			return "사자의 생존";
		}
		@Override
		public String note() {
			return "총 4라운드 중 세 라운드 굶으면 죽음";
		}
	},
	RABBIT("토끼", 1, WOODS, false, 1) {
		@Override
		public String note() {
			return "청둥오리, 토끼, 사슴, 수달 중 생존해있는 동물들이 한 장소에 있으면 죽지 않는다";
		}
	},
	MALLARD("청둥오리", 1, SKY, true, 1) {
		@Override
		public String note() {
			return "청둥오리, 토끼, 사슴, 수달 중 생존해있는 동물들이 한 장소에 있으면 죽지 않는다";
		}
	},
	DEER("사슴", 1, PLAINS, false, 1) {
		@Override
		public String note() {
			return "청둥오리, 토끼, 사슴, 수달 중 생존해있는 동물들이 한 장소에 있으면 죽지 않는다";
		}
	},
	OTTER("수달", 1, RIVER, false, 1) {
		@Override
		public String note() {
			return "청둥오리, 토끼, 사슴, 수달 중 생존해있는 동물들이 한 장소에 있으면 죽지 않는다";
		}
	},
	CHAMELEON("카멜레온", 1, WOODS, false, 1) {
		@Override
		public String note() {
			return "다른 플레이어가 '엿보기' 했을 때 자신의 정체를 속일 수 있음";
		}
	},
	RAT("쥐", 1, WOODS, false, 2) {
		@Override
		public String winningCondition() {
			return "사자의 승리";
		}
		@Override
		public String losingCondition() {
			return "사자의 죽음";
		}
		@Override
		public String note() {
			return "한 번에 2명 엿볼 수 있음";
		}
	},
	EGYPTIAN_PLOVER("악어새", 1, RIVER, true, 2) {
		@Override
		public String winningCondition() {
			return "악어의 승리";
		}
		@Override
		public String losingCondition() {
			return "악어의 죽음";
		}
		@Override
		public String note() {
			return "한 번에 2명 엿볼 수 있음";
		}
	},
	CROW("까마귀", 1, SKY, true, 2) {
		@Override
		public String winningCondition() {
			return "우승자 예상 후, 적중하면 승리 (캐릭터 선정 후, 우승예상자 지정)";
		}
		@Override
		public String losingCondition() {
			return "우승예상자의 패배";
		}
		@Override
		public String note() {
			return "한 번에 2명 엿볼 수 있음";
		}
	},
	SNAKE("뱀", 0, WOODS, false, 1) {
		@Override
		public String winningCondition() {
			return "종료시 9명 이상 죽으면 승리";
		}
		@Override
		public String losingCondition() {
			return "승리조건을 충족하지 못할 시";
		}
		@Override
		public String note() {
			return "공격불가. 단, 뱀을 공격하는 모든 동물 죽음";
		}
	};
	
	private final int rank_;
	private final FoodChainArea habitat_;
	private final String name_;
	private final boolean flyable_;
	private final int peepCount_;
	private FoodChainCharacter(String n, int r, FoodChainArea h, boolean f, int p) {
		rank_ = r;
		habitat_ = h;
		name_ = n;
		flyable_ = f;
		peepCount_ = p;
	}
	
	public int getRank() { return rank_; }
	public FoodChainArea getHabitat() { return habitat_; }
	public String getName() { return name_; }
	public boolean isFlyable() { return flyable_; }
	public int getPeepingCount() { return peepCount_; }
	public String winningCondition() { return "생존"; }
	public String losingCondition() { return "죽음"; }
	public String note() { return "없음"; }
	
	public static FoodChainCharacter getCharacterOf(String name) throws Exception {
		for ( FoodChainCharacter c: FoodChainCharacter.values() ) {
			if ( name.equals(c.getName()) ) {
				return c;
			}
		}
		
		throw new GeniusServerException("'" + name + "'이라는 이름의 동물은 없습니다.");
	}
}

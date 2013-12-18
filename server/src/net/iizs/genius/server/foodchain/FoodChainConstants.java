package net.iizs.genius.server.foodchain;

public interface FoodChainConstants {
	static final String WAIT_USAGE_SIMPLE = ">>> /start, /quit, /add, /del, /info, /to [닉네임] [메시지]";
	static final String INIT_USAGE_SIMPLE = ">>> /select [동물], /peep [닉네임], /info, /to [닉네임] [메시지]";
	static final String MOVE_USAGE_SIMPLE = ">>> /move [지역], /info, /to [닉네임] [메시지]";
	static final String ATTACK_USAGE_SIMPLE = ">>> /a [닉네임], /next, /info, /to [닉네임] [메시지]";
	
	static final long ONE_DAY_MILLI = 24 * 60 * 60 * 1000; 
	static final int MAX_GAME_ROUND = 4;
	static final int ATTACK_TIME_LIMIT_SECOND = 5 * 60;
	static final int KILLS_SNAKE_TO_WIN = 9;
}

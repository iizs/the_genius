package net.iizs.genius.foodchain;

public interface Constants {
	static final String NEWLINE = "\r\n";
	static final String LOBBY_USAGE_SIMPLE = ">>> /nickname [닉네임], /list, /join [방번호], /create, /bye";
	static final String LOBBY_USAGE_DETAIL = ">>> /nickname [닉네임]: 닉네임 변경, /list: 게임방 목록, /join [방번호]: 게임방 입장, /create: 게임방 생성, /bye: 종료";
	
	static final String WAIT_USAGE_SIMPLE = ">>> /start, /quit, /add_bot, /del_bot, /info, /to [닉네임] [메시지]";
	static final String INIT_USAGE_SIMPLE = ">>> /select [동물], /peep [닉네임], /info, /to [닉네임] [메시지]";
	static final String MOVE_USAGE_SIMPLE = ">>> /move [지역], /info, /to [닉네임] [메시지]";
	
	static final long ONE_DAY_MILLI = 24 * 60 * 60 * 1000; 
	static final int MAX_GAME_ROUND = 4;
}

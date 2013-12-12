package net.iizs.genius.foodchain;

import static net.iizs.genius.foodchain.Constants.*;

public class WaitingState extends GameRoomState {
	private GameRoom room;
	
	public WaitingState(GameRoom r) {
		// comment
		room = r;
	}

	public void userCommand(String nickname, String[] cmds) throws Exception {
		printUsageSimple(nickname);
	}
	
	public void printUsageSimple(String nickname) throws Exception {
		room.getPlayer(nickname).getChannel().writeAndFlush(ROOM_USAGE_SIMPLE + NEWLINE);
	}
}

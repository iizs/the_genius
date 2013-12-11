package net.iizs.genius.foodchain;

public abstract class GameRoomState {
	public abstract void userCommand(String nickname, String[] cmds) throws Exception;
	public abstract void printUsageSimple(String nickname) throws Exception;
}

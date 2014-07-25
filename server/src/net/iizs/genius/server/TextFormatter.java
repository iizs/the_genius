package net.iizs.genius.server;

public class TextFormatter extends ServerMessageFormatter {

	@Override
	public String formatWorldMessage(String msg) {
		return new String( "== " + msg + " ==");
	}

	@Override
	public String formatGameRoomMessage(String msg) {
		return new String( "<< " + msg + " >>" );
	}

	@Override
	public String formatChatMessage(String id, String msg) {
		return new String( id + " : " + msg );
	}

	@Override
	public String formatWhisperMessage(String id, String msg) {
		return new String( "** " + id + " : " + msg + " **" );
	}

	@Override
	public String formatResponseMessage(String msg) {
		return new String( "| " + msg );
	}

}

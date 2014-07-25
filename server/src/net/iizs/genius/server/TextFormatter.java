package net.iizs.genius.server;

import static net.iizs.genius.server.Constants.*;

public class TextFormatter extends ServerMessageFormatter {

	@Override
	public String formatWorldMessage(String msg) {
		return new String( "== " + msg + " ==" + NEWLINE);
	}

	@Override
	public String formatGameRoomMessage(String msg) {
		return new String( "<< " + msg + " >>" + NEWLINE );
	}

	@Override
	public String formatChatMessage(String id, String msg) {
		return new String( id + " : " + msg + NEWLINE );
	}

	@Override
	public String formatWhisperMessage(String id, String msg) {
		return new String( "** " + id + " : " + msg + " **" + NEWLINE );
	}

	@Override
	public String formatErrorMessage(String msg) {
		return new String( "!! " + msg + " !!" + NEWLINE );
	}

	@Override
	public String formatResponseMessage(String msg) {
		return new String( "| " + msg + NEWLINE );
	}

}

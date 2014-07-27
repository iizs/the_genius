package net.iizs.genius.server;

import static net.iizs.genius.server.Constants.*;

import com.google.gson.Gson;

public class TextFormatter extends ServerMessageFormatter {
	private Gson gson_;
	
	public TextFormatter() {
		gson_ = new Gson();
	}

	@Override
	public String formatWorldMessage(String msg) {
		return new String( "== " + msg + " ==" + NEWLINE);
	}

	@Override
	public String formatGameRoomMessage(String msg) {
		return new String( "<< " + msg + " >>" + NEWLINE );
	}
	
	@Override
	public String formatLobbyMessage(String msg) {
		return new String( "{{ " + msg + " }}" + NEWLINE );
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
	public String formatResponseMessage(AbstractResponse resp) {
		return new String( "| " + gson_.toJson(resp) + NEWLINE );
	}

}

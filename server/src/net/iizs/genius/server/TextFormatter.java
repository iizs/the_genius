package net.iizs.genius.server;

import static net.iizs.genius.server.Constants.*;

import java.lang.reflect.Type;
import java.util.HashMap;

public class TextFormatter extends ServerMessageFormatter {
	
	private HashMap<Type, CustomTextFormatter<?>> customFormatters_;
	
	public TextFormatter() {
		customFormatters_ = new HashMap<>();
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
		if ( customFormatters_.get( resp.getClass() ) != null ) {
			CustomTextFormatter<AbstractResponse> f = (CustomTextFormatter<AbstractResponse>) customFormatters_.get( resp.getClass() );
			return f.formatMessage(resp, resp.getClass());
		}
		return new String( "| " + resp.getMessage() + NEWLINE );
	}

	@Override
	public ServerMessageFormatter registerCustomFormatter(Type type, Object customFormatter) {
		if ( customFormatter instanceof CustomTextFormatter ) {
			customFormatters_.put(type, (CustomTextFormatter<?>) customFormatter);
		}
		return this;
	}

}

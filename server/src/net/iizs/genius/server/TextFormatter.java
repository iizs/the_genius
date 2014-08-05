package net.iizs.genius.server;

import static net.iizs.genius.server.Constants.*;

import java.lang.reflect.Type;
import java.util.HashMap;

public class TextFormatter extends ServerMessageFormatter {
	
	private HashMap<Type, CustomTextFormatter<AbstractResponse>> customFormatters_;
	
	public TextFormatter() {
		customFormatters_ = new HashMap<>();
	}
	
	private String formatMessage(String prefix, String msg, String postfix) {
		if ( msg.indexOf('\n')  < 0 ) {
			return prefix + msg + postfix + NEWLINE;
		
		}
		
		String[] msgs = msg.split("\n");
		String ret = "";
		for ( String s: msgs ) {
			ret += prefix + s + postfix + NEWLINE;
		}
		return ret;
	}

	@Override
	public String formatWorldMessage(String msg) {
		return formatMessage("== ", msg, " ==");
	}

	@Override
	public String formatGameRoomMessage(String msg) {
		return formatMessage("<< ", msg, " >>");
	}
	
	@Override
	public String formatLobbyMessage(String msg) {
		return formatMessage("{{ ", msg, " }}");
	}

	@Override
	public String formatChatMessage(String id, String msg) {
		return formatMessage( id + " : ", msg, "");
	}

	@Override
	public String formatWhisperMessage(String id, String msg) {
		return formatMessage("** " + id + " : ", msg, " **");
	}

	@Override
	public String formatErrorMessage(String msg) {
		return formatMessage("!! ", msg, " !!");
	}

	@Override
	public String formatResponseMessage(AbstractResponse resp) {
		if ( customFormatters_.get( resp.getClass() ) != null ) {
			CustomTextFormatter<AbstractResponse> f = (CustomTextFormatter<AbstractResponse>) customFormatters_.get( resp.getClass() );
			return f.formatMessage(resp, resp.getClass());
		}
		return new String( "| " + resp.getMessage() + NEWLINE );
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServerMessageFormatter registerCustomFormatter(Type type, Object customFormatter) {
		if ( customFormatter instanceof CustomTextFormatter ) {
			customFormatters_.put(type, (CustomTextFormatter<AbstractResponse>) customFormatter);
		}
		return this;
	}

}

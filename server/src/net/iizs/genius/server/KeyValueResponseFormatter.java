package net.iizs.genius.server;

import static net.iizs.genius.server.Constants.NEWLINE;

import java.lang.reflect.Type;
import java.util.List;

public class KeyValueResponseFormatter implements CustomTextFormatter<KeyValueResponse<?,?>> {

	@Override
	public String formatMessage(KeyValueResponse<?, ?> src, Type typeOfSrc) {
		List<?> keys = src.keyList();
		String resp = "";
		for ( int i=0; i < keys.size(); ++i ) {
			resp += "| " + keys.get(i).toString() + " = " + src.get( keys.get(i) ) + NEWLINE;
		}
		return resp;
	}

}

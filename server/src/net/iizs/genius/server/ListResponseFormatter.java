package net.iizs.genius.server;

import java.lang.reflect.Type;
import java.util.Iterator;
import static net.iizs.genius.server.Constants.*;

public class ListResponseFormatter implements CustomTextFormatter<ListResponse<?>> {

	@Override
	public String formatMessage(ListResponse<?> src, Type typeOfSrc) {
		Iterator<?> i = src.iterator();
		String resp = "";
		while ( i.hasNext() ) {
			resp += "| " + i.next().toString() + NEWLINE;
		}
		return resp;
	}

}

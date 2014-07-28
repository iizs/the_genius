package net.iizs.genius.server;

import java.lang.reflect.Type;
import java.util.Iterator;

public class ListResponseFormatter implements CustomTextFormatter<ListResponse> {

	@Override
	public String formatMessage(ListResponse src, Type typeOfSrc) {
		// TODO Auto-generated method stub
		Iterator<Object> i = src.iterator();
		String resp = "";
		while ( i.hasNext() ) {
			resp += i.next().toString() + " ";
		}
		return resp;
	}

}

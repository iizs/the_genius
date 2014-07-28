package net.iizs.genius.server;

import java.lang.reflect.Type;

public interface CustomTextFormatter<T> {
	public String formatMessage(T src, Type typeOfSrc);

}

package net.iizs.genius.server.foodchain;

import net.iizs.genius.server.GeniusServerException;

public class NoMorePeepAllowedException extends GeniusServerException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6649839077005489649L;

	public NoMorePeepAllowedException(String string) {
		super(string);
	}
	
	public NoMorePeepAllowedException() {
		super("");
	}

}

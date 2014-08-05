package net.iizs.genius.server.foodchain;

import net.iizs.genius.server.GeniusServerException;

public class CannotPeepYourselfException extends GeniusServerException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3672209146165298216L;

	public CannotPeepYourselfException(String string) {
		super(string);
	}
	
	public CannotPeepYourselfException() {
		super("");
	}

}

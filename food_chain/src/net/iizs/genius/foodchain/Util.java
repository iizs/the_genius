package net.iizs.genius.foodchain;

public class Util {
	private static String passchars_ = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*?-_=+";

	private static String idchars_ = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	public static String generatePassword(int length) {
		String s = "";
		
		for ( int i=0; i<length; ++i ) {
			s += passchars_.charAt( (int) (Math.random() * passchars_.length()) );
		}
		return s;
	}
	
	public static String generatePassword() {
		return generatePassword( 8 );
	}
	
	public static String generateNickname(int length) {
		String s = "";
		
		for ( int i=0; i<length; ++i ) {
			s += idchars_.charAt( (int) (Math.random() * idchars_.length()) );
		}
		return s;
	}
	
	public static String generateNickname() {
		return generateNickname( 8 );
	}

}

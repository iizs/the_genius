package net.iizs.genius.foodchain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InitState extends AbstractGameRoomState {

	public InitState() {
	}

	public InitState(AbstractGameRoomState c) {
		super(c);
		
		List<Character> chars = Arrays.asList( Character.values() );
		Collections.shuffle( chars );
		
		for ( int i=0; i < chars.size(); ++i ) {
			
		}
	}

	@Override
	public AbstractGameRoomState userCommand(String nickname, String req) throws Exception {
		// TODO Auto-generated method stub
		return this;

	}

	@Override
	public void printUsageSimple(String nickname) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void showInfo(String nickname) throws Exception {
		// TODO Auto-generated method stub

	}

}

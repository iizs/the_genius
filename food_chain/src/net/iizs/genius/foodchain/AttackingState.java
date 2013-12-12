package net.iizs.genius.foodchain;

import static net.iizs.genius.foodchain.Constants.*;

public class AttackingState extends AbstractGameRoomState {

	public AttackingState(AbstractGameRoomState cl) {
		super(cl);
		
		++round_;
		
		broadcast("각 플레이어들은 /a [닉네임] 명령으로 같은 지역에 있는 플레이어를 공격할 수 있습니다.");
		broadcast("공격 결과는 즉시 공개됩니다. 제한 시간은 " + ( ATTACK_TIME_LIMIT_SECOND / 60 ) + "분 입니다.");
		broadcast("공격 결과 판정 순서는 *입력 순서대로* 입니다.");
		broadcast("더 이상 공격을 하지 않으려 한다면 /next 명령을 입력해주세요.");
		broadcast("모든 플레이어가 /next 명령을 입력한다면, " + ( ATTACK_TIME_LIMIT_SECOND / 60 ) + "분이 지나지 않아도 다음 라운드로 진행됩니다.");
	}

	@Override
	public AbstractGameRoomState userCommand(String nickname, String req)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
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

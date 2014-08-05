package net.iizs.genius.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

public abstract class ServerMessageFormatter {
	
	// 서버에 접속해있는 모든 사람들에게 발송된는 메시지
	public abstract String formatWorldMessage(String msg);
	
	// 게임방 안의 모든 사람들에게 발송되는 메시지
	public abstract String formatGameRoomMessage(String msg);
	
	// 로비의 모든 사람들에게 발송되는 메시지
	public abstract String formatLobbyMessage(String msg);
	
	// 채팅 메시지
	public abstract String formatChatMessage(String id, String msg);
	
	// 귓말 
	public abstract String formatWhisperMessage(String id, String msg);
	
	// 사용자의 명령에 대한 응답
	public abstract String formatResponseMessage(AbstractResponse resp);
	
	// 에러 메시지
	public abstract String formatErrorMessage(String msg);
	
	// custom formatter를 추가하기 위한 method
	// ServerMessageFormatter 를 반환하는 건 Builder pattern을 만족시키기 위해서이다.
	public abstract ServerMessageFormatter registerCustomFormatter(Type type, Object customFormatter);

	public static ServerMessageFormatter getInstance(String name) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String s = ServerMessageFormatter.class.getPackage().getName() + "."  + name;
		Class<?> c = Class.forName(s);
		Constructor<?> ctor = c.getConstructor();
		Object o = ctor.newInstance();
		return (ServerMessageFormatter) o;
	}
}

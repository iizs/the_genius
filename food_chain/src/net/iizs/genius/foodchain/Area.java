package net.iizs.genius.foodchain;

public enum Area {
	PLAINS("들"),
	WOODS("숲"),
	SKY("하늘"),
	RIVER("강");
	
	private final String name_;
	private Area(String n) {
		name_ = n;
	}
	
	public String getName() { return name_; }
	
	public static Area getAreaOf(String name) throws Exception {
		for ( Area a: Area.values() ) {
			if ( name.equals(a.getName()) ) {
				return a;
			}
		}
		
		throw new GeniusServerException("'" + name + "'이라는 이름의 지역은 없습니다.");
	}
}

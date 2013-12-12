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
}

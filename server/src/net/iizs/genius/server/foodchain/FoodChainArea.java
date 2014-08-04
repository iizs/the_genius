package net.iizs.genius.server.foodchain;

public enum FoodChainArea {
	PLAINS("areaPlains"),
	WOODS("areaWoods"),
	SKY("areaSky"),
	RIVER("areaRiver"),
	HALL("areaHall");
	
	private final String id_;
	private FoodChainArea(String n) {
		id_ = n;
	}
	
	public String getId() { return id_; }

	@Override
	public String toString() {
		return id_;
	}
	
}

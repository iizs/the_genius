package net.iizs.genius.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class KeyValueResponse<K,V> extends AbstractResponse {
	private ArrayList<K> keys_;
	private HashMap<K,V> map_;

	public KeyValueResponse(String msg) {
		super(msg);
		keys_ = new ArrayList<>();
		map_ = new HashMap<>();
	}
	
	public void clear() {
		keys_.clear();
		map_.clear();
	}
	
	public V put(K key, V value) {
		if ( ! keys_.contains(key) ) {
			keys_.add(key);
		}
		return map_.put(key, value);
	}
	
	public V get(Object key) {
		return map_.get(key);
	}
	
	public List<K> keyList() {
		return Collections.unmodifiableList(keys_);
	}
}

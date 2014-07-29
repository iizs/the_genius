package net.iizs.genius.server;

import java.util.ArrayList;
import java.util.Iterator;

public class ListResponse<T> extends AbstractResponse {
	private ArrayList<T> listitems_;
	
	public ListResponse(String msg) {
		super(msg);
		listitems_ = new ArrayList<>();
	}

	public boolean add(T e) { return listitems_.add(e); }
	
	public Iterator<T> iterator() { return listitems_.iterator(); }
}

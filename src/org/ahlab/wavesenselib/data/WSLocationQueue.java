package org.ahlab.wavesenselib.data;

import java.util.LinkedList;

public class WSLocationQueue {
	private static WSLocationQueue instance = null;

	private LinkedList<Location> locationQueue;

	WSLocationQueue() {
		locationQueue = new LinkedList<Location>();
	}

	public static WSLocationQueue getInstance() {
		if (instance == null) {
			instance = new WSLocationQueue();
		}
		return instance;
	}
	
	public void enqueueLocation(Location item) {
		getInstance();
		locationQueue.addLast(item);
	}
	
	public Location dequeueLocation() {
		getInstance();
		Location location = locationQueue.poll();
		if(location == null)
			return new Location(0, 0, 0);
		return location;
	}
	
	public boolean hasLocationItems() {
		getInstance();
		return !locationQueue.isEmpty();
	}

	public int size() {
		getInstance();
		return locationQueue.size();
	}
	
	public Location getLastLocation() {
		getInstance();
		return locationQueue.peekLast();
	}
	
	public String checkLoad(){
		return "WSLocationQueueLoaded";
	}
}

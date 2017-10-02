package org.ahlab.wavesenselib.data;

import java.util.LinkedList;

public class WSQueue {
	private static WSQueue instance = null;
	private LinkedList<Integer> queue;

	WSQueue() {
		queue = new LinkedList<Integer>();
	}

	public static WSQueue getInstance() {
		if (instance == null) {
			instance = new WSQueue();
		}
		return instance;
	}

	public void enqueue(Integer item) {
		getInstance();
		queue.addLast(item);
	}

	public Integer dequeue() {
		getInstance();
		Integer gestureNumber = queue.poll();
		if(gestureNumber == null)
			return Integer.valueOf(-1);
		return gestureNumber;
	}
	
	public boolean hasItems() {
		getInstance();
		return !queue.isEmpty();
	}

	public int size() {
		getInstance();
		return queue.size();
	}
	
	public String checkLoad(){
		return "WSQueueLoaded";
	}
}

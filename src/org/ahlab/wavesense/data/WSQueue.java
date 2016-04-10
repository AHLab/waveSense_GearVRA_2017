package org.ahlab.wavesense.data;

import java.util.LinkedList;

public class WSQueue {
	private static WSQueue instance = null;
	private LinkedList<String> queue;

	WSQueue() {
		queue = new LinkedList<String>();
	}

	public static WSQueue getInstance() {
		if (instance == null) {
			instance = new WSQueue();
		}
		return instance;
	}

	public void enqueue(String item) {
		getInstance();
		queue.addLast(item);
	}

	public String dequeue() {
		getInstance();
		return queue.poll();
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

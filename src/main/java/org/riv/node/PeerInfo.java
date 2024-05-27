package org.riv.node;

public class PeerInfo {
	
	public PeerInfo(String address, String box_pub_key) {
		this.address = address;
		this.box_pub_key = box_pub_key;
	}

	private Boolean up = null;
	private String address;
	private String box_pub_key;
	private long last_seen = System.currentTimeMillis();
	
	public Boolean isUp() {
		if(up == null || up) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
	public void setUp(Boolean up) {
		this.up = up;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getBox_pub_key() {
		return box_pub_key;
	}
	public void setBox_pub_key(String box_pub_key) {
		this.box_pub_key = box_pub_key;
	}
	public long getLast_seen() {
		return last_seen;
	}
	public void setLast_seen(long last_seen) {
		this.last_seen = last_seen;
	}
}

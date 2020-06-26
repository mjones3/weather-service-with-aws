package com.example.actuatorservice;

public class Hostname {

	private final long id;
	private final String hostname;

	public Hostname(long id, String hostname) {
		this.id = id;
		this.hostname = hostname;
	}

	public long getId() {
		return id;
	}

	public String getHostname() {
		return hostname;
	}

}

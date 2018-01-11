package com.buwizz.buwizzdemo.bluetooth.commands;

public class ModeCommand implements Command {

	private int mode;

	public void setMode(int mode) {
		this.mode = mode;
	}

	@Override
	public byte[] get() {
		byte[] bytes = new byte[2];
		bytes[0] = 0x11;
		bytes[1] = (byte) mode;
		return bytes;
	}
}

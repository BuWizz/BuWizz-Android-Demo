package com.buwizz.buwizzdemo.bluetooth.commands;

public class SpeedCommand implements Command {

	private byte[] speed = new byte[4];

	public void setSpeed(int channel, double value) {
		if (channel >= 1 && channel <= 4) {
			this.speed[channel - 1] = convertToByte(value);
		}
	}

	@Override
	public byte[] get() {
		byte[] bytes = new byte[6];
		bytes[0] = 0x10;
		System.arraycopy(speed, 0, bytes, 1, 4);
		bytes[5] = 0;
		return bytes;
	}

	private byte convertToByte(double value) {
		return (byte) (value * 0x7F);
	}
}

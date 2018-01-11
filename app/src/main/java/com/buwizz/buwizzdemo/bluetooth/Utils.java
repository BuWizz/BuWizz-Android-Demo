package com.buwizz.buwizzdemo.bluetooth;

import java.util.Locale;
import java.util.UUID;

class Utils {

	static String toHexString(UUID uuid) {
		StringBuilder builder = new StringBuilder(16);
		String msbStr = Long.toHexString(uuid.getMostSignificantBits());
		if (msbStr.length() < 16) {
			int diff = 16 - msbStr.length();
			for (int i = 0; i < diff; i++) {
				builder.append('0');
			}
		}
		builder.append(msbStr);
		builder.delete(8, 16);
		builder.delete(0, 4);
		return builder.toString().toUpperCase(Locale.getDefault());
	}
}

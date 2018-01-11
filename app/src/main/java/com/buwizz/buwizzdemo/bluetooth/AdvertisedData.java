package com.buwizz.buwizzdemo.bluetooth;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class AdvertisedData {

	private final List<UUID> services;

	AdvertisedData(byte[] advertisedData) {
		services = parseUuids(advertisedData);
	}

	public List<UUID> getServices() {
		return services;
	}

	// http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation/24539704#24539704
	private List<UUID> parseUuids(byte[] advertisedData) {
		List<UUID> uuids = new ArrayList<>();

		ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
		while (buffer.remaining() > 2) {
			byte length = buffer.get();
			if (length == 0) break;

			byte type = buffer.get();
			switch (type) {
				case 0x02: // Partial list of 16-bit UUIDs
				case 0x03: // Complete list of 16-bit UUIDs
					while (length >= 2) {
						uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
						length -= 2;
					}
					break;

				case 0x06: // Partial list of 128-bit UUIDs
				case 0x07: // Complete list of 128-bit UUIDs
					while (length >= 16) {
						long lsb = buffer.getLong();
						long msb = buffer.getLong();
						uuids.add(new UUID(msb, lsb));
						length -= 16;
					}
					break;

				default:
					buffer.position(buffer.position() + length - 1);
					break;
			}
		}

		return uuids;
	}

	boolean contains(UUID service) {
		return services.contains(service);
	}
}

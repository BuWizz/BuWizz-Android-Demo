package com.buwizz.buwizzdemo.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.buwizz.buwizzdemo.bluetooth.commands.Command;
import com.buwizz.buwizzdemo.bluetooth.commands.ModeCommand;
import com.buwizz.buwizzdemo.bluetooth.commands.SpeedCommand;

import java.util.Arrays;
import java.util.UUID;

import static com.buwizz.buwizzdemo.bluetooth.Constants.DESCRIPTOR_UUID;

public class BleDevice {

	private static final String TAG = "BleDevice";

	private final BluetoothDevice bluetoothDevice;
	private final int rssi;
	private final AdvertisedData advertisedData;

	private BluetoothGatt gatt;
	private BluetoothGattCharacteristic characteristic;

	private ModeCommand modeCommand;
	private SpeedCommand speedCommand;

	private boolean isSending;
	private boolean isWaiting;

	BleDevice(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
		this.bluetoothDevice = bluetoothDevice;
		this.rssi = rssi;
		this.advertisedData = new AdvertisedData(scanRecord);
	}

	public BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}

	public boolean isAdvertisingService(String serviceUUID) {
		return isAdvertisingService(UUID.fromString(serviceUUID));
	}

	public boolean isAdvertisingService(UUID serviceUUID) {
		return advertisedData.contains(serviceUUID);
	}

	void disconnect() {
		if (gatt != null) {
			loopCommands.removeCallbacks(sendCommand);
			loopCommands = null;
			characteristic = null;
			gatt.close();
			gatt = null;
		}
	}

	public BluetoothGatt getGatt() {
		return gatt;
	}

	void setGatt(BluetoothGatt gatt, boolean notifications) {
		if (notifications) {
			gatt.setCharacteristicNotification(characteristic, true);
		}

		this.gatt = gatt;
	}

	public BluetoothGattCharacteristic getCharacteristic() {
		return characteristic;
	}

	void setCharacteristic(BluetoothGattCharacteristic characteristic) {
		characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
		this.characteristic = characteristic;
	}

	void setGattDescriptor() {
		for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
			// find descriptor UUID that matches Client Characteristic Configuration (0x2902)
			// and then call setValue on that descriptor
			String descriptorUUID = Utils.toHexString(descriptor.getUuid());
			if (!DESCRIPTOR_UUID.equals(descriptorUUID)) continue;

			boolean successSet = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			boolean successWrite = gatt.writeDescriptor(descriptor);

			Log.d(TAG, "Descriptor Set: " + successSet + " Write: " + successWrite);

			break;
		}
	}

	void setModeCommand(byte mode) {
		modeCommand = new ModeCommand();
		modeCommand.setMode(mode);
		Log.d(TAG, "Set mode: " + mode);
		startSending();
	}

	void setSpeedCommand(int channel, double value) {
		speedCommand = new SpeedCommand();
		speedCommand.setSpeed(channel, value);
	}

	private Handler loopCommands;
	private final Runnable sendCommand = new Runnable() {
		@Override
		public void run() {
			sendCommand();
		}
	};

	private void startSending() {
		if (loopCommands == null) {
			loopCommands = new Handler(Looper.myLooper());
			sendCommand.run();
		}
	}

	private void sendCommand() {
		if (characteristic != null && !isSending && !isWaiting) {
			// speed mode setting takes precedence
			byte[] bytes = null;
			if (modeCommand != null) {
				bytes = modeCommand.get();
				modeCommand = null;
				Log.d(TAG, "Apply mode: " + Arrays.toString(bytes));
			} else if (speedCommand != null) {
				bytes = speedCommand.get();
			}

			if (bytes != null) {
				characteristic.setValue(bytes);

				if (!gatt.writeCharacteristic(characteristic)) {
					isWaiting = false;
					Log.d(TAG, "Error writing characteristic! - Bytes: " + Arrays.toString(bytes));
				} else {
					isSending = true;
					isWaiting = true;

					Log.d(TAG, "Bytes: " + Arrays.toString(bytes));
					resetWaitingFlag(50);
				}
			}
		}

		if (loopCommands != null) {
			loopCommands.postDelayed(sendCommand, 10);
		}
	}

	void sent() {
		isSending = false;
	}

	private void resetWaitingFlag(long delayMillis) {
		new Handler(Looper.myLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				isWaiting = false;
			}
		}, delayMillis);
	}
}

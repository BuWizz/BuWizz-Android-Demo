package com.buwizz.buwizzdemo.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import static com.buwizz.buwizzdemo.bluetooth.Constants.CHARACTERISTIC_UUID;
import static com.buwizz.buwizzdemo.bluetooth.Constants.MAX_SCAN_TIME;
import static com.buwizz.buwizzdemo.bluetooth.Constants.REQUEST_ENABLE_BT;
import static com.buwizz.buwizzdemo.bluetooth.Constants.SERVICE_UUID;

public class BleManager {

	private static final String TAG = "BleManager";

	private final BluetoothAdapter adapter;

	private ScanListener scanListener;
	private boolean isScanning;

	private BleDevice bleDevice;
	private ConnectionState connectionState;
	private ConnectionListener connectionListener;

	public BleManager(Context context) {
		BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		adapter = btManager.getAdapter();
		connectionState = ConnectionState.DISCONNECTED;
	}

	public void enable(Activity activity) {
		enable(activity, REQUEST_ENABLE_BT);
	}

	public void enable(@NonNull Activity activity, int requestCode) {
		if (!isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableIntent, requestCode);
		}
	}

	public boolean isEnabled() {
		return adapter != null && adapter.isEnabled();
	}

	public void setScanListener(ScanListener scanListener) {
		this.scanListener = scanListener;
	}

	public void scan() {
		boolean scanningStarted = false;
		if (isEnabled() && !isScanning) {
			isScanning = true;
			scanningStarted = adapter.startLeScan(scanCallback);
			if (scanListener != null) scanListener.onScanStarted();
		}

		if (scanningStarted) {
			new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
				@Override
				public void run() {
					stopScan();
				}
			}, MAX_SCAN_TIME);
		}
	}



//	private BluetoothLeScanner leScanner;
//
//	public void scan() {
//		if (isEnabled() && !isScanning) {
//			isScanning = true;
//
//			leScanner = adapter.getBluetoothLeScanner();
//			ScanSettings settings = new ScanSettings.Builder()
//					.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//					.build();
//
//			ParcelUuid parcelUuid = ParcelUuid.fromString(SERVICE_UUID);
//			//ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(parcelUuid).build();
//			ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName("BuWizz").build();
//
//			List<ScanFilter> filters = new ArrayList<>();
//			filters.add(scanFilter);
//
//			leScanner.startScan(filters, settings, mScanCallback);
//
//			if (scanListener != null) scanListener.onScanStarted();
//		}
//
//		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				stopScan();
//			}
//		}, MAX_SCAN_TIME);
//	}
//
//	private ScanCallback mScanCallback = new ScanCallback() {
//		@Override
//		public void onScanResult(int callbackType, ScanResult result) {
//			Log.i("callbackType", String.valueOf(callbackType));
//			Log.i("result", result.toString());
//
//			if (scanListener != null) {
//				bleDevice = new BleDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
//				scanListener.onDeviceDiscovered(bleDevice);
//			}
//
//		}
//
//		@Override
//		public void onBatchScanResults(List<ScanResult> results) {
//			for (ScanResult sr : results) {
//				Log.i("ScanResult - Results", sr.toString());
//			}
//		}
//
//		@Override
//		public void onScanFailed(int errorCode) {
//			Log.e("Scan Failed", "Error Code: " + errorCode);
//		}
//	};
//
//	private void stopScan() {
//		if (isEnabled() && isScanning) {
//			leScanner.stopScan(mScanCallback);
//			isScanning = false;
//			if (scanListener != null) scanListener.onScanStopped();
//		}
//	}



	public void stopScan() {
		if (isEnabled() && isScanning) {
			adapter.stopLeScan(scanCallback);
			isScanning = false;
			if (scanListener != null) scanListener.onScanStopped();
		}
	}

	public void setConnectionListener(ConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
	}

	private void notifyConnectionListener(final ConnectionState connectionState) {
		this.connectionState = connectionState;

		if (connectionListener != null) {
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					connectionListener.onConnectionStateChange(connectionState);
				}
			});
		}
	}

	public void connect(Context context, String deviceAddress) {
		BluetoothDevice device = getDevice(deviceAddress);
		if (device == null) return;

		notifyConnectionListener(ConnectionState.CONNECTING);

		BluetoothGatt gatt;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
		} else {
			gatt = device.connectGatt(context, false, gattCallback);
		}

		Log.d(TAG, "connect uuid: " + gatt.getDevice().getAddress());
	}

	public void disconnect() {
		notifyConnectionListener(ConnectionState.DISCONNECTING);

		if (bleDevice != null) {
			bleDevice.disconnect();
			notifyConnectionListener(ConnectionState.DISCONNECTED);
		}
	}

	private BluetoothDevice getDevice(String deviceAddress) {
		if (adapter != null && !TextUtils.isEmpty(deviceAddress)) {
			return adapter.getRemoteDevice(deviceAddress);
		}
		return null;
	}

	private final BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			if (scanListener != null) {
				bleDevice = new BleDevice(device, rssi, scanRecord);
				scanListener.onDeviceDiscovered(bleDevice);
			}
		}
	};

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
//			// this will get called anytime you perform a read or write characteristic operation
//			BuWizzDevices.BuWizzDevice device = _buwizzDevices.getDevice(gatt);
//			BuWizzResponse.BuWizzMode previousMode = device.getLastResponse().getMode();
//
//			device.updateResponse(characteristic.getValue());
//			BuWizzResponse lastResponse = device.getLastResponse();
//			BuWizzResponse.BuWizzMode lastMode = lastResponse.getMode();
//
//			MagicUtils.logd(TAG, "Received: " + device.bytesToString(characteristic.getValue()) + " BuWizz: " + lastResponse);
//
////			// ker po začetem firmware upload-u ne dobivamo več odziva od BuWizz-a, iz katerega bi
////			// lahko prebrali mode (Bootloader ali Firmware), si vodimo lastno evidenco mode-a in
////			// zato ne pustimo, da nam response to povozi
////			if (Arrays.asList(BuWizzResponse.BuWizzMode.WillUpdateFirmware, BuWizzResponse.BuWizzMode.UpdatingFirmware).contains(previousMode)) {
////				device.getLastResponse().setMode(previousMode);
////				lastMode = previousMode;
////			}
////
////			if (_firmwareUploader.isReady()) {
////				if (_firmwareUploader.getFirmwareVersion() > lastResponse.getFirmwareVersion() && lastMode == BuWizzResponse.BuWizzMode.Firmware && !device.hasFirmwareUploadStarted()) {
//////					if (!_updateStartedOnce) {
//////						_updateStartedOnce = true;
////					device.startFirmwareUpload(_firmwareUploader);
////					notifyListeners(ListenerNotificationType.DeviceNeedsFirmwareUpdate, gatt.getDevice());
//////					}
////				}
////
////				if (lastMode == BuWizzResponse.BuWizzMode.Bootloader && !device.hasFirmwareUploadStarted()) {
//////					_updateStartedOnce = true;
////					notifyListeners(ListenerNotificationType.DeviceNeedsFirmwareUpdate, gatt.getDevice());
////				}
////
////				switch (lastMode) {
////					case Bootloader:
////					case WillUpdateFirmware:
////					case UpdatingFirmware:
////						device.processFirmwareUploadResponse(_firmwareUploader, characteristic.getValue(), _uploadProgress);
////						break;
////				}
////			}
//
//			notifyListeners(ListenerNotificationType.DeviceResponseReceived, gatt.getDevice(), device.getLastResponse());
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicRead(gatt, characteristic, status);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			Log.d(TAG, "onCharacteristicWrite status: " + status);

			if (bleDevice != null) {
				bleDevice.sent();
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);

			if (status == BluetoothGatt.GATT_SUCCESS) {
//				Log.d(TAG, "Success writing descriptor (" + BuWizzDevices.getFriendlyName(gatt.getDevice()) + ")");
				Log.d(TAG, "Success writing descriptor (" + gatt.getDevice() + ")");
			} else {
				Log.d(TAG, "Error writing descriptor (" + gatt.getDevice() + ")");
			}
		}

		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
			// this will get called when a device connects or disconnects
			Log.d(TAG, "onConnectionStateChange uuid:" + gatt.getDevice().getAddress() + " s:" + status);

			if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				notifyConnectionListener(ConnectionState.DISCONNECTED);
			}

			// define-i za statuse: https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-4.4.4_r2.0.1/stack/include/gatt_api.h
			// ideje za ta error:
			// 1. mogoče se 133 zgodi, če se dva device connect-a kličeta prehitro eden za drugim
			// 2. nekateri pravijo, da je na nekaterih napravah zgleda potrebno klicati connect na UI thread-u: http://stackoverflow.com/a/30356557
			// 3. baje da je error 133, če pride do timeout-a pri connect-u, zato klic spodaj (glej https://code.google.com/p/android/issues/detail?id=69834)
			// 4. obstaja nek class za potencialno rešitev: http://stackoverflow.com/a/39269766
			// 5. za hitro odpiranje in zapiranje connection-a: http://stackoverflow.com/a/35935762
			if (status == 133 && newState == BluetoothProfile.STATE_DISCONNECTED) { // Gatt error
				gatt.close();

				// tole je na predlog iz: https://code.google.com/p/android/issues/detail?id=69834 (comment #24)
				Log.d(TAG, "trying reconnect uuid:" + gatt.getDevice().getAddress() + " s:" + status);
				new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
					@Override
					public void run() {
						gatt.connect();
					}
				}, 200);
				return;
			}

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.d(TAG, "Connected to device: " + gatt.getDevice() + ". Discovering services.");
				gatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.d(TAG, "Disconnected from device: " + gatt.getDevice());
			}
		}

		@Override
		public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
			// this will get called after the client initiates a BluetoothGatt.discoverServices() call
			UUID validServiceUUID = UUID.fromString(SERVICE_UUID);

			List<BluetoothGattService> services = gatt.getServices();
			for (BluetoothGattService service : services) {
				if (!service.getUuid().equals(validServiceUUID)) continue;

				List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

				for (BluetoothGattCharacteristic characteristic : characteristics) {
					String characteristicUUID = Utils.toHexString(characteristic.getUuid());
					if (!CHARACTERISTIC_UUID.equals(characteristicUUID)) continue;

					bleDevice.setCharacteristic(characteristic);
					bleDevice.setGatt(gatt, true);

					bleDevice.setGattDescriptor();

					notifyConnectionListener(ConnectionState.CONNECTED);

					break;
				}

				break;
			}
		}
	};

	public void sendModeCommand(int mode) {
		if (bleDevice != null) {
			bleDevice.setModeCommand((byte) mode);
		}
	}

	public void sendSpeedCommand(int channel, double value) {
		if (bleDevice != null) {
			bleDevice.setSpeedCommand(channel, value);
		}
	}


	public interface ScanListener {

		void onScanStarted();

		void onScanStopped();

		void onDeviceDiscovered(BleDevice bleDevice);
	}


	public interface ConnectionListener {

		void onConnectionStateChange(ConnectionState connectionState);
	}
}

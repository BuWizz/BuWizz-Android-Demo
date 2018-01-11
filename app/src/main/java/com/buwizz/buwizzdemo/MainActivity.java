package com.buwizz.buwizzdemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.buwizz.buwizzdemo.bluetooth.BleDevice;
import com.buwizz.buwizzdemo.bluetooth.BleManager;
import com.buwizz.buwizzdemo.bluetooth.ConnectionState;
import com.buwizz.buwizzdemo.controls.DrivePad;

import static com.buwizz.buwizzdemo.bluetooth.Constants.REQUEST_ENABLE_BT;
import static com.buwizz.buwizzdemo.bluetooth.Constants.SERVICE_UUID;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";

	private BleManager bleManager;

	private Button connect;
	private Button[] speedButtons;
	private Button[] channelButtons;
	private TextView speedTitle;
	private TextView channelTitle;
	private DrivePad pad;

	private int selectedChannel = 1;

	private ArrayAdapter<String> foundDevices;
	private final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			final String deviceAddress = foundDevices.getItem(which);
			AlertDialog.Builder builderInner = new AlertDialog.Builder(MainActivity.this);
			builderInner.setMessage(deviceAddress);
			builderInner.setTitle("Your selected BuWizz is");
			builderInner.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,int which) {
					bleManager.connect(MainActivity.this, deviceAddress);
					dialog.dismiss();
				}
			});
			builderInner.show();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		foundDevices = new ArrayAdapter<>(this, R.layout.dialog_scan);

		bleManager = new BleManager(this);
		bleManager.setScanListener(new BleManager.ScanListener() {
			@Override
			public void onScanStarted() {
				foundDevices.clear();
			}

			@Override
			public void onScanStopped() {
				Toast.makeText(MainActivity.this, "Scanning stopped!", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onDeviceDiscovered(BleDevice bleDevice) {
				String deviceAddress = bleDevice.getBluetoothDevice().getAddress();
				if (foundDevices.getPosition(deviceAddress) < 0 && bleDevice.isAdvertisingService(SERVICE_UUID)) {
					foundDevices.add(deviceAddress);
					foundDevices.notifyDataSetChanged();
				}
			}
		});

		bleManager.setConnectionListener(new BleManager.ConnectionListener() {
			@Override
			public void onConnectionStateChange(ConnectionState connectionState) {
				switch (connectionState) {
					case DISCONNECTED:
						connect.setText("Connect");
						connect.setEnabled(true);
						connect.setOnClickListener(connectClickListener);
						break;
					case CONNECTING:
						connect.setText("Connecting");
						connect.setEnabled(false);
						break;
					case CONNECTED:
						connect.setText("Disconnect");
						connect.setEnabled(true);
						connect.setOnClickListener(disconnectClickListener);
						break;
					case DISCONNECTING:
						connect.setEnabled(false);
						break;
				}

				enableElements(connectionState == ConnectionState.CONNECTED);
			}
		});

		setupConnectButton();
		setupTitles();
		setupSpeedButtons();
		setupChannelButtons();
		setupDrivePad();

		enableElements(false);
	}

	private void setupConnectButton() {
		connect = (Button) findViewById(R.id.btn_connect);
		connect.setOnClickListener(connectClickListener);
	}

	private View.OnClickListener connectClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			openDialog();

			if (bleManager.isEnabled()) {
				bleManager.scan();
			} else {
				bleManager.enable(MainActivity.this);
			}
		}
	};

	private View.OnClickListener disconnectClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			bleManager.disconnect();
		}
	};

	private void setupTitles() {
		speedTitle = (TextView) findViewById(R.id.txt_speed);
		channelTitle = (TextView) findViewById(R.id.txt_channel);

		updateSpeedTitle(0);
		updateChannelTitle(selectedChannel);
	}

	private void updateSpeedTitle(int speed) {
		speedTitle.setText("Speed: " + speed);
	}

	private void updateChannelTitle(int channel) {
		channelTitle.setText("Channel: " + channel);
	}

	private void setupSpeedButtons() {
		speedButtons = new Button[5];
		speedButtons[0] = (Button) findViewById(R.id.btn_speed_0);
		speedButtons[1] = (Button) findViewById(R.id.btn_speed_1);
		speedButtons[2] = (Button) findViewById(R.id.btn_speed_2);
		speedButtons[3] = (Button) findViewById(R.id.btn_speed_3);
		speedButtons[4] = (Button) findViewById(R.id.btn_speed_4);

		for (int i = 0; i < speedButtons.length; ++i) {
			setSpeedButtonClickListener(speedButtons[i], i);
		}
	}

	private void setupChannelButtons() {
		channelButtons = new Button[4];
		channelButtons[0] = (Button) findViewById(R.id.btn_channel_1);
		channelButtons[1] = (Button) findViewById(R.id.btn_channel_2);
		channelButtons[2] = (Button) findViewById(R.id.btn_channel_3);
		channelButtons[3] = (Button) findViewById(R.id.btn_channel_4);

		for (int i = 0; i < channelButtons.length; ++i) {
			setChannelButtonClickListener(channelButtons[i], i + 1);
		}
	}

	private void setSpeedButtonClickListener(Button button, final int mode) {
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bleManager.sendModeCommand(mode);
				updateSpeedTitle(mode);
			}
		});
	}

	private void setChannelButtonClickListener(Button button, final int channel) {
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (channel >= 1 && channel <= 4) {
					selectedChannel = channel;
					updateChannelTitle(channel);
				}
			}
		});
	}

	private void setupDrivePad() {
		pad = (DrivePad) findViewById(R.id.drive_pad);
		pad.setMotionListener(new DrivePad.MotionListener() {
			@Override
			public void onMotion(double x, double y) {
				Log.d(TAG, "x: " + x + " y: " + y);
				bleManager.sendSpeedCommand(selectedChannel, y);
			}
		});
	}

	private void enableElements(boolean enable) {
		for (Button button : speedButtons) {
			button.setEnabled(enable);
		}

		for (Button button : channelButtons) {
			button.setEnabled(enable);
		}

		pad.setEnabled(enable);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
			bleManager.scan();
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void openDialog() {
		AlertDialog.Builder deviceSelector = new AlertDialog.Builder(MainActivity.this);
		deviceSelector.setIcon(R.mipmap.ic_launcher);
		deviceSelector.setTitle("Select a BuWizz:");

		deviceSelector.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		deviceSelector.setAdapter(foundDevices, dialogClickListener);
		deviceSelector.show();
	}
}

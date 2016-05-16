package com.example.myobotcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BT_Comm {

	// MAC adress of the bot
	final String nxtAdr = "00:16:53:1A:D6:DF"; 
	
	// Blutooth device of the phone
	BluetoothAdapter localAdapter;
	
	//Test
	
	// Blutooth connection
	BluetoothSocket socket_nxt;

	// Enable Blutooth on the phone
	public void enableBT() {
		localAdapter = BluetoothAdapter.getDefaultAdapter();

		if (!localAdapter.isEnabled()) {
			localAdapter.enable();
			while (!localAdapter.isEnabled());
		}
	}

	// Bot connexion
	public boolean connectToNXT() {
		boolean res;
		BluetoothDevice nxt = localAdapter.getRemoteDevice(nxtAdr);

		try {
			socket_nxt = nxt.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			socket_nxt.connect();
			res = true;

		} catch (IOException e) {
			Log.d("Bluetooth", "Err: Device not found or cannot connect");
			res = false;
		}

		return res;
	}

	// Send a message
	public void writeMessage(byte[] buffer) throws InterruptedException {
		try {

			OutputStream out = socket_nxt.getOutputStream();
			out.write(buffer);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Receive a message
	public byte[] readMessage(int nb) {
		byte buffer[] = new byte[16];

		try {
			InputStream in = socket_nxt.getInputStream();

			for (int i = 0; i < nb; i++) {
				buffer[i] = (byte) in.read();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return buffer;
	}
}
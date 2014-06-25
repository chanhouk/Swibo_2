package com.example.swibo_2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DecimalFormat;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.util.Log;

public class SensorService extends Service implements SensorEventListener {

	private static final String TAG = SensorService.class.getName();
	public final static String MY_ACTION = "MY_ACTION";

	private SensorManager serviceSensorManager = null;
	private WakeLock mWakeLock = null;

	private float[] mOrientation;

	public static double test;

	private String tempAddress;
	private int tempPort;

	private static String data;

	private DatagramPacket packet;

	private InetAddress serverAddr = null;
	private DatagramSocket socket = null;

	private float[] mGravity;
	private float[] mGeomagnetic;

	private Float pitch = null;
	private Float roll = null;

	public SensorService(){}

	/*
	 * Note:
	 * add a time in activity to pass speed to service for sensor delay time.
	 */

	/**
	 * Register this as a sensor event listener.
	 * option:
	 * 	SENSOR_DELAY_NORMAL		600,000 microseconds
	 * 	SENSOR_DELAY_UI			60,000 microseconds
	 * 	SENSOR_DELAY_GAME 		20,000 microseconds
	 * 	SENSOR_DELAY_FASTEST	0 MICROSECOND
	 * @param x
	 */
	private void registerListener() {
		serviceSensorManager.registerListener(this,serviceSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_GAME);
		serviceSensorManager.registerListener(this,serviceSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
		serviceSensorManager.registerListener(this,serviceSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_GAME);
	}

	/*
	 * Un-register this as a sensor event listener.
	 */
	private void unregisterListener() {
		serviceSensorManager.unregisterListener(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		serviceSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
	}

	@Override
	public void onDestroy() {
		unregisterListener();
		mWakeLock.release();
		stopForeground(true);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		// ========== ========== ========== OLD ========== ========== ==========
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION){
			mOrientation = event.values;
		}


		if(mOrientation != null){
			DecimalFormat floatFormat = new DecimalFormat("###.###");
			data = floatFormat.format(mOrientation[1])+","+floatFormat.format(mOrientation[2]);
		}
		// ========== ========== ========== ========== ========== ========== ==========

		// ========== ========== ========== NEW ========== ========== ==========
//		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
//			mGravity = event.values;
//		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
//			mGeomagnetic = event.values;
//		if (mGravity != null && mGeomagnetic != null) {
//			float R[] = new float[9];
//			float I[] = new float[9];
//			boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
//			if (success) {
//				//				float orientation[] = new float[3];
//				//				SensorManager.getOrientation(R, orientation);
//				//				Float pitch = orientation[1];
//				//				Float roll = orientation[2];
//				//				data = String.valueOf(pitch)+","+String.valueOf(roll);
//				if (event.sensor.getType() == Sensor.TYPE_ORIENTATION){
//					mOrientation = event.values;
//				}
//				if(mOrientation != null){
//					DecimalFormat floatFormat = new DecimalFormat("###.###");
//					data = floatFormat.format(mOrientation[1])+","+floatFormat.format(mOrientation[2]);
//				}
//			}
//		}
		// ========== ========== ========== ========== ========== ========== ==========
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		startForeground(Process.myPid(), new Notification());
		mWakeLock.acquire();	// Create a lock to keep service to run until force to exit
		registerListener();

		if(MainActivity.streamState()){
			Bundle b = intent.getExtras();
			tempAddress = b.getString("address");
			tempPort = Integer.parseInt(b.getString("port"));
			// Debug
//			Log.i(TAG, tempAddress + ","+ tempPort);

			try {
				serverAddr = InetAddress.getByName(tempAddress);
				socket = new DatagramSocket();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Error while creating socket");
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG,"Error while creating ip-address");
				e.printStackTrace();
			}
			new Thread(new Client()).start();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	public class Client implements Runnable {

		@Override 
		public void run() {
			System.out.println("thread begin");
			byte[] buf = new byte[256]; 
			while(MainActivity.streamState())
			{ 				
				try {
					Thread.sleep(30);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

				try {
					// Debug
//					Log.i(TAG, "data: "+ data);

					buf = data.getBytes();	// This turn any data into bytes before sending with UDP
					packet = new DatagramPacket(buf, buf.length, serverAddr, tempPort); 
					socket.send(packet);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e(TAG,"Error while sending packet to target");
					e.printStackTrace();
				} 

			} 
			System.out.println("Thread ended");
		}

	}
}

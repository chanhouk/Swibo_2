package com.example.swibo_2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.preference.PreferenceManager;
import android.provider.Settings;

public class MainActivity extends ActionBarActivity {

	private EditText ipEditText;

	private TextView p1TextView;
	private TextView p2TextView;

	private ToggleButton p1;
	private ToggleButton p2;
	private ToggleButton sTB;

	private String SERVERIP;
	private String SERVERPORT;

	private SharedPreferences SP;

	private static boolean streamOn;
	private static boolean usingP1;
	private static boolean usingP2;

	private static Pattern VALID_IPV4_PATTERN = null;
	private static Pattern VALID_IPV6_PATTERN = null;
	private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
	private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

	private ConnectivityManager connManager;
	private NetworkInfo mWifi;
	
	private Intent intent;

	private final String swiboSite = "http://www.swibo.co.nz/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
		SP = PreferenceManager.getDefaultSharedPreferences(this);

		intent = new Intent(MainActivity.this,SensorService.class);
		startService(intent);

		usingP1 = true;
		usingP2 = false;

		connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()){

		}else{
			connectionDialogue();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_ip) {
			editIPDialog();
			return true;
		}
		if (id == R.id.action_player1) {
			editP1PortDialog();
			return true;
		}
		if (id == R.id.action_player2){
			editP2PortDialog();
			return true;
		}
		if (id == R.id.action_website){
			visitSite();
			return true;
		}
		if (id == R.id.action_setDefault){
			setDefault();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Stream toggle button
	 * @param view
	 */
	public void stream(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {

			if(usingP1){
				SERVERPORT = SP.getString("player1Port", "");
			}
			if(usingP2){
				SERVERPORT = SP.getString("player2Port", "");
			}
			// Debug
//			System.out.println("SERVERIP: "+ SERVERIP + " SERVERPORT: "+SERVERPORT);

			if(SERVERIP.equals("") || SERVERPORT.equals("")){
				Toast toast = Toast.makeText(MainActivity.this,"Need to have IP and Port.\n(use MENU to edit!)", Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
				sTB.setChecked(false);
				return;
			}

			p1.setEnabled(false);
			p2.setEnabled(false);
			sTB.setTextColor(Color.RED);

//			Intent intent = new Intent(MainActivity.this, SensorService.class);        
			Bundle b = new Bundle();
			b.putString("address", SERVERIP);
			b.putString("port",SERVERPORT);
			intent.putExtras(b);
			streamOn = true;
			startService(intent);
			
		}else{
			p1.setEnabled(usingP2);
			p2.setEnabled(usingP1);
			sTB.setTextColor(Color.BLACK);

			Intent intent = new Intent(MainActivity.this,SensorService.class);
			stopService(intent);
			
			streamOn = false;
		}
	}

	/**
	 * Player 1 toggle button
	 */
	public void player1ToggleButton(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			usingP1 = true;
			usingP2 = false;
			p1.setEnabled(false);
			p2.setEnabled(true);
			p1.setChecked(true);
			p2.setChecked(false);
			p1.setTextColor(Color.RED);
			p2.setTextColor(Color.BLACK);
		}else{

		}
	}

	/**
	 * Player 2 toggle button
	 */
	public void player2ToggleButton(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			usingP1 = false;
			usingP2 = true;
			p1.setEnabled(true);
			p2.setEnabled(false);
			p1.setChecked(false);
			p2.setChecked(true);
			p1.setTextColor(Color.BLACK);
			p2.setTextColor(Color.RED);
		}else{

		}
	}
	
	/**
	 *  Back button
	 */
	@Override
	public void onBackPressed() {
		if(!streamState()){
			super.onBackPressed();
			stopService(new Intent(this, SensorService.class));
			finish();
			return;
		}else{
			Toast toast = Toast.makeText(MainActivity.this,"Streaming (Cannot exit apps)", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		private EditText IFET;
		private TextView P1FTV;
		private TextView P2FTV;
		private ToggleButton P1FTB;
		private ToggleButton P2FTB;
		private ToggleButton STB;

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);

			IFET = (EditText) rootView.findViewById(R.id.ipAddressTextView); 
			IFET.setEnabled(false);

			P1FTV = (TextView) rootView.findViewById(R.id.Player1PortTextView);
			P2FTV = (TextView) rootView.findViewById(R.id.Player2PortTextView);

			P1FTB = (ToggleButton) rootView.findViewById(R.id.TogglePlayer1);
			P1FTB.setEnabled(false);

			P2FTB = (ToggleButton) rootView.findViewById(R.id.TogglePlayer2);

			STB = (ToggleButton) rootView.findViewById(R.id.streamToggleButton);

			((MainActivity) rootView.getContext()).setUpView(IFET, P1FTB, P2FTB, STB, P1FTV, P2FTV);

			return rootView;
		}
	}

	/**
	 * To pass element from fragment to Activity
	 */
	public void setUpView(EditText ip, ToggleButton TB1, ToggleButton TB2, ToggleButton TB3, TextView TV1, TextView TV2) {
		// TODO Auto-generated method stub
		this.ipEditText = ip;

		this.p1 = TB1;
		this.p2 = TB2;
		this.sTB = TB3;

		this.p1TextView = TV1;
		this.p2TextView = TV2;

		
		intent = new Intent(MainActivity.this,SensorService.class);
//		startService(intent);
		
		loadSavedPreferences();

		if(SP.getString("ip", "") == "" || SP.getString("ip", "") == null){
			setDefault();
		}
	}

	private void loadSavedPreferences() {
		loadIPAddress();
		loadP1Port();
		loadP2Port();
	}

	private void savePreferences(String key, String value) {
		Editor editor = SP.edit();
		editor.putString(key, value);
		editor.commit();
	}

	private void loadIPAddress() {
		SERVERIP = SP.getString("ip", "");;
		ipEditText.setText(SERVERIP);
	}

	private void loadP1Port() {
		p1TextView.setText(SP.getString("player1Port", ""));
	}

	private void loadP2Port() {
		p2TextView.setText(SP.getString("player2Port", ""));
	}

	private void editIPDialog() {
		if(!streamState()){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Edit IP-Address").setTitle("IP-ADDRESS");

			final EditText ipInput = new EditText(MainActivity.this);
			LinearLayout.LayoutParams InputAddress = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);

			ipInput.setLayoutParams(InputAddress);
			ipInput.setSingleLine(true);
			ipInput.setText("192.168.");
			builder.setView(ipInput);

			builder.setPositiveButton("Save",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// show network settings

					if(isIpAddress(ipInput.getText().toString())){
						savePreferences("ip", ipInput.getText().toString());
						loadIPAddress();
						dialog.cancel();
					}else{
						Toast toast = Toast.makeText(MainActivity.this,"Incorrect IP-Address\n(Example: 192.168.1.1)", Toast.LENGTH_LONG);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
						editIPDialog();
					}
				}
			});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// dismiss
					dialog.cancel();
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
		}else{
			Toast toast = Toast.makeText(MainActivity.this,"Streaming (No editing allow)", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}

	private void editP1PortDialog() {
		if(!streamState()){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Edit player 1 port:").setTitle("Player 1");

			final EditText portInput = new EditText(MainActivity.this);
			LinearLayout.LayoutParams InputPort = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);

			portInput.setLayoutParams(InputPort);
			portInput.setSingleLine(true);
			builder.setView(portInput);

			builder.setPositiveButton("Save",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(isNumeric(portInput.getText().toString())){
						String temp = p2TextView.getText().toString();
						String temp2 = portInput.getText().toString();
						System.out.println(temp + " " + temp2);

						if(portCompare(temp, portInput.getText().toString())){
							savePreferences("player1Port", portInput.getText().toString());
							loadP1Port();
							dialog.cancel();
						}else{
							Toast toast = Toast.makeText(MainActivity.this,"Port being used by player 2", Toast.LENGTH_LONG);
							toast.setGravity(Gravity.CENTER, 0, 0);
							toast.show();
							editP1PortDialog();
						}
					}else{
						Toast toast = Toast.makeText(MainActivity.this,"PORT must be integer value\n(Example: 9876)", Toast.LENGTH_LONG);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
						editP1PortDialog();
					}
				}
			});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// dismiss
					dialog.cancel();
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
		}else{
			Toast toast = Toast.makeText(MainActivity.this,"Streaming (No editing allow)", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}

	private void editP2PortDialog() {
		if(!streamState()){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Edit player 2 port:").setTitle("Player 2");

			final EditText portInput = new EditText(MainActivity.this);
			LinearLayout.LayoutParams InputPort = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);

			portInput.setLayoutParams(InputPort);
			portInput.setSingleLine(true);
			builder.setView(portInput);

			builder.setPositiveButton("Save",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(isNumeric(portInput.getText().toString())){
						if(portCompare(SP.getString("player1Port", ""), portInput.getText().toString())){
							savePreferences("player2Port", portInput.getText().toString());
							loadP2Port();
							dialog.cancel();
						}else{
							Toast toast = Toast.makeText(MainActivity.this,"Port being used by player 1", Toast.LENGTH_LONG);
							toast.setGravity(Gravity.CENTER, 0, 0);
							toast.show();
							editP2PortDialog();
						}
					}else{
						Toast toast = Toast.makeText(MainActivity.this,"PORT must be integer value\n(Example: 9876)", Toast.LENGTH_LONG);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
						editP2PortDialog();
					}
				}
			});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// dismiss
					dialog.cancel();
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
		}else{
			Toast toast = Toast.makeText(MainActivity.this,"Streaming (No editing allow)", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}

	private void connectionDialogue() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Connect to a wifi network now?\n(Need to be the network the game running on!)").setTitle("No wifi Connection.");
		builder.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// show network settings
				dialog.dismiss();
				Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
				startActivity(intent);
			}
		});
		builder.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// dismiss
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void setDefault() {
		if(!streamState()){
			savePreferences("ip", "192.168.1.100");
			savePreferences("player1Port", "5555");
			savePreferences("player2Port", "5556");
			loadSavedPreferences();
		}else{
			Toast toast = Toast.makeText(MainActivity.this,"Streaming (No editing allow)", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}

	public static boolean isNumeric(String str) {  
		// if anything goes wrong try will catch exception and send false back
		try  
		{  
			Double.parseDouble(str);
		}  
		catch(NumberFormatException nfe)  
		{  
			return false;  
		}  
		return true;  
	}

	/**
	 * This class provides a variety of basic utility methods that are not
	 * dependent on any other classes within the org.jamwiki package structure.
	 */

	static {
		try {
			VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
			VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
			//logger.severe("Unable to compile pattern", e);
		}
	}

	/**
	 * Determine if the given string is a valid IPv4 or IPv6 address.  This method
	 * uses pattern matching to see if the given string could be a valid IP address.
	 *
	 * @param ipAddress A string that is to be examined to verify whether or not
	 *  it could be a valid IP address.
	 * @return <code>true</code> if the string is a value that is a valid IP address,
	 *  <code>false</code> otherwise.
	 */
	public static boolean isIpAddress(String ipAddress) {

		Matcher m1 = VALID_IPV4_PATTERN.matcher(ipAddress);
		if (m1.matches()) {
			return true;
		}
		Matcher m2 = VALID_IPV6_PATTERN.matcher(ipAddress);
		return m2.matches();
	}

	public static boolean portCompare(String s1, String s2) {
		if(Double.parseDouble(s1) == Double.parseDouble(s2)){
			return false;
		}else{
			return true;
		}
	}

	private void visitSite() {
		if(!streamState()){
			Intent i = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(swiboSite));
			startActivity(i);
		}else{
			Toast toast = Toast.makeText(MainActivity.this,"Streaming (No editing allow)", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}

	public static boolean streamState() {
		return streamOn;
	}
}

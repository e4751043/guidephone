package com.tonycube.googlemapdemo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

//�H�U�a��

//�H�W�a��
public class MainActivity extends Activity implements LocationListener {
	private static final String MAP_URL = "file:///android_asset/googlemap.htm";
	private WebView webView;
	private EditText LatText, LogText, filename;
	private Button submit;
	private Button connect;
	private Button disconnect;
	private boolean webviewReady = false;
	private Location mostRecentLocation = null;
	//
	private TextView recvNum1;
	private TextView recvNum2;
	private TextView recvNum3;

	// �Ū�Serial Port Profile
	private static final UUID SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int REQUEST_ENABLE_BT = 0;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;

	private BluetoothAdapter btAdapter; // �Ūޤ���
	private BluetoothSocket btSocket; // �Ū�Socket
	private InputStream btIn; // �Ū޿�JStream
	private OutputStream btOut; // �Ū޿�XStream
	private String deviceAddr; // ���s�����Ū޸˸m��}

	//
	private void getLocation() {// ���o�˸m��GPS��m���
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = locationManager.getBestProvider(criteria, true);
		// In order to make sure the device is getting the location, request
		// updates.
		locationManager.requestLocationUpdates(provider, 1, 0, this);
		mostRecentLocation = locationManager.getLastKnownLocation(provider);
	}

	@Override
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {

		//
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Resources res = getResources(); // Resource object to get Drawables

		TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
		tabHost.setup();

		TabSpec spec1 = tabHost.newTabSpec("Tab 1");
		spec1.setContent(R.id.tab1);
		spec1.setIndicator("Tab 1");

		TabSpec spec2 = tabHost.newTabSpec("Tab 2");
		spec2.setIndicator("Tab 2");
		spec2.setContent(R.id.tab2);

		TabSpec spec3 = tabHost.newTabSpec("Tab 3");
		spec3.setIndicator("Tab 3");
		spec3.setContent(R.id.tab3);

		tabHost.addTab(spec1);
		tabHost.addTab(spec2);
		tabHost.addTab(spec3);
		// ����
		// TextView
		recvNum1 = (TextView) findViewById(R.id.textView5);
		recvNum2 = (TextView) findViewById(R.id.textView4);
		recvNum3 = (TextView) findViewById(R.id.textView6);

		// ��l���Ū�
		btAdapter = BluetoothAdapter.getDefaultAdapter();

		// �p�G�Ūޥ��}�ҫh�}��
		if (!btAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}

		// ��J�W��
		filename = (EditText) findViewById(R.id.filename);

		// G
		LatText = (EditText) findViewById(R.id.LatText);
		LogText = (EditText) findViewById(R.id.LogText);
		submit = (Button) findViewById(R.id.submit);
		// �}�l�s�u
		connect = (Button) findViewById(R.id.connect);
		connect.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				deviceAddr = "98:D3:31:B1:3A:56";
				Thread t = new Thread(sppConnect);
				t.start();
			}
		});
		// �����s�u
		disconnect = (Button) findViewById(R.id.disconnect);
		disconnect.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				Disconnect();
				ShowMsg("�����s�u", true);

			}
		});
		submit.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {

				// TODO Auto-generated method stub

				if (webviewReady) {
					// �ѿ�J���g�n�׭ȼе��b�a�ϤW�A�I�s�bgooglemaps.html����mark�禡
					final String markURL = "javascript:mark("
							+ LatText.getText() + "," + LogText.getText() + ")";
					webView.loadUrl(markURL);

					// �e�����ܼе��I��m�A�I�s�bgooglemaps.html����centerAt�禡
					final String centerURL = "javascript:centerAt("
							+ LatText.getText() + "," + LogText.getText() + ")";
					webView.loadUrl(centerURL);
				}

			}
		});
		getLocation();// ���o�w���m
		setupWebView();// �]�wwebview
		if (mostRecentLocation != null) {
			LatText.setText("" + mostRecentLocation.getLatitude());
			LogText.setText("" + mostRecentLocation.getLongitude());

			// �N�e�����ܩw���I����m
			final String centerURL = "javascript:centerAt("
					+ mostRecentLocation.getLatitude() + ","
					+ mostRecentLocation.getLongitude() + ")";
			if (webviewReady)
				webView.loadUrl(centerURL);
		}
		// G
	}

	// ����DeviceListActivity���^�ǭ�
	/*
	 * public void onActivityResult(int requestCode, int resultCode, Intent
	 * data) { if (requestCode == REQUEST_CONNECT_DEVICE_SECURE) { if
	 * (resultCode == Activity.RESULT_OK) {
	 * 
	 * } } }
	 */

	//
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);
		return true;
	}

	// Menu����code
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.connect:
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;
		case R.id.disconnect:
			Disconnect();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// �{��������
	@Override
	protected void onDestroy() {
		super.onDestroy();

		// �����Ū޳s�u
		Disconnect();
	}

	// �s���Ū޸˸m
	private Runnable sppConnect = new Runnable() {

		public void run() {
			// �p�G�˸m�w�s���άO�L�˸m��}�h�����s��
			if (btIn != null || deviceAddr == null)
				return;

			try {
				// �إ�SPP��RfcommSocket�A�ö}�l�s�u
				btSocket = btAdapter.getRemoteDevice(deviceAddr)
						.createRfcommSocketToServiceRecord(SPP_UUID);
				btSocket.connect();

				// �Ū޿�JStream
				btIn = btSocket.getInputStream();

				// �Ū޿�XStream
				btOut = btSocket.getOutputStream();

				// �إ߷sThread���T����
				Thread t = new Thread(sppReceiver);
				t.start();

				ShowMsg("�Ū޸˸m�w�}��: " + deviceAddr, true);
			} catch (Exception e) {
				e.printStackTrace();

				try {
					btSocket.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}

				btSocket = null;
				ShowMsg("�Ū޸˸m�}�ҥ���: " + e.getMessage(), true);
			}
		}

	};
	// �{���D�n�ݭק��m
	// ------------------------------------------------------------------------------------------
	// �����ŪްT��
	private Runnable sppReceiver = new Runnable() {

		public void run() {
			byte[] buffer = new byte[1024];// �Ȧs��
			byte[] data = new byte[1024];// �Ȧs��
			int length; // ����T��������
			int j = 0;// buffer[] �������ަ�m
			float Left_length = 0;// ����Z����
			String Left_lengthtoStr = "";
			float Front_length = 0;// �e��Z����
			String Front_lengthtoStr = "";
			float Right_length = 0;// �k��Z����
			String Right_lengthtoStr = "";
			boolean onStart = false;// �Ψӱ���{���O�_���}�Y�줸
			try {
				// btIn���ĥB����ƥi�H�q�Ū޿�JStreamŪ�J
				while (btIn != null && (length = btIn.read(buffer)) != -1) {
					for (int i = 0; i < length; i++) {
						Log.e(" ", data[j] + "");// �i�ϥ�LogCat�beclipse�L�X���쪺�ƭȡA��K����
						data[j] = buffer[i];
						if (data[j] == -1) {// �ݦ���}�Y�줸��A�}��onStart boolean
							onStart = true;
							j = 0;// ���F�����K�Ӥ@�ժ����Ƕǻ���ơA�ݧ��}�Y�줸��A�N��Ʊq��m0�}�l�s��
						}
						if (onStart == true) {
							if (j % 10 == 3) {// �}�C3����m��
								float tmp1 = data[j - 1];
								float tmp2 = data[j];
								Left_length = (tmp1 * 127 + tmp2);
								DecimalFormat df = new DecimalFormat("#.##");// �榡�ƿ�X�A���ܤp���I��T��
								Left_lengthtoStr = df.format(Left_length);

							} else if (j % 10 == 5) {// �}�C5����m��
								float tmp1 = data[j - 1];
								float tmp2 = data[j];
								Front_length = (tmp1 * 127 + tmp2);
								DecimalFormat df = new DecimalFormat("#.##");
								Front_lengthtoStr = df.format(Front_length);
							} else if (j % 10 == 7) {
								float tmp1 = data[j - 1];
								float tmp2 = data[j];
								Right_length = (tmp1 * 127 + tmp2);
								DecimalFormat df = new DecimalFormat("#.##");
								Right_lengthtoStr = df.format(Right_length);
							}
						}

						// ��ܦb�����textview�W
						Show(Left_lengthtoStr, recvNum1);
						Show(Front_lengthtoStr, recvNum2);
						Show(Right_lengthtoStr, recvNum3);

						j += 1;
						if (j > 1024) {
							j = 0;
						}
					}
					if (Left_length > 0 && Front_length > 0 && Right_length > 0) {
						String content = filename.getText().toString();
						SimpleDateFormat formatter = new SimpleDateFormat(
								"yyyy-MM-dd-HH:mm:ss");
						Date curDate = new Date(System.currentTimeMillis()); // �����e�ɶ�
						String time = formatter.format(curDate);

						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
						String date = sdf.format(new java.util.Date());

						FileWriter fwL = new FileWriter("/sdcard/DCIM/" + date
								+ "-" + content + "-L.txt", true);
						BufferedWriter bwL = new BufferedWriter(fwL); // �NBufferedWeiter�PFileWrite���󰵳s��
						bwL.write(time + "---" + Left_lengthtoStr);
						bwL.newLine();
						bwL.close();

						FileWriter fwF = new FileWriter("/sdcard/DCIM/" + date
								+ "-" + content + "-F.txt", true);
						BufferedWriter bwF = new BufferedWriter(fwF); // �NBufferedWeiter�PFileWrite���󰵳s��
						bwF.write(time + "---" + Front_lengthtoStr);
						bwF.newLine();
						bwF.close();

						FileWriter fwR = new FileWriter("/sdcard/DCIM/" + date
								+ "-" + content + "-R.txt", true);
						BufferedWriter bwR = new BufferedWriter(fwR); // �NBufferedWeiter�PFileWrite���󰵳s��
						bwR.write(time + "---" + Right_lengthtoStr);
						bwR.newLine();
						bwR.close();

						Thread.sleep(1000);
					}
					Arrays.fill(buffer, (byte) 0); // �M��buffer
				}
			} catch (Exception e) {
				e.printStackTrace();

				Disconnect(); // ���_�s�u
				ShowMsg("�ŪްT���������ѡA�s�u�w���]: " + e.getMessage(), true);
			}
		}

	};

	// ��ܰT��, msg���n��ܪ��T��, isToast��true�ɥHToast�覡��ܰT���A�_�h�N�T����X��EditText
	private void ShowMsg(final String msg, final boolean isToast) {

		// �����bUI Thread��
		runOnUiThread(new Runnable() {
			public void run() {
				if (isToast)
					Toast.makeText(getApplicationContext(), msg,
							Toast.LENGTH_SHORT).show();
			}
		});

	}

	// ��ܦ��줧���O��
	private void Show(final String msg, final TextView whichText) {

		// �����bUI Thread��
		runOnUiThread(new Runnable() {
			public void run() {
				whichText.setText(msg);
			}
		});

	}

	// ���_�s�u
	synchronized private void Disconnect() {

		try {
			if (btIn != null)
				btIn.close(); // �����Ū޿�JStream
			if (btOut != null)
				btOut.close(); // �����Ū޿�XStream
			if (btSocket != null)
				btSocket.close(); // �����Ū�Socket
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			btIn = null;
			btOut = null;
			btSocket = null;
		}

	}

	// G

	// G

	// GGGGGGGGG
	//

	/** Sets up the WebView object and loads the URL of the page **/
	private void setupWebView() {

		webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		// Wait for the page to load then send the location information
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				// webView.loadUrl(centerURL);
				webviewReady = true;// webview�w�g���J����
			}

		});
		webView.loadUrl(MAP_URL);
	}

	@Override
	public void onLocationChanged(Location location) {// �w���m���ܮɷ|���檺��k
		// TODO Auto-generated method stub
		if (location != null) {
			LatText.setText("" + location.getLatitude());
			LogText.setText("" + location.getLongitude());
			// �N�e�����ܩw���I����m�A�I�s�bgooglemaps.html����centerAt�禡
			final String centerURL = "javascript:centerAt("
					+ location.getLatitude() + "," + location.getLongitude()
					+ ")";
			if (webviewReady)
				webView.loadUrl(centerURL);
		}
	}

	public void Start(View v) {
		String send = "1";
		byte[] a = send.getBytes();
		try {
			// ��X�T��
			btOut.write(a);// 1
			btOut.flush();
		} catch (IOException e) {
			e.printStackTrace();

			Disconnect(); // ���_�s�u

			ShowMsg("�ŪްT���e�X���ѡA�s�u�w���]: " + e.getMessage(), true);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}
}
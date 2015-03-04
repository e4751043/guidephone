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

//以下冠章

//以上冠章
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

	// 藍芽Serial Port Profile
	private static final UUID SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int REQUEST_ENABLE_BT = 0;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;

	private BluetoothAdapter btAdapter; // 藍芽介面
	private BluetoothSocket btSocket; // 藍芽Socket
	private InputStream btIn; // 藍芽輸入Stream
	private OutputStream btOut; // 藍芽輸出Stream
	private String deviceAddr; // 欲連接之藍芽裝置位址

	//
	private void getLocation() {// 取得裝置的GPS位置資料
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
		// 元件
		// TextView
		recvNum1 = (TextView) findViewById(R.id.textView5);
		recvNum2 = (TextView) findViewById(R.id.textView4);
		recvNum3 = (TextView) findViewById(R.id.textView6);

		// 初始化藍芽
		btAdapter = BluetoothAdapter.getDefaultAdapter();

		// 如果藍芽未開啟則開啟
		if (!btAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}

		// 輸入名稱
		filename = (EditText) findViewById(R.id.filename);

		// G
		LatText = (EditText) findViewById(R.id.LatText);
		LogText = (EditText) findViewById(R.id.LogText);
		submit = (Button) findViewById(R.id.submit);
		// 開始連線
		connect = (Button) findViewById(R.id.connect);
		connect.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				deviceAddr = "98:D3:31:B1:3A:56";
				Thread t = new Thread(sppConnect);
				t.start();
			}
		});
		// 結束連線
		disconnect = (Button) findViewById(R.id.disconnect);
		disconnect.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				Disconnect();
				ShowMsg("結束連線", true);

			}
		});
		submit.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {

				// TODO Auto-generated method stub

				if (webviewReady) {
					// 由輸入的經緯度值標註在地圖上，呼叫在googlemaps.html中的mark函式
					final String markURL = "javascript:mark("
							+ LatText.getText() + "," + LogText.getText() + ")";
					webView.loadUrl(markURL);

					// 畫面移至標註點位置，呼叫在googlemaps.html中的centerAt函式
					final String centerURL = "javascript:centerAt("
							+ LatText.getText() + "," + LogText.getText() + ")";
					webView.loadUrl(centerURL);
				}

			}
		});
		getLocation();// 取得定位位置
		setupWebView();// 設定webview
		if (mostRecentLocation != null) {
			LatText.setText("" + mostRecentLocation.getLatitude());
			LogText.setText("" + mostRecentLocation.getLongitude());

			// 將畫面移至定位點的位置
			final String centerURL = "javascript:centerAt("
					+ mostRecentLocation.getLatitude() + ","
					+ mostRecentLocation.getLongitude() + ")";
			if (webviewReady)
				webView.loadUrl(centerURL);
		}
		// G
	}

	// 接收DeviceListActivity的回傳值
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

	// Menu執行code
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

	// 程式結束時
	@Override
	protected void onDestroy() {
		super.onDestroy();

		// 結束藍芽連線
		Disconnect();
	}

	// 連接藍芽裝置
	private Runnable sppConnect = new Runnable() {

		public void run() {
			// 如果裝置已連接或是無裝置位址則取消連接
			if (btIn != null || deviceAddr == null)
				return;

			try {
				// 建立SPP的RfcommSocket，並開始連線
				btSocket = btAdapter.getRemoteDevice(deviceAddr)
						.createRfcommSocketToServiceRecord(SPP_UUID);
				btSocket.connect();

				// 藍芽輸入Stream
				btIn = btSocket.getInputStream();

				// 藍芽輸出Stream
				btOut = btSocket.getOutputStream();

				// 建立新Thread收訊息用
				Thread t = new Thread(sppReceiver);
				t.start();

				ShowMsg("藍芽裝置已開啟: " + deviceAddr, true);
			} catch (Exception e) {
				e.printStackTrace();

				try {
					btSocket.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}

				btSocket = null;
				ShowMsg("藍芽裝置開啟失敗: " + e.getMessage(), true);
			}
		}

	};
	// 程式主要需修改位置
	// ------------------------------------------------------------------------------------------
	// 接收藍芽訊息
	private Runnable sppReceiver = new Runnable() {

		public void run() {
			byte[] buffer = new byte[1024];// 暫存區
			byte[] data = new byte[1024];// 暫存區
			int length; // 收到訊息的長度
			int j = 0;// buffer[] 中的索引位置
			float Left_length = 0;// 左方距離值
			String Left_lengthtoStr = "";
			float Front_length = 0;// 前方距離值
			String Front_lengthtoStr = "";
			float Right_length = 0;// 右方距離值
			String Right_lengthtoStr = "";
			boolean onStart = false;// 用來控制程式是否找到開頭位元
			try {
				// btIn有效且有資料可以從藍芽輸入Stream讀入
				while (btIn != null && (length = btIn.read(buffer)) != -1) {
					for (int i = 0; i < length; i++) {
						Log.e(" ", data[j] + "");// 可使用LogCat在eclipse印出收到的數值，方便除錯
						data[j] = buffer[i];
						if (data[j] == -1) {// 待收到開頭位元後，開啟onStart boolean
							onStart = true;
							j = 0;// 為了維持八個一組的順序傳遞資料，待找到開頭位元後，將資料從位置0開始存放
						}
						if (onStart == true) {
							if (j % 10 == 3) {// 陣列3的位置時
								float tmp1 = data[j - 1];
								float tmp2 = data[j];
								Left_length = (tmp1 * 127 + tmp2);
								DecimalFormat df = new DecimalFormat("#.##");// 格式化輸出，取至小數點後三位
								Left_lengthtoStr = df.format(Left_length);

							} else if (j % 10 == 5) {// 陣列5的位置時
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

						// 顯示在手機的textview上
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
						Date curDate = new Date(System.currentTimeMillis()); // 獲取當前時間
						String time = formatter.format(curDate);

						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
						String date = sdf.format(new java.util.Date());

						FileWriter fwL = new FileWriter("/sdcard/DCIM/" + date
								+ "-" + content + "-L.txt", true);
						BufferedWriter bwL = new BufferedWriter(fwL); // 將BufferedWeiter與FileWrite物件做連結
						bwL.write(time + "---" + Left_lengthtoStr);
						bwL.newLine();
						bwL.close();

						FileWriter fwF = new FileWriter("/sdcard/DCIM/" + date
								+ "-" + content + "-F.txt", true);
						BufferedWriter bwF = new BufferedWriter(fwF); // 將BufferedWeiter與FileWrite物件做連結
						bwF.write(time + "---" + Front_lengthtoStr);
						bwF.newLine();
						bwF.close();

						FileWriter fwR = new FileWriter("/sdcard/DCIM/" + date
								+ "-" + content + "-R.txt", true);
						BufferedWriter bwR = new BufferedWriter(fwR); // 將BufferedWeiter與FileWrite物件做連結
						bwR.write(time + "---" + Right_lengthtoStr);
						bwR.newLine();
						bwR.close();

						Thread.sleep(1000);
					}
					Arrays.fill(buffer, (byte) 0); // 清空buffer
				}
			} catch (Exception e) {
				e.printStackTrace();

				Disconnect(); // 中斷連線
				ShowMsg("藍芽訊息接收失敗，連線已重設: " + e.getMessage(), true);
			}
		}

	};

	// 顯示訊息, msg為要顯示的訊息, isToast為true時以Toast方式顯示訊息，否則將訊息輸出至EditText
	private void ShowMsg(final String msg, final boolean isToast) {

		// 必須在UI Thread做
		runOnUiThread(new Runnable() {
			public void run() {
				if (isToast)
					Toast.makeText(getApplicationContext(), msg,
							Toast.LENGTH_SHORT).show();
			}
		});

	}

	// 顯示收到之壓力值
	private void Show(final String msg, final TextView whichText) {

		// 必須在UI Thread做
		runOnUiThread(new Runnable() {
			public void run() {
				whichText.setText(msg);
			}
		});

	}

	// 中斷連線
	synchronized private void Disconnect() {

		try {
			if (btIn != null)
				btIn.close(); // 關閉藍芽輸入Stream
			if (btOut != null)
				btOut.close(); // 關閉藍芽輸出Stream
			if (btSocket != null)
				btSocket.close(); // 關閉藍芽Socket
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
				webviewReady = true;// webview已經載入完畢
			}

		});
		webView.loadUrl(MAP_URL);
	}

	@Override
	public void onLocationChanged(Location location) {// 定位位置改變時會執行的方法
		// TODO Auto-generated method stub
		if (location != null) {
			LatText.setText("" + location.getLatitude());
			LogText.setText("" + location.getLongitude());
			// 將畫面移至定位點的位置，呼叫在googlemaps.html中的centerAt函式
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
			// 輸出訊息
			btOut.write(a);// 1
			btOut.flush();
		} catch (IOException e) {
			e.printStackTrace();

			Disconnect(); // 中斷連線

			ShowMsg("藍芽訊息送出失敗，連線已重設: " + e.getMessage(), true);
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
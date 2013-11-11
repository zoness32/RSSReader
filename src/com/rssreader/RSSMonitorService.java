package com.rssreader;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

public class RSSMonitorService extends Service {
	private List<Message> oldList = new ArrayList<Message>();
	private List<Message> newList = new ArrayList<Message>();
	private RSSLoadUtility load = new RSSLoadUtility();
	private DefaultHttpClient client = new DefaultHttpClient();
	private HttpGet getMethod = new HttpGet();
	boolean cancel, diff;
	String url;
	UpdateThread t = new UpdateThread();
	private static int sound = R.raw.tone;

	public static int getSound() {
		return sound;
	}

	public static void setSound(int newSound) {
		sound = newSound;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		load.setCancel(false);
		diff = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		load.setCancel(false);
		url = intent.getStringExtra("url");
		t.start();
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		t.interrupt();
	}

	private String[] packageList() {
		String[] msgs = new String[newList.size() * 5];
		int idx = 0;
		for (int i = 0; i < newList.size(); ++i) {
			msgs[idx++] = newList.get(i).getDate();
			msgs[idx++] = newList.get(i).getDescription();
			msgs[idx++] = newList.get(i).getLink().toString();
			msgs[idx++] = newList.get(i).getTitle();
			msgs[idx++] = newList.get(i).getUpdated();
		}

		return msgs;
	}

	class UpdateThread extends Thread {

		@Override
		public void interrupt() {
			load.setCancel(true);
		}

		@Override
		public void run() {
			try {
				oldList = load.loadRSS(url, getMethod, client);
			} catch (Exception e) {
				Log.e("Load", "oldList initiation error: " + e);
			}

			while (!cancel) {
				try {
					Thread.sleep(10000);
					newList = load.loadRSS(url, getMethod, client);
					Message newMsg, oldMsg;
					diff = false;
					for (int i = 0; i < newList.size(); ++i) {
						newMsg = newList.get(i);
						oldMsg = oldList.get(i);
						if (!newMsg.getLink().toString()
								.equals(oldMsg.getLink().toString())) {
							diff = true;
							newMsg.setUpdated(true);
						}
						if (!newMsg.getDate().toString()
								.equals(oldMsg.getDate().toString())) {
							diff = true;
							newMsg.setUpdated(true);
						}
						if (!newMsg.getDescription().toString()
								.equals(oldMsg.getDescription().toString())) {
							diff = true;
							newMsg.setUpdated(true);
						}
						if (!newMsg.getTitle().toString()
								.equals(oldMsg.getTitle().toString())) {
							diff = true;
							newMsg.setUpdated(true);
						}
					}

					if (diff) {
						Log.d("different", "Feed has been updated.");
						MediaPlayer mp = MediaPlayer.create(
								getApplicationContext(), sound);
						mp.start();

						ViewRSSListActivity.setUnpackaged(false);

						Intent i = new Intent(getBaseContext(),
								ViewRSSListActivity.class);
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.putExtra("data", packageList());
						getApplication().startActivity(i);
					}
				} catch (Exception e) {
					Log.e("Thread", "Exception: " + e);
				}

				oldList = newList;
			}
		}

	}
}
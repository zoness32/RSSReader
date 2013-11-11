package com.rssreader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rssreader.ShakeDetector.Callback;

public class ViewRSSListActivity extends ListActivity {

	private DefaultHttpClient client;
	private TextView txtUrl;
	private Button loadBtn;
	private List<Message> messages;
	private Thread t;
	private boolean successful, monIsDisabled = true;
	private static boolean unpackaged = false;
	private Exception eMsg;
	private ProgressDialog dlg;
	private RSSLoadUtility load = new RSSLoadUtility();
	private ShakeDetector detector;
	private Callback cb;
	private Calendar oldTime, newTime;
	private ArrayAdapter<Message> adapter;
	private int index, clickedId;

	public static void setUnpackaged(boolean bool) {
		unpackaged = bool;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		client = new DefaultHttpClient();
		txtUrl = (TextView) findViewById(R.id.txtURL);
		loadBtn = (Button) findViewById(R.id.btnLoad);
		oldTime = Calendar.getInstance();

		cb = new Callback() {
			@Override
			public void shakingStarted() {
			}

			@Override
			public void shakingStopped() {
				newTime = Calendar.getInstance();
				if (newTime.getTimeInMillis() - oldTime.getTimeInMillis() > 2000)
					loadFeed();
			}
		};

		detector = new ShakeDetector(this, 2.0d, 0, cb);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.getItem(0).setEnabled(!monIsDisabled);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.view_rssdetail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_monitor:
			Toast.makeText(this, "monitoring", 4000).show();
			monIsDisabled = true;
			startService(new Intent(this, RSSMonitorService.class).putExtra(
					"url", txtUrl.getText().toString()));
			break;
		case R.id.action_settings:
			startActivity(new Intent(this, SoundActivity.class));
			break;
		}

		invalidateOptionsMenu();
		return true;
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		clickedId = (int) id;

		Intent details = new Intent(this, ViewRSSDetailActivity.class);
		Message m = messages.get((int) id);
		details.putExtra("link", m.getLink().toString());
		if (m.getTitle().substring(0, 11).equals("(UPDATED)  "))
			m.setTitle(m.getTitle().substring(11, m.getTitle().length()));
		details.putExtra("title", m.getTitle());
		details.putExtra("description", m.getDescription());
		details.putExtra("date", m.getDate());
		this.startActivity(details);
	}

	public void btnLoad_clicked(View v) {
		loadFeed();
	}

	@Override
	protected void onPause() {
		super.onPause();

		detector.close();
	}

	@Override
	protected void onResume() {
		super.onResume();

		String[] newMsgs = getIntent().getStringArrayExtra("data");
		if (newMsgs != null) {
			if (!unpackaged)
				unpackageList(newMsgs);
			unpackaged = true;
			successful = true;
			finished();
			highlightUpdated();

			// disable "Monitor Feed" button
			monIsDisabled = true;
			invalidateOptionsMenu();
		}

		detector = new ShakeDetector(this, 2.0d, 0, cb);
	}

	private void highlightUpdated() {
		for (index = 0; index < adapter.getCount() - 1; ++index) {
			Message m = adapter.getItem(index);
			String s = m.getUpdated();
			if (s.equals("true")) {
				m.setTitle("(UPDATED)  " + m.getTitle());
			} else if (s.equals("false") && index == clickedId
					&& m.getTitle().substring(0, 11).equals("(UPDATED)  "))
				m.setTitle(m.getTitle().substring(11, m.getTitle().length()));

			m.setUpdated(false);
		}

	}

	private void unpackageList(String[] newMsgs) {
		List<Message> msgs = new ArrayList<Message>();
		int len = newMsgs.length / 5;
		int idx = 0;
		for (int i = 0; i < len; ++i) {
			Message m = new Message();
			m.setDate(newMsgs[idx++]);
			m.setDescription(newMsgs[idx++]);
			m.setLink(newMsgs[idx++]);
			m.setTitle(newMsgs[idx++]);
			String s = newMsgs[idx++];
			if (s.equals("true"))
				m.setUpdated(true);
			else
				m.setUpdated(false);
			msgs.add(m);
		}

		messages = msgs;
	}

	private void loadFeed() {
		loadBtn.setEnabled(false);
		oldTime = Calendar.getInstance();

		// enable Monitor Feed button
		monIsDisabled = false;
		invalidateOptionsMenu();

		stopService(new Intent(this, RSSMonitorService.class));

		successful = false;
		load.setCancel(false);
		t = new Networking();
		t.start();

		dlg = ProgressDialog.show(this, "", "Working. Please wait...", true,
				true, new OnCancelListener() {

					// Clicking the back button causes the onCancel method to
					// execute
					@Override
					public void onCancel(DialogInterface arg0) {
						t.interrupt(); // request thread to stop
					}

				});
	}

	private void finished() {
		loadBtn.setEnabled(true);
		invalidateOptionsMenu();
		if (successful) {
			adapter = new ArrayAdapter<Message>(this,
					android.R.layout.simple_list_item_1, messages);
			setListAdapter(adapter);
			monIsDisabled = false;
		}
	}

	class Networking extends Thread {
		HttpGet getMethod = new HttpGet();

		@Override
		public void interrupt() {
			super.interrupt();
			load.setCancel(true);
			successful = true;
			getMethod.abort();
			Log.d("interrupt", "Thread interrupted.");

			runOnUiThread(new Runnable() {
				public void run() {
					if (messages != null)
						finished();
					else
						loadBtn.setEnabled(true);
				}
			});
		}

		@Override
		public void run() {
			try {
				messages = loadRSS(txtUrl.getText().toString());
			} catch (Exception e) {
				Log.e("ViewRSSListActivity", "Exception fetching data", e);
				eMsg = e;
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(getBaseContext(),
								"Request failed: " + eMsg.toString(), 4000)
								.show();
						dlg.dismiss();
						loadBtn.setEnabled(true);
					}
				});

				successful = false;
			}
		}

		protected List<Message> loadRSS(String url) throws Exception {
			messages = load.loadRSS(url, getMethod, client);

			dlg.dismiss();
			runOnUiThread(new Runnable() {
				public void run() {
					finished();
				}
			});
			successful = true;
			return messages;
		}
	}
}
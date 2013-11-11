package com.rssreader;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class SoundActivity extends ListActivity {
	private int selected = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sound_settings);

		getRawFiles();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String filename = l.getItemAtPosition((int) id).toString();
		selected = this.getResources().getIdentifier(filename, "raw",
				"com.rssreader");
		if (selected != 0) {
			RSSMonitorService.setSound(selected);
		} else {
			Toast.makeText(this, "sound change failed", 4000).show();
		}
	}

	public void playBtn_clicked(View v) {
		if (selected != 0) {
			MediaPlayer mp = MediaPlayer.create(getApplicationContext(),
					selected);
			mp.start();
		}
	}

	public void getRawFiles() {
		Field[] fields = R.raw.class.getFields();
		List<String> filenames = new ArrayList<String>();

		for (int count = 0; count < fields.length; count++) {
			filenames.add(fields[count].getName());
		}

		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_single_choice,
				android.R.id.text1, filenames));
	}

}

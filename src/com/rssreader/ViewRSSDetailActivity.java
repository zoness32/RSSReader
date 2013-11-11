package com.rssreader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ViewRSSDetailActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_rssdetail);
		Intent i = getIntent();

		TextView linkLbl = (TextView) findViewById(R.id.linkLbl);
		TextView titleLbl = (TextView) findViewById(R.id.titleLbl);
		TextView descriptionLbl = (TextView) findViewById(R.id.descriptionLbl);
		TextView dateLbl = (TextView) findViewById(R.id.dateLbl);

		linkLbl.setText(i.getStringExtra("link"));
		titleLbl.setText(i.getStringExtra("title"));
		descriptionLbl.setText(i.getStringExtra("description"));
		dateLbl.setText(i.getStringExtra("date"));
	}

}

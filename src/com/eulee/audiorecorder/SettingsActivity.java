package com.eulee.audiorecorder;

import com.varma.samples.audiorecorder.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SettingsActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		Button doneButton = (Button)findViewById(R.id.btnSettingsDone);
		doneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){
			finish();
			}
			});
	}
}
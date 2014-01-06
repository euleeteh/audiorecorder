package com.eulee.audiorecorder;

//import com.varma.samples.audiorecorder.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AudioListActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audiolist);
		Button doneButton = (Button)findViewById(R.id.btnAudioDone);
		doneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){
			finish();
			}
			});
	}
}

package com.eulee.audiorecorder;

//import com.varma.samples.audiorecorder.R;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;

import java.io.File;

public class AudioListActivity extends ListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audiolist);

        AppLog.logString("Going through the directory to list files");

        String filePath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filePath,"AudioRecorder");

        File[] fileList = file.listFiles();
        String[] theNamesOfFiles = new String[fileList.length];
        for (int i = 0; i < theNamesOfFiles.length; i++) {
            theNamesOfFiles[i] = fileList[i].getName();
            AppLog.logString(theNamesOfFiles[i]);
        }
        ArrayAdapter<String> listViewItems =  new ArrayAdapter<String>(this, R.layout.row, theNamesOfFiles);

        setListAdapter(listViewItems);

        Button doneButton = (Button)findViewById(R.id.btnAudioDone);
		doneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){
			finish();
            AppLog.logString("Done. Return to Main page");
			}
			});
	}
}

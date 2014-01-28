package com.eulee.audiorecorder;

//Audio recorder/encoder obtained from:
//http://www.devlper.com/2010/12/android-audio-recording-part-2/
//Edited by Eu-Lee Teh 2013


import android.app.ListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;

import java.io.File;
import java.io.FilenameFilter;

public class AudioListActivity extends ListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audiolist);

        AppLog.logString("Going through the directory to list files");

        String filePath = Environment.getExternalStorageDirectory().getPath();
        FilenameFilter filter = new fileExtensionFilter();
        File file = new File(filePath,"AudioRecorder");

        File[] fileList = file.listFiles(filter);

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



        // selecting single ListView item
        ListView lv = getListView();
        // listening to single listitem click
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView parent, View view,int position, long id){
                AppLog.logString("playing " + id);

            }

        });
	}

     // Class to filter files which are having .mp3 extension
    class fileExtensionFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".wav") || name.endsWith(".WAV"));
        }
    }
}

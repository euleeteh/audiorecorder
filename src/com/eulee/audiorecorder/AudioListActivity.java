package com.eulee.audiorecorder;

//import com.varma.samples.audiorecorder.R;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
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



        /*// selecting single ListView item
        ListView lv = getListView();
        // listening to single listitem click
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // getting listitem index
                int songIndex = position;

                // Starting new intent
                Intent in = new Intent(getApplicationContext(),
                        AndroidBuildingMusicPlayerActivity.class);
                // Sending songIndex to PlayerActivity
                in.putExtra("songIndex", songIndex);
                setResult(100, in);
                // Closing PlayListView
                finish();
            }
        });*/
	}
    /**
     * Class to filter files which are having .mp3 extension
     * */
    class fileExtensionFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".wav") || name.endsWith(".WAV"));
        }
    }
}

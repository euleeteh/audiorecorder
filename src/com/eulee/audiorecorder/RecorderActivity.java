package com.eulee.audiorecorder;

//Audio recorder/encoder obtained from:
//http://www.devlper.com/2010/12/android-audio-recording-part-2/
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

//import com.varma.samples.audiorecorder.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.text.format.Time;
import android.widget.Toast;

public class RecorderActivity extends Activity {
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    //private static final String AUDIO_RECORDER_BACKUP_FOLDER = "AudioRecorderBk";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private AudioTrack liveStream = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    public String currentFileName;

    private AudioManager localAudioManager;
    private BluetoothAdapter mAdapter;
    public int state;
    private Context mContext = null;
    public static int headsetAudioState;

    public BroadcastReceiver registerReceiver = new BroadcastReceiver() {
//        private AudioManager localAudioManager;
        private static final int STATE_DISCONNECTED  = 0x00000000;
        private static final int STATE_CONNECTED = 0x00000002;
        private static final String EXTRA_STATE = "android.bluetooth.headset.extra.STATE";
        private static final String ACTION_BT_HEADSET_STATE_CHANGED  = "android.bluetooth.headset.action.STATE_CHANGED";
//        private static final String ACTION_BT_HEADSET_FORCE_ON = "android.bluetooth.headset.action.FORCE_ON";
//        private static final String ACTION_BT_HEADSET_FORCE_OFF = "android.bluetooth.headset.action.FORCE_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
            AppLog.logString("BT Headset: check-in...");
            try {
//                String action = intent.getAction();
//
//                if(action == null)
//                    return;
//
//                if(action.equals(ACTION_BT_HEADSET_STATE_CHANGED)){
                    int extraData = intent.getIntExtra(EXTRA_STATE  , STATE_DISCONNECTED);
                    if(extraData == STATE_CONNECTED ){

                        AppLog.logString("BT Headset: connected...");

                    }else if(extraData == STATE_DISCONNECTED){

                        AppLog.logString("BT Headset: disconnected...");
                    }
//                }
            } catch (Exception e) {

                AppLog.logString("BT Headset: error...");

            }
        }
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
//            AppLog.logString("Audio SCO state: " + state);
//            //headsetAudioState = intent.getIntExtra(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED, -2);
//
//            if ("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED".equals(intent.getAction())) {
//                headsetAudioState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -2);
//                AppLog.logString(Integer.toString(headsetAudioState));
//            }
//            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
//                AppLog.logString("SCO audio connected");
////                if(!isRecording){
////                    AppLog.logString("Unregister Receiver");
////                    unregisterReceiver(this);
////                }
//            }
//                else if(AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state){
//                    AppLog.logString("SCO audio disconnected");
////                if(isRecording){
////                    AppLog.logString("Stop Recording");
////                    enableButtons(false);
////                    stopRecording();
////                    AppLog.logString("Stopping bluetooth");
////                    localAudioManager.stopBluetoothSco();
////                    Toast toast = Toast.makeText(context, "Device Disconnected...", Toast.LENGTH_SHORT);
////                    toast.show();
////
////                }
////                    unregisterReceiver(this);
////                    stopRecording();
//                }
//        }
    };
    //BroadcastReceiver registerReceiver;

//    private Context context = getApplicationContext();
    //private BluetoothProfile.ServiceListener mProfileListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setButtonHandlers();
        enableButtons(false);

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        localAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mContext = this;
        IntentFilter newIntent = new IntentFilter();
        newIntent.addAction(/*AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED*/"android.bluetooth.headset.action.STATE_CHANGED");
        mContext.registerReceiver(registerReceiver, newIntent);

    }

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnList)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnSettings)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    // enables "Start", "List" and "Settings" buttons when app is not recording
    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
        enableButton(R.id.btnList, !isRecording);
        enableButton(R.id.btnSettings, !isRecording);
    }

    private String getFilename() {
        String filePath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filePath, AUDIO_RECORDER_FOLDER);

        Time currentTime = new Time();
        currentTime.setToNow();

        currentFileName = file.getAbsolutePath() + "/" + currentTime.format2445();
        return (currentFileName + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getBackupFilename() {

/*        String filePath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filePath, AUDIO_RECORDER_BACKUP_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }*/
        return (currentFileName + ".bk");
    }

    private String getTempFilename() {
        String filePath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filePath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filePath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording() {


//        AppLog.logString("starting bluetooth audio SCO connection...");
//        localAudioManager.startBluetoothSco();
//        isRecording = true;
//        int countDown = 1000;
//        try{
//            wait(200);
            if(localAudioManager.isBluetoothScoOn()){
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING, bufferSize);

                liveStream = new AudioTrack(AudioManager.STREAM_DTMF, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_STEREO, RECORDER_AUDIO_ENCODING, bufferSize, AudioTrack.MODE_STREAM);
                liveStream.setPlaybackRate(RECORDER_SAMPLERATE);
                recorder.startRecording();
                AppLog.logString("Start Recording...");

                isRecording = true;

                recordingThread = new Thread(new Runnable() {

                    @Override
                    public void run() {

                            writeAudioDataToFile();


                    }
                }, "AudioRecorder Thread");

                recordingThread.start();
//                countDown = 0;

            }
        else{
                AppLog.logString("else term here..");
                return;
            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }


    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        }

        int read = 0;

        liveStream.play();

        if (null != os) {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);
                liveStream.write(data,0,data.length);

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
//                if(headsetAudioState == 0){
//                    AppLog.logString("Stop Recording");
//                    enableButtons(false);
//                    stopRecording();
//                    AppLog.logString("Stopping bluetooth");
//                    am.stopBluetoothSco();
//                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(), getFilename());
        copyWaveFile(getTempFilename(), getBackupFilename());
        deleteTempFile();

        File file = new File(currentFileName + AUDIO_RECORDER_FILE_EXT_WAV);

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
        //unregisterReceiver();
//        AppLog.logString("Stopping bluetooth");
//        am.stopBluetoothSco();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    /*private void initialiseRecording(){
        AppLog.logString("Check1");
        boolean isHeadsetConnected = false;
        BluetoothHeadset mBluetoothHeadset;

        // Get the default adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Establish connection to the proxy.
        mBluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.HEADSET);

        try {
            Method method = mAdapter.getClass().getMethod("getProfileConnectionState", int.class);
            // retval = mAdapter.getProfileConnectionState(android.bluetooth.BluetoothProfile.HEADSET) != android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
            isHeadsetConnected = (Integer)method.invoke(mAdapter, 1) != 0;
            AppLog.logString(Boolean.toString(isHeadsetConnected));
            if (isHeadsetConnected){
                //check if bluetooth device is connected
                //if it is - show toast "Recording..."
                AppLog.logString("Start Recording");
                startRecording();
                enableButtons(true);
            }
            else{
                //else - show toast "Device Not Connected"
                AppLog.logString("Device Not Connected 1");
            }
        } catch (Exception exc) {
            AppLog.logString("Device Not Connected 2");
            // nothing to do
        }
    }*/

    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Context context = getApplicationContext();
            CharSequence toastText;
            int duration = Toast.LENGTH_SHORT;
            Toast toast;

            switch (v.getId()) {
                case R.id.btnStart:
//                initialiseRecording();
                    AppLog.logString("starting bluetooth audio SCO connection...");
                    localAudioManager.startBluetoothSco();
                    startRecording();

                    if(isRecording){
                        toastText = "Start Recording...";
                        enableButtons(true);
                    }
                    else{
                        toastText = "Device Not Connected...";
                        AppLog.logString("Stopping bluetooth");
                        localAudioManager.stopBluetoothSco();
                    }
                    toast = Toast.makeText(context, toastText, duration);
                    toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
                    toast.show();
                    //AppLog.logString("Start Recording");
                    //enableButtons(true);
                    //startRecording();
                    break;

                case R.id.btnStop:
                    toastText = "Stop Recording...";
                    toast = Toast.makeText(context, toastText, duration);
                    toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
                    toast.show();
                    AppLog.logString("Stop Recording");
                    enableButtons(false);
                    stopRecording();
                    AppLog.logString("Stopping bluetooth");
                    localAudioManager.stopBluetoothSco();
                    break;

                case R.id.btnList:
                    Intent intent_list = new Intent(v.getContext(), AudioListActivity.class);
                    startActivity(intent_list);
                    break;

                case R.id.btnSettings:
                    Intent intent_settings = new Intent(v.getContext(), SettingsActivity.class);
                    startActivity(intent_settings);
                    break;
            }
        }
    };
}

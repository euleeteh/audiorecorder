package com.eulee.audiorecorder;

//Audio recorder/encoder obtained from:
//http://www.devlper.com/2010/12/android-audio-recording-part-2/
//Edited by Eu-Lee Teh 2013


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.text.format.Time;
import android.widget.Toast;

public class RecorderActivity extends Activity {
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private AudioTrack liveStream = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private Thread btMicCheckerThread = null;
    private boolean isRecording = false;
    private View stopButtonView;
    private String currentFileName;
    private static AudioManager localAudioManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        stopButtonView= findViewById(R.id.btnStop);

        setButtonHandlers();
        enableButtons(false);

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        localAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    }

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button)stopButtonView).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnList)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnSettings)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    // enables "Start", "List" and "Settings" buttons when app is not recording
    public void enableButtons(boolean buttonsEnable) {
        enableButton(R.id.btnStart, !buttonsEnable);
        enableButton(R.id.btnStop, buttonsEnable);
        enableButton(R.id.btnList, !buttonsEnable);
        enableButton(R.id.btnSettings, !buttonsEnable);
    }

    private String getFilename() {
        return (currentFileName + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getBackupFilename() {
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
        String filePath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filePath, AUDIO_RECORDER_FOLDER);

        Time currentTime = new Time();
        currentTime.setToNow();

        currentFileName = file.getAbsolutePath() + "/" + currentTime.format2445();

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

            btMicCheckerThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    AppLog.logString("checker thread running");

                    while (true) {
                        if (!localAudioManager.isBluetoothScoOn()) {

                            AppLog.logString("Stop Recording - device disconnected");
                            stopRecording();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                    Toast toast = Toast.makeText(getApplicationContext(), "Device Disconnected. Stop Recording...", Toast.LENGTH_SHORT);
                                    toast.show();
                                    enableButtons(false);
                                    AppLog.logString("Stopping bluetooth");
                                    localAudioManager.stopBluetoothSco();
                                }
                            });
                            return;
                        }
                    }
                }
            },"Mic CheckerThread");
            btMicCheckerThread.start();
        }
        else{
            AppLog.logString("else term here..");
            return;
        }
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

            liveStream.stop();
            liveStream.release();

            recorder = null;
            liveStream = null;
            recordingThread = null;
        }
        copyWaveFile(getTempFilename(), getFilename());
        copyWaveFile(getTempFilename(), getBackupFilename());
        deleteTempFile();

        File file = new File(currentFileName + AUDIO_RECORDER_FILE_EXT_WAV);
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen = 0;
        long totalDataLen;
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

    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Context context = getApplicationContext();
            CharSequence toastText;
            int duration = Toast.LENGTH_SHORT;
            Toast toast;

            switch (v.getId()) {
                case R.id.btnStart:

                    AppLog.logString("starting bluetooth audio SCO connection...");
                    localAudioManager.startBluetoothSco();
                    AppLog.logString("check 1: BT mic is on: " + Boolean.toString(localAudioManager.isBluetoothScoOn()));
                    startRecording();

                    if(isRecording){
                        toastText = "Start Recording...";
                        enableButtons(true);
                        AppLog.logString("check 2: BT mic is on: " + Boolean.toString(localAudioManager.isBluetoothScoOn()));
                    }
                    else{
                        toastText = "Device Not Connected...";
                        AppLog.logString("Stopping bluetooth");
                        localAudioManager.stopBluetoothSco();
                        AppLog.logString("check 3: BT mic is on: " + Boolean.toString(localAudioManager.isBluetoothScoOn()));
                    }
                    toast = Toast.makeText(context, toastText, duration);
                    toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
                    toast.show();
                    break;

                case R.id.btnStop:
                    AppLog.logString("check 4: BT mic is on: " + Boolean.toString(localAudioManager.isBluetoothScoOn()));
                    toastText = "Stop Recording...";
                    toast = Toast.makeText(context, toastText, duration);
                    toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
                    toast.show();
                    AppLog.logString("Stop Recording");
                    enableButtons(false);
                    stopRecording();
                    AppLog.logString("Stopping bluetooth");
                    localAudioManager.stopBluetoothSco();
                    AppLog.logString("check 5: BT mic is on: " + Boolean.toString(localAudioManager.isBluetoothScoOn()));
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

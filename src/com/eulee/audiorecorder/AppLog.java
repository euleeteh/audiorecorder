package com.eulee.audiorecorder;

//Audio recorder/encoder obtained from:
//http://www.devlper.com/2010/12/android-audio-recording-part-2/
//Edited by Eu-Lee Teh 2013


import android.util.Log;

public class AppLog {
	private static final String APP_TAG = "AudioRecorder";
	
	public static int logString(String message){
		return Log.i(APP_TAG,message);
	}
}

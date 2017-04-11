package com.besta.speechdemo;

import android.app.Application;

import com.gotev.speech.Logger;
import com.gotev.speech.Speech;

/**
 * @author Aleksandar Gotev
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Speech.init(this, getPackageName());
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        //Logger.setLogLevel(Logger.LogLevel.INFO);
    }
}

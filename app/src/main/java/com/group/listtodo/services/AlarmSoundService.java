package com.group.listtodo.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import androidx.annotation.Nullable;
import com.group.listtodo.R;

public class AlarmSoundService extends Service {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private CountDownTimer autoStopTimer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String soundName = intent.getStringExtra("SOUND_NAME");

        int resId = getResources().getIdentifier(soundName, "raw", getPackageName());
        if (resId == 0) resId = R.raw.sound_alarm; 

        mediaPlayer = MediaPlayer.create(this, resId);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 1000};
        if (vibrator != null) vibrator.vibrate(pattern, 0);

        autoStopTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                stopSelf(); 
            }
        }.start();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (autoStopTimer != null) {
            autoStopTimer.cancel();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}

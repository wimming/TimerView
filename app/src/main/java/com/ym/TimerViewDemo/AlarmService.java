package com.ym.TimerViewDemo;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;

import java.util.Calendar;

/**
 * Created by ym on 16-10-12.
 */

public class AlarmService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        long countDownInMillis = intent.getLongExtra("countDownInMillis", -1);

        //操作：发送一个广播，广播接收后Toast提示定时操作完成
        Intent newIntent = new Intent(AlarmService.this, AlarmReceiver.class);
        newIntent.setAction("Alarm_GoOff");
        PendingIntent sender = PendingIntent.getBroadcast(AlarmService.this, 0, newIntent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MILLISECOND, (int) countDownInMillis);

        AlarmManager alarm = (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);

        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        long [] pattern = {0, 100};
        vibrator.vibrate(pattern, -1);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

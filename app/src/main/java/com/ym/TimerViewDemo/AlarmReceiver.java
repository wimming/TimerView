package com.ym.TimerViewDemo;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

/**
 * Created by ym on 16-9-3.
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        if(intent.getAction().equals("Alarm_GoOff")){
            Vibrator vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
            long [] pattern = {100, 200, 1000, 200};   // 停止 开启 停止 开启
            vibrator.vibrate(pattern, 0);

            Intent intentToActivity = new Intent(context, MainActivity.class);
            intentToActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentToActivity);
        }
        else if (intent.getAction().equals("Alarm_Cancel")) {
            Vibrator vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.cancel();

            AlarmManager alarm = (AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);
            Intent intent2 = new Intent(context, AlarmReceiver.class);
            intent2.setAction("Alarm_GoOff");
            alarm.cancel(PendingIntent.getBroadcast(context, 0, intent2, 0));
        }
        else if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

        }
    }
}

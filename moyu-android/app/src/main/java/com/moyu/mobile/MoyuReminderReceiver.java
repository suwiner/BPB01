package com.moyu.mobile;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

/** Local reminders only. Android may batch alarms under battery-saving modes. */
public final class MoyuReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL="moyu_browser_work_reminders";
    private static final String TASK="com.moyu.mobile.TASK_REMINDER";
    private static final String DAILY="com.moyu.mobile.DAILY_REPORT";
    private static final String WEEKLY="com.moyu.mobile.WEEKLY_REPORT";
    private static final String NOTES="moyu_mobile_notes_v2";
    private static final int REPORT_DAILY_ID=200901,REPORT_WEEKLY_ID=200902;

    static void createChannel(Context context){
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(nm!=null)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"墨鱼工作提醒",NotificationManager.IMPORTANCE_DEFAULT));
        }
    }
    private static SharedPreferences p(Context context){
        return context.getSharedPreferences("moyu_mobile_preferences",Context.MODE_PRIVATE);
    }
    private static PendingIntent pending(Context ctx,String action,String key,int code){
        Intent intent=new Intent(ctx,MoyuReminderReceiver.class);
        intent.setAction(action);intent.putExtra("id",key);
        return PendingIntent.getBroadcast(ctx,code,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static AlarmManager alarms(Context ctx){
        return (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
    }
    private static void set(Context ctx,long when,PendingIntent pi){
        AlarmManager am=alarms(ctx);if(am==null)return;
        if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
        else am.set(AlarmManager.RTC_WAKEUP,when,pi);
    }
    static void cancel(Context ctx,String id){
        if(id==null||id.isEmpty())return;
        AlarmManager am=alarms(ctx);
        if(am!=null)am.cancel(pending(ctx,TASK,id,id.hashCode()));
    }
    static void schedule(Context ctx,JSONObject note){
        if(note==null)return;
        String id=note.optString("id");
        if(id.isEmpty())return;
        cancel(ctx,id);
        if(note.optBoolean("done")||!"todo".equals(note.optString("type")))return;
        long due=note.optLong("due");
        if(due<=0)return;
        set(ctx,Math.max(System.currentTimeMillis()+10000L,due),pending(ctx,TASK,id,id.hashCode()));
    }
    static void cancelReport(Context ctx,boolean weekly){
        AlarmManager am=alarms(ctx);
        String action=weekly?WEEKLY:DAILY;
        int key=weekly?REPORT_WEEKLY_ID:REPORT_DAILY_ID;
        if(am!=null)am.cancel(pending(ctx,action,action,key));
    }
    static void scheduleReport(Context ctx,boolean weekly){
        cancelReport(ctx,weekly);
        String prefix=weekly?"weekly_reminder":"daily_reminder";
        SharedPreferences sp=p(ctx);if(!sp.getBoolean(prefix,false))return;
        Calendar c=Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY,sp.getInt(prefix+"_hour",weekly?18:17));
        c.set(Calendar.MINUTE,sp.getInt(prefix+"_minute",0));
        c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
        if(weekly) {
            while(c.get(Calendar.DAY_OF_WEEK)!=Calendar.SUNDAY)c.add(Calendar.DAY_OF_YEAR,1);
            if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,7);
        }else if(c.getTimeInMillis()<=System.currentTimeMillis()){
            c.add(Calendar.DAY_OF_YEAR,1);
        }
        String action=weekly?WEEKLY:DAILY;
        set(ctx,c.getTimeInMillis(),pending(ctx,action,action,weekly?REPORT_WEEKLY_ID:REPORT_DAILY_ID));
    }
    private static JSONObject find(Context ctx,String id) {
        try{
            JSONArray notes=new JSONArray(p(ctx).getString(NOTES,"[]"));
            for(int i=0;i<notes.length();i++){
                JSONObject item=notes.optJSONObject(i);
                if(item!=null&&id.equals(item.optString("id")))return item;
            }
        }catch(Exception ignored){}
        return null;
    }
    private static void notifyUser(Context ctx,String title,String content,int notificationId){
        createChannel(ctx);
        if(Build.VERSION.SDK_INT>=33&&ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        Intent launch=new Intent(ctx,ToolsActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(ctx,notificationId,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(ctx,CHANNEL):new Notification.Builder(ctx);
        Notification n=builder.setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title).setContentText(content).setContentIntent(pi)
            .setAutoCancel(true).setShowWhen(true).build();
        NotificationManager nm=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm!=null)nm.notify(notificationId,n);
    }
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null)return;
        String action=intent.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action)||"android.intent.action.MY_PACKAGE_REPLACED".equals(action)){
            try{
                JSONArray arr=new JSONArray(p(context).getString(NOTES,"[]"));
                for(int i=0;i<arr.length();i++)schedule(context,arr.optJSONObject(i));
            }catch(Exception ignored){}
            scheduleReport(context,false);scheduleReport(context,true);
            return;
        }
        if(TASK.equals(action)){
            String id=intent.getStringExtra("id");
            JSONObject note=find(context,id);
            if(note==null||note.optBoolean("done")||!"todo".equals(note.optString("type")))return;
            notifyUser(context,"待办事项尚未完成",note.optString("title","查看待办任务"),id.hashCode());
            int minutes=note.optInt("every",0);
            if(minutes>0)set(context,System.currentTimeMillis()+minutes*60000L,pending(context,TASK,id,id.hashCode()));
        }else if(DAILY.equals(action)){
            if(p(context).getBoolean("daily_reminder",false)){
                notifyUser(context,"工作日报提醒","今天的日报还没写？打开墨鱼浏览器记录一下。",REPORT_DAILY_ID);
                scheduleReport(context,false);
            }
        }else if(WEEKLY.equals(action)){
            if(p(context).getBoolean("weekly_reminder",false)){
                notifyUser(context,"工作周报提醒","可以汇总本周日报并完善工作周报。",REPORT_WEEKLY_ID);
                scheduleReport(context,true);
            }
        }
    }
}

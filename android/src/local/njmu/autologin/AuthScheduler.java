package local.njmu.autologin;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.*;


final class AuthScheduler {
    static final String MODE_RELIABLE="reliable";
    static final String MODE_QUIET="quiet";
    static final int JOB_PERIODIC=0x4E4A01;
    static final int JOB_TRIGGER=0x4E4A02;
    static final long MIN_PERIOD_MS=15*60*1000L;
    static boolean isQuietMode(Context c){
        return MODE_QUIET.equals(c.getSharedPreferences("auth",0).getString("runMode",MODE_QUIET));
    }
    static boolean isEnabled(Context c){
        return c.getSharedPreferences("auth",0).getBoolean("enabled",false);
    }
    private static JobScheduler jobs(Context c){
        return (JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }
    static void cancel(Context c){
        JobScheduler j=jobs(c);
        if(j!=null){j.cancel(JOB_PERIODIC);j.cancel(JOB_TRIGGER);}
        c.getSharedPreferences("auth",0).edit().putString("jobState","cancelled").apply();
    }
    static boolean isScheduled(Context c){
        JobScheduler j=jobs(c);
        if(j==null) return false;
        return j.getPendingJob(JOB_PERIODIC)!=null;
    }
    static void schedule(Context c){
        if(!isEnabled(c)||!isQuietMode(c)) return;
        JobScheduler j=jobs(c);
        if(j==null)return;
        // Recreating a periodic job on every screen-open resets its 15-minute clock.
        if(j.getPendingJob(JOB_PERIODIC)!=null)return;
        JobInfo info=new JobInfo.Builder(JOB_PERIODIC,new ComponentName(c,AuthJobService.class))
                .setPersisted(true).setPeriodic(MIN_PERIOD_MS).build();
        int result=j.schedule(info);
        c.getSharedPreferences("auth",0).edit()
            .putString("jobState",result==JobScheduler.RESULT_SUCCESS?"scheduled":"failed").apply();
    }
    static void trigger(Context c){
        if(!isEnabled(c)||!isQuietMode(c))return;
        schedule(c);
        JobScheduler j=jobs(c);
        if(j==null)return;
        JobInfo info=new JobInfo.Builder(JOB_TRIGGER,new ComponentName(c,AuthJobService.class))
                .setMinimumLatency(0L).setOverrideDeadline(30_000L).build();
        int result=j.schedule(info);
        c.getSharedPreferences("auth",0).edit()
            .putString("jobTriggerState",result==JobScheduler.RESULT_SUCCESS?"requested":"failed").apply();
    }
    static void changeMode(Context c,boolean quiet){
        c.getSharedPreferences("auth",0).edit().putString("runMode",quiet?MODE_QUIET:MODE_RELIABLE).apply();
        cancel(c);
        c.stopService(new Intent(c,AuthService.class));
        if(quiet&&isEnabled(c)) trigger(c);
    }
    static void pause(Context c){
        c.getSharedPreferences("auth",0).edit().putBoolean("enabled",false).apply();
        cancel(c);
        c.stopService(new Intent(c,AuthService.class));
    }
}

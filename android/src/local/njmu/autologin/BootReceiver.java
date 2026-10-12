package local.njmu.autologin;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(!AuthScheduler.isEnabled(c))return;
        if(AuthScheduler.isQuietMode(c)){
            AuthScheduler.trigger(c);
        }else{
            try{c.startForegroundService(new Intent(c,AuthService.class));}
            catch(Exception ex){c.getSharedPreferences("auth",0).edit().putString("serviceStartError",ex.getClass().getSimpleName()).apply();}
        }
    }
}

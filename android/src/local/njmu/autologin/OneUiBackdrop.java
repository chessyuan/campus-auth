package local.njmu.autologin;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

/**
 * Native Canvas adaptation of the read-only WoAiQianDao AppTechBackground.kt.
 * The six drawable passes, paths, gradient stops, opacity, motion formulas,
 * 24s/18s phases and 12sp/-30 degree watermark grid follow that reference.
 * Does NOT import or change source app code or depend on Jetpack Compose.
 */
public final class OneUiBackdrop extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private int bg = 0xFFF7F9FB;
    private int primary = 0xFF2189FF;
    private int secondary = 0xFF897AC9;
    private int watermarkColor = 0x21000000;
    private boolean amoled;
    private boolean active;
    private boolean auroraEnabled = true;
    private boolean allowed = true;
    private String watermark = "";
    private long startNs = 0L;
    private boolean observerRegistered;
    private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            refreshAnimationSetting();
        }
    };
    private final ContentObserver animationObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) { refreshAnimationSetting(); }
    };

    public OneUiBackdrop(Context context) { super(context); init(); }
    public OneUiBackdrop(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    private void init() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        ink.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        ink.setTextAlign(Paint.Align.LEFT);
        startNs = System.nanoTime();
    }

    public void setPalette(OneUiPalette.PaletteColors scheme, boolean isAmoled) {
        bg = scheme.background;
        primary = scheme.primary;
        amoled = isAmoled;
        float[] hsl = OneUiPalette.colorToHsl(OneUiPalette.parseColor(
                OneUiAppearance.seedHex(getContext()), 0xFF2189FF));
        float secHue = (hsl[0] + 12f) % 360f;
        float secS = amoled ? clamp(hsl[1] * 0.55f, .20f, .55f)
                : scheme.isDark ? clamp(hsl[1] * .5f, .20f, .50f)
                : clamp(hsl[1] * .45f, .20f, .50f);
        secondary = OneUiPalette.hslToColor(secHue, secS, scheme.isDark ? (amoled ? .74f : .73f) : .45f);
        if (OneUiAppearance.useDynamic(getContext()) && !amoled) {
            secondary = OneUiAppearance.readSystemColor(getContext(),
                    scheme.isDark ? "system_accent2_200" : "system_accent2_600", secondary);
        }
        int alpha = amoled ? 36 : scheme.isDark ? 34 : 33; // .14, .135, .13
        watermarkColor = Color.argb(alpha, scheme.isDark ? 255 : 0,
                scheme.isDark ? 255 : 0, scheme.isDark ? 255 : 0);
        invalidate();
    }

    public void setAuroraEnabled(boolean enabled) {
        auroraEnabled = enabled;
        invalidate();
    }

    public void setWatermarkText(String value) {
        watermark = value == null ? "" : value.trim();
        invalidate();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean canAnimate() {
        try {
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isPowerSaveMode()) return false;
            return Settings.Global.getFloat(getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
        } catch (Exception e) { return true; }
    }

    private void refreshAnimationSetting() {
        boolean now = canAnimate();
        if (now && !allowed) startNs = System.nanoTime();
        allowed = now;
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshAnimationSetting();
        if (!observerRegistered) {
            observerRegistered = true;
            try {
                IntentFilter filter = new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
                if (Build.VERSION.SDK_INT >= 33)
                    getContext().registerReceiver(powerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                else getContext().registerReceiver(powerReceiver, filter);
            } catch (RuntimeException ignored) {}
            try {
                getContext().getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                    false, animationObserver);
            } catch (RuntimeException ignored) {}
        }
    }

    @Override protected void onDetachedFromWindow() {
        if (observerRegistered) {
            observerRegistered = false;
            try { getContext().unregisterReceiver(powerReceiver); } catch (RuntimeException ignored) {}
            try { getContext().getContentResolver().unregisterContentObserver(animationObserver); }
            catch (RuntimeException ignored) {}
        }
        active = false;
        super.onDetachedFromWindow();
    }

    public void setActivityActive(boolean isActive) {
        active = isActive;
        allowed = canAnimate();
        if (isActive) invalidate();
    }

    private static int rgba(int rgb, float alpha) {
        return (Math.round(clamp(alpha,0f,1f)*255f)<<24) | (rgb&0xFFFFFF);
    }

    // Compose Color.lerp() interpolates in Oklab. Keep this conversion
    // instead of mixing sRGB bytes, to retain its neon gradient tone.
    private static double[] toOklab(int c) {
        double r = toLinear(Color.red(c)/255d);
        double g = toLinear(Color.green(c)/255d);
        double b = toLinear(Color.blue(c)/255d);
        double l = Math.cbrt(.4122214708*r + .5363325363*g + .0514459929*b);
        double m = Math.cbrt(.2119034982*r + .6806995451*g + .1073969566*b);
        double s = Math.cbrt(.0883024619*r + .2817188376*g + .6299787005*b);
        return new double[]{
                .2104542553*l + .7936177850*m - .0040720468*s,
                1.9779984951*l - 2.4285922050*m + .4505937099*s,
                .0259040371*l + .7827717662*m - .8086757660*s
        };
    }
    private static double toLinear(double c) { return c <= .04045 ? c/12.92 : Math.pow((c+.055)/1.055,2.4); }
    private static double toSrgb(double c) { return c <= .0031308 ? 12.92*c : 1.055*Math.pow(c,1/2.4)-.055; }
    private static int mixOklab(int a, int b, double t) {
        double[] x=toOklab(a), y=toOklab(b);
        double l=x[0]+(y[0]-x[0])*t, m=x[1]+(y[1]-x[1])*t, s=x[2]+(y[2]-x[2])*t;
        double L=Math.pow(l+.3963377774*m+.2158037573*s,3);
        double M=Math.pow(l-.1055613458*m-.0638541728*s,3);
        double S=Math.pow(l-.0894841775*m-1.2914855480*s,3);
        int rr=Math.round((float)(clamp((float)toSrgb(+4.0767416621*L-3.3077115913*M+.2309699292*S),0,1)*255));
        int gg=Math.round((float)(clamp((float)toSrgb(-1.2684380046*L+2.6097574011*M-.3413193965*S),0,1)*255));
        int bb=Math.round((float)(clamp((float)toSrgb(-.0041960863*L-.7034186147*M+1.7076147010*S),0,1)*255));
        return Color.rgb(rr,gg,bb);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w=getWidth(), h=getHeight();
        if (w<=0 || h<=0) return;
        c.drawColor(bg);
        // Unlike the optional aurora, the account watermark is compulsory:
        // disabling the aurora leaves the watermark visible on the base surface.
        if (!auroraEnabled) {
            drawWatermark(c, w, h);
            return;
        }

        double timeSec = allowed ? (System.nanoTime()-startNs) / 1_000_000_000.0 : 0.0;
        float phase1 = allowed ? (float)(timeSec%24.0 /24.0 *Math.PI*2) : 0f;
        float phase2 = allowed ? (float)(timeSec%18.0 /18.0 *Math.PI*2) : 0f;
        float drift=(float)Math.sin(phase1);
        float breath=(float)Math.sin(phase2);
        boolean dark = OneUiPalette.calculateLuminance(bg) < .48;
        int teal=mixOklab(primary, dark?0xFF19F5CE:0xFF00AFA1,.86);
        int violet=mixOklab(secondary, dark?0xFFA569FF:0xFF7044C4,.86);
        int blue=dark?0xFF339AFF:0xFF3981CF;
        float glowTeal=dark?.42f:.26f, glowViolet=dark?.40f:.25f;
        float bandTeal=dark?.50f:.33f, bandViolet=dark?.48f:.33f;
        float radius=Math.max(w,h)*.72f;

        // First radial glow: 0%, 50%, 100% stops; original reference order.
        float x1=w*(.78f+.18f*(float)Math.cos(phase1));
        float y1=h*(.14f+.11f*drift);
        radial(c,x1,y1,radius,new int[]{
                rgba(teal,glowTeal*(.88f+.12f*breath)),
                rgba(blue,glowTeal*.17f),Color.TRANSPARENT
        },new float[]{0,.5f,1});

        float x2=w*(.15f+.16f*(float)Math.sin(phase2));
        float y2=h*(.83f+.10f*(float)Math.cos(phase2));
        radial(c,x2,y2,radius*.90f,new int[]{
                rgba(violet,glowViolet*(.88f+.12f*drift)),
                rgba(blue,glowViolet*.20f),Color.TRANSPARENT
        },new float[]{0,.5f,1});

        float x3=w*(.50f+.08f*(float)Math.sin(phase1+phase2));
        float y3=h*(.52f+.08f*(float)Math.cos(phase1));
        radial(c,x3,y3,radius*.60f,new int[]{
                rgba(blue,dark?.18f:.11f),Color.TRANSPARENT
        },new float[]{0,1});

        // First actual aurora ribbon: same cubic control points and vertical gradient.
        float ty=h*(.30f+.07f*drift), tw=h*.19f;
        Path upper=new Path();
        upper.moveTo(-.35f*w,ty);
        upper.cubicTo(.05f*w,ty-.19f*h,.55f*w,ty+.15f*h,1.35f*w,ty-.06f*h);
        upper.lineTo(1.35f*w,ty+tw);
        upper.cubicTo(.55f*w,ty+.15f*h+tw,.05f*w,ty-.19f*h+tw,-.35f*w,ty+tw);
        upper.close();
        gradientPath(c,upper,ty-.18f*h,ty+tw+.18f*h,new int[]{
                Color.TRANSPARENT,rgba(teal,bandTeal),rgba(blue,bandTeal*.40f),Color.TRANSPARENT
        });

        // Second actual aurora ribbon, not a substitute radial glow.
        float by=h*(.69f+.07f*breath), bw=h*.16f;
        Path lower=new Path();
        lower.moveTo(-.30f*w,by+.06f*h);
        lower.cubicTo(.21f*w,by+.18f*h,.73f*w,by-.21f*h,1.30f*w,by);
        lower.lineTo(1.30f*w,by+bw);
        lower.cubicTo(.73f*w,by-.21f*h+bw,.21f*w,by+.18f*h+bw,-.30f*w,by+.06f*h+bw);
        lower.close();
        gradientPath(c,lower,by-.24f*h,by+bw+.25f*h,new int[]{
                Color.TRANSPARENT,rgba(violet,bandViolet),rgba(blue,bandViolet*.36f),Color.TRANSPARENT
        });

        drawWatermark(c,w,h);

        if(active && allowed && getVisibility()==VISIBLE && isAttachedToWindow()) {
            postInvalidateDelayed(33L);
        }
    }

    private void radial(Canvas c,float x,float y,float r,int[] colors,float[] positions){
        fill.setShader(new RadialGradient(x,y,r,colors,positions,Shader.TileMode.CLAMP));
        c.drawCircle(x,y,r,fill);
        fill.setShader(null);
    }

    private void gradientPath(Canvas c,Path path,float y1,float y2,int[] colors){
        fill.setShader(new LinearGradient(0,y1,0,y2,colors,
                new float[]{0f,1f/3f,2f/3f,1f},Shader.TileMode.CLAMP));
        c.drawPath(path,fill);
        fill.setShader(null);
    }

    private void drawWatermark(Canvas canvas,float width,float height){
        if(watermark.isEmpty())return;
        float density=getResources().getDisplayMetrics().density;
        float scaled=getResources().getDisplayMetrics().scaledDensity;
        ink.setColor(watermarkColor);
        ink.setTextSize(12f*scaled);
        ink.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        Paint.FontMetrics fm=ink.getFontMetrics();
        float textWidth=ink.measureText(watermark);
        float textHeight=fm.descent-fm.ascent;
        float stepX=Math.max(textWidth+86f*density,170f*density);
        float stepY=Math.max(textHeight+66f*density,88f*density);
        int row=0;
        for(float y=-height*.2f;y<height*1.3f;y+=stepY,row++){
            float offset=(row%2==1)?stepX/2f:0f;
            for(float x=-width*.2f+offset;x<width*1.3f;x+=stepX){
                canvas.save();
                canvas.rotate(-30f,x,y);
                canvas.drawText(watermark,x,y-fm.top,ink);
                canvas.restore();
            }
        }
    }
}

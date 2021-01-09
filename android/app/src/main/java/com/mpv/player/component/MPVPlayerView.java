package com.mpv.player.component;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import static com.mpv.player.component.MPVLib.mpvFormat.MPV_FORMAT_FLAG;
import static com.mpv.player.component.MPVLib.mpvFormat.MPV_FORMAT_INT64;
import static com.mpv.player.component.MPVLib.mpvFormat.MPV_FORMAT_NONE;

public class MPVPlayerView extends SurfaceView
        implements SurfaceHolder.Callback, MPVLib.EventObserver, MPVLib.LogObserver {
    private static final String TAG = "MPVPlayerView";
    String dataSource;
    private boolean isSurfaceCreated = false;

    public MPVPlayerView(Context context) {
        super(context);
        initialize();
    }

    public MPVPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public MPVPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public MPVPlayerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initialize() {
        getHolder().addCallback(this);
        MPVLib.create(getContext());
        MPVLib.setOptionString("config", "false");
        MPVLib.init();
        initOptions();
        observeProperties();

        MPVLib.addObserver(this);
        MPVLib.addLogObserver(this);
    }

    private void setPropertyFromSharedPreference(SharedPreferences sharedPreferences, String key, String value) {
        String preference = sharedPreferences.getString(key, "");

        if (!TextUtils.isEmpty(preference))
            MPVLib.setOptionString(value, preference);
    }

    private void initOptions() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        String hwdec = "no";
        if (sharedPreferences.getBoolean("hardware_decoding", true)) {
            hwdec = "mediacodec-copy";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

            Log.v(TAG, "Display ${disp.displayId} reports FPS of $refreshRate");
            MPVLib.setOptionString("override-display-fps",
                    String.valueOf(wm.getDefaultDisplay().getMode().getRefreshRate()));
        } else {
            Log.v(TAG, "Android version too old, disabling refresh rate functionality "
                    + "(${Build.VERSION.SDK_INT} < ${Build.VERSION_CODES.M})");
        }

        // ao: set optimal sample rate for opensles, to get better audio playback

        // TODO: better be optional as it may not be ideal if the user switches audio
        // device during playback.
        MPVLib.setOptionString("audio-samplerate",
                String.valueOf(AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)));

        // set non-complex options

        setPropertyFromSharedPreference(sharedPreferences, "default_audio_language", "alang");
        setPropertyFromSharedPreference(sharedPreferences, "default_subtitle_language", "slang");

        setPropertyFromSharedPreference(sharedPreferences, "video_scale", "scale");
        setPropertyFromSharedPreference(sharedPreferences, "video_scale_param1", "scale-param1");
        setPropertyFromSharedPreference(sharedPreferences, "video_scale_param2", "scale-param2");

        setPropertyFromSharedPreference(sharedPreferences, "video_downscale", "dscale");
        setPropertyFromSharedPreference(sharedPreferences, "video_downscale_param1", "dscale-param1");
        setPropertyFromSharedPreference(sharedPreferences, "video_downscale_param2", "dscale-param2");

        setPropertyFromSharedPreference(sharedPreferences, "video_tscale", "tscale");
        setPropertyFromSharedPreference(sharedPreferences, "video_tscale_param1", "tscale-param1");
        setPropertyFromSharedPreference(sharedPreferences, "video_tscale_param2", "tscale-param2");

        String debandMode = sharedPreferences.getString("video_debanding", "");
        if (debandMode == "gradfun") {
            // lower the default radius (16) to improve performance
            MPVLib.setOptionString("vf", "gradfun=radius=12");
        } else if (debandMode == "gpu") {
            MPVLib.setOptionString("deband", "yes");
        }

        String vidsync = sharedPreferences.getString("video_sync", "audio");
        MPVLib.setOptionString("video-sync", vidsync);

        if (sharedPreferences.getBoolean("video_interpolation", false))
            MPVLib.setOptionString("interpolation", "yes");

        if (sharedPreferences.getBoolean("gpudebug", false))
            MPVLib.setOptionString("gpu-debug", "yes");

        if (sharedPreferences.getBoolean("video_fastdecode", false)) {
            MPVLib.setOptionString("vd-lavc-fast", "yes");
            MPVLib.setOptionString("vd-lavc-skiploopfilter", "nonkey");
        }

        // set options

        MPVLib.setOptionString("vo", "gpu");
        MPVLib.setOptionString("gpu-context", "android");
        MPVLib.setOptionString("hwdec", hwdec);
        MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9");
        MPVLib.setOptionString("ao", "opensles");
        MPVLib.setOptionString("tls-verify", "yes");
        MPVLib.setOptionString("input-default-bindings", "yes");
        MPVLib.setOptionString("demuxer-max-bytes", "${32 * 1024 * 1024}");
        MPVLib.setOptionString("demuxer-max-back-bytes", "${32 * 1024 * 1024}");
        MPVLib.setOptionString("save-position-on-quit", "no");// done manually by MPVActivity
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;

        if (isSurfaceCreated) {
            String[] command = { "loadfile", dataSource };
            MPVLib.command(command);
            Log.e(TAG, "load stream:" + dataSource);
        }
    }

    public void pause() {
        MPVLib.setPropertyBoolean("pause", true);
    }

    public boolean isPaused() {
        return MPVLib.getPropertyBoolean("pause");
    }

    public void play() {
        MPVLib.setPropertyBoolean("pause", false);
    }

    public int getDuration() {
        return MPVLib.getPropertyInt("duration");
    }

    public void seek(int position) {
        MPVLib.setPropertyInt("time-pos", position);
    }

    public int getPosition() {
        return MPVLib.getPropertyInt("time-pos");
    }

    public void mute(boolean enable) {
        if (enable) {
            MPVLib.setPropertyBoolean("mute", true);
        } else {
            MPVLib.setPropertyBoolean("mute", false);
        }

    }

    public boolean isMute() {
        return MPVLib.getPropertyBoolean("mute");
    }

    public void stop() {
        seek(0);
        pause();
    }

    // Activity Pause
    public void onPause() {
        MPVLib.setPropertyString("vid", "no");
        pause();
    }

    // Activity Resume
    public void onResume() {
        // Interruptions can happen without the surface being destroyed,
        // so we need to cover this case too and reenable video output
        if (getHolder().getSurface() != null && getHolder().getSurface().isValid()) {
            MPVLib.setPropertyInt("vid", 1);
            play();
        }
    }

    public void onEnd() {
        WritableMap map = Arguments.createMap();
        final ReactContext context = (ReactContext) getContext();
        context.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onEnd", map);
    }

    public void onProgress(int duration, int currentTime) {
        WritableMap map = Arguments.createMap();
        map.putInt("duration", duration);
        map.putInt("currentTime", currentTime);
        final ReactContext context = (ReactContext) getContext();
        context.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onProgress", map);
    }

    public void onLoad(boolean loaded) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("loaded", loaded);
        final ReactContext context = (ReactContext) getContext();
        context.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onLoad", map);
    }

    public void onMute(boolean mute) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("mute", mute);
        final ReactContext context = (ReactContext) getContext();
        context.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onMute", map);
    }

    public void onPlay(boolean play) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("play", play);
        final ReactContext context = (ReactContext) getContext();
        context.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onPlay", map);
    }

    public void onBuffer(boolean buffering) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("buffering", buffering);
        final ReactContext context = (ReactContext) getContext();
        context.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onBuffer", map);
    }

    public void onError(String error) {
        WritableMap map = Arguments.createMap();
        map.putString("error", error);
        final ReactContext context = (ReactContext) getContext();
        context.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onError", map);
    }

    public void destroy() {
        // Disable surface callbacks to avoid using unintialized mpv state
        getHolder().removeCallback(this);

        MPVLib.destroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        MPVLib.attachSurface(getHolder().getSurface());
        isSurfaceCreated = true;
        if (dataSource != null) {
            String[] command = { "loadfile", dataSource };
            MPVLib.command(command);
            Log.e(TAG, "load stream:" + dataSource);
            dataSource = null;
        } else {
            // Get here when user goes to home screen and then returns to the app
            // mpv disables video output when opengl context is destroyed, enable it back
            Log.e(TAG, "GL Enable");
            MPVLib.setPropertyInt("vid", 1);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        MPVLib.setPropertyString("android-surface-size", width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.e(TAG, "Surface destroyed");
        MPVLib.detachSurface();
    }

    private void observeProperties() {
        MPVLib.observeProperty("time-pos", MPV_FORMAT_INT64);
        MPVLib.observeProperty("duration", MPV_FORMAT_INT64);
        MPVLib.observeProperty("buffer", MPV_FORMAT_INT64);
        MPVLib.observeProperty("pause", MPV_FORMAT_FLAG);
        MPVLib.observeProperty("mute", MPV_FORMAT_FLAG);
    }

    @Override
    public void eventProperty(@NonNull String property) {

    }

    @Override
    public void eventProperty(@NonNull String property, long value) {

        if (property.equalsIgnoreCase("time-pos")) {
            int duration = getDuration();
            if (duration < 0)
                duration = -1;
            onProgress(duration, (int) value);
        }
    }

    @Override
    public void eventProperty(@NonNull String property, boolean value) {

        if (property.equalsIgnoreCase("mute")) {
            onMute(value);
        } else if (property.equalsIgnoreCase("pause")) {
            onPlay(!value);
        }
    }

    @Override
    public void eventProperty(@NonNull String property, @NonNull String value) {

    }

    @Override
    public void event(int eventId) {
    }

    @Override
    public void logMessage(@NonNull String prefix, int level, @NonNull String text) {
        if (text.indexOf("Opening done") == 0) {
            onLoad(true);
        } else if (text.indexOf("Failed to open") == 0) {
            onLoad(false);
            onError("Failed to open");
        } else if (level == MPVLib.mpvLogLevel.MPV_LOG_LEVEL_ERROR) {
            onError(text);
        } else if (text.indexOf("finished playback, success") == 0) {
            onEnd();
        } else if (text.indexOf("Enter buffering") == 0) {
            onBuffer(true);
        } else if (text.indexOf("End buffering") == 0) {
            onBuffer(false);
        }
    }
}

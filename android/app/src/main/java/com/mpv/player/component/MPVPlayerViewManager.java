package com.mpv.player.component;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

public class MPVPlayerViewManager extends SimpleViewManager<MPVPlayerView> {
    public static final String REACT_CLASS = "MPVPlayerView";

    private static final String PROP_PLAY = "play";
    private static final String PROP_SEEK = "seek";
    private static final String PROP_MUTE = "mute";
    private static final String PROP_STOP = "stop";
    private static final String PROP_SCREEN_PAUSE = "onPause";

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @NonNull
    @Override
    protected MPVPlayerView createViewInstance(@NonNull ThemedReactContext reactContext) {
        return new MPVPlayerView(reactContext);
    }

    @ReactProp(name="url")
    public void setVideoPath(MPVPlayerView view, String urlPath) {
        view.setDataSource(urlPath);
    }

    @ReactProp(name = PROP_PLAY, defaultBoolean = true)
    public void play(final MPVPlayerView videoView, final boolean play) {
        if (play) {
            videoView.play();
        }
        else {
            videoView.pause();
        }
    }

    @ReactProp(name = PROP_SEEK)
    public void seek(final MPVPlayerView videoView, final int position) {
        videoView.seek(position);
    }

    @ReactProp(name = PROP_MUTE, defaultBoolean = false)
    public void mute(final MPVPlayerView videoView, final boolean enable) {
        videoView.mute(enable);
    }

    @ReactProp(name = PROP_STOP, defaultBoolean = false)
    public void stop(final MPVPlayerView videoView, final boolean stop) {
        if (stop)
            videoView.stop();
        else 
            videoView.play();
    }

    @ReactProp(name = PROP_SCREEN_PAUSE, defaultBoolean = false)
    public void onPause(final MPVPlayerView videoView, final boolean paused) {
        if (paused)
            videoView.onPause();
        else 
            videoView.onResume();
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                "onError",
                MapBuilder.of("registrationName", "onError"),
                "onLoad",
                MapBuilder.of("registrationName", "onLoad"),
                "onProgress",
                MapBuilder.of("registrationName", "onProgress"),
                "onBuffer",
                MapBuilder.of("registrationName", "onBuffer"),
                "onEnd",
                MapBuilder.of("registrationName", "onEnd"),
				"onMute",
                MapBuilder.of("registrationName", "onMute"),
				"onPlay",
                MapBuilder.of("registrationName", "onPlay")
        );
    }
}

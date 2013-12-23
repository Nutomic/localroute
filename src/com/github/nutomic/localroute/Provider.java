package com.github.nutomic.localroute;

import java.util.ArrayList;

import android.content.Context;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.res.Resources;
import android.media.AudioManager;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor;
import android.support.v7.media.MediaRouter;

final class Provider extends MediaRouteProvider {

    private static final String ROUTE_ID = "local_route";
    
    AudioManager mAudio = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

    private static final ArrayList<IntentFilter> CONTROL_FILTERS;
    static {
        IntentFilter f = new IntentFilter();
        f.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f.addAction(MediaControlIntent.ACTION_PLAY);
        f.addAction(MediaControlIntent.ACTION_PAUSE);
        f.addAction(MediaControlIntent.ACTION_SEEK);
        f.addAction(MediaControlIntent.ACTION_STOP);
        f.addDataScheme("http");
        f.addDataScheme("https");
        addDataTypeUnchecked(f, "audio/*");

        CONTROL_FILTERS = new ArrayList<IntentFilter>();
        CONTROL_FILTERS.add(f);
    }

    private static void addDataTypeUnchecked(IntentFilter filter, String type) {
        try {
            filter.addDataType(type);
        } catch (MalformedMimeTypeException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Provider(Context context) {
        super(context);
        
        Resources r = context.getResources();

        MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
                ROUTE_ID,
                r.getString(R.string.local_device))
                .addControlFilters(CONTROL_FILTERS)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolume(mAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
                .build();
        

        MediaRouteProviderDescriptor providerDescriptor =
                new MediaRouteProviderDescriptor.Builder()
                .addRoute(routeDescriptor)
                .build();
        setDescriptor(providerDescriptor);
    }

    @Override
    public RouteController onCreateRouteController(String routeId) {
        return new Controller(routeId, getContext());
    }
}
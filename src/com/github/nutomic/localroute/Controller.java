package com.github.nutomic.localroute;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouter.ControlRequestCallback;

public class Controller extends MediaRouteProvider.RouteController implements 
		MediaPlayer.OnCompletionListener {
	
	private Context mContext;
	
	private AudioManager mAudio;
    
    AudioManager.OnAudioFocusChangeListener mFocusListener;
	
    private final String mRouteId;
    
    private String mItemId;
    
    private int mState;

	MediaPlayer mPlayer = new MediaPlayer();

    public Controller(String routeId, Context context) {
    	mContext = context;
        mRouteId = routeId;
        mAudio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onRelease() {
    	mPlayer.release();
    }

    @Override
    public void onSelect() {
    	mAudio.requestAudioFocus(mFocusListener, AudioManager.STREAM_MUSIC, 
    			AudioManager.AUDIOFOCUS_GAIN);
    }

    @Override
    public void onUnselect() {
    	mAudio.abandonAudioFocus(mFocusListener);
    }

    @Override
    public void onSetVolume(int volume) {
        mAudio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    @Override
    public void onUpdateVolume(int delta) {
        int currentVolume = mAudio.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume + delta, 0);
    }

    @Override
    public boolean onControlRequest(Intent intent, ControlRequestCallback callback) {
    	String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
    	String itemId = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
        if (intent.getAction().equals(MediaControlIntent.ACTION_PLAY)) {
        	try {
        		mPlayer.reset();
				mPlayer.setDataSource(mContext, intent.getData());
				mPlayer.prepare();
				mPlayer.start();
				mItemId = intent.getDataString();
	        	mState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
            	getStatus(mItemId, mRouteId, callback);
            	return true;
			} catch (IllegalArgumentException e) {
				mState = MediaItemStatus.PLAYBACK_STATE_ERROR;
				e.printStackTrace();
			} catch (IOException e) {
				mState = MediaItemStatus.PLAYBACK_STATE_ERROR;
				e.printStackTrace();
			}
    	}
        else if (intent.getAction().equals(MediaControlIntent.ACTION_PAUSE)) {
        	mPlayer.pause();
        	mState = MediaItemStatus.PLAYBACK_STATE_PAUSED;
            return true;
    	}
        else if (intent.getAction().equals(MediaControlIntent.ACTION_RESUME)) {
        	mPlayer.start();
        	mState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
            return true;
    	}
        else if (intent.getAction().equals(MediaControlIntent.ACTION_STOP)) {
        	mPlayer.stop();
        	mState = MediaItemStatus.PLAYBACK_STATE_CANCELED;
            return true;
    	}
        else if (intent.getAction().equals(MediaControlIntent.ACTION_SEEK)) {
        	mPlayer.seekTo((int) intent.getLongExtra(
                				MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0));
        	getStatus(itemId, sessionId, callback);
            return true;
    	}
        else if(intent.getAction().equals(MediaControlIntent.ACTION_GET_STATUS)) {
        	getStatus(itemId, sessionId, callback);
            return true;
        }
		return false;
    }
    
    private void getStatus(String itemId, String sessionId, ControlRequestCallback callback) {
    	if (callback == null)
    		return;

		Bundle status = null;
		
		if (mItemId.equals(itemId)) {
			status = new MediaItemStatus.Builder(mState)
					.setContentPosition(mPlayer.getCurrentPosition())
					.setContentDuration(mPlayer.getDuration())
					.setTimestamp(SystemClock.elapsedRealtime())
					.build().asBundle();
			
			status.putString(MediaControlIntent.EXTRA_SESSION_ID, mRouteId);
			status.putString(MediaControlIntent.EXTRA_ITEM_ID, mItemId);
		}
		else
			status = new MediaItemStatus.Builder(MediaItemStatus.PLAYBACK_STATE_INVALIDATED)
					.build().asBundle();
		
		callback.onResult(status);
    	
    }

	@Override
	public void onCompletion(MediaPlayer mp) {
		mState = MediaItemStatus.PLAYBACK_STATE_FINISHED;
	}
}

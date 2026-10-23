package com.mundoplay.app;

import android.content.Context;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;

/** Builds the player with a shorter live startup buffer. */
public final class FastPlayer {
    private FastPlayer() {}

    public static SimpleExoPlayer create(Context context) {
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(15000, 50000, 1000, 3000)
                .build();
        return new SimpleExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build();
    }
}
// Build trigger for the verified 6.37 beta workflow.

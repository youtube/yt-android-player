/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.examples.youtubeapidemo;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.PlayerStyle;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.examples.youtubeapidemo.ui.FlippingView;
import com.examples.youtubeapidemo.ui.ImageWallView;

/**
 * A demo application aimed at showing the capabilities of the YouTube Player API.  It shows a video
 * wall of flipping YouTube thumbnails.  Every 5 flips, one of the thumbnails will be replaced with
 * a playing YouTube video.
 */
public class VideoWallDemoActivity extends Activity implements
    FlippingView.Listener,
    YouTubePlayer.OnInitializedListener,
    YouTubeThumbnailView.OnInitializedListener {

  private static final int RECOVERY_DIALOG_REQUEST = 1;

  /** The player view cannot be smaller than 110 pixels high. */
  private static final float PLAYER_VIEW_MINIMUM_HEIGHT_DP = 110;
  private static final int MAX_NUMBER_OF_ROWS_WANTED = 4;

  // Example playlist from which videos are displayed on the video wall
  private static final String PLAYLIST_ID = "ECAE6B03CA849AD332";

  private static final int INTER_IMAGE_PADDING_DP = 5;

  // YouTube thumbnails have a 16 / 9 aspect ratio
  private static final double THUMBNAIL_ASPECT_RATIO = 16 / 9d;

  private static final int INITIAL_FLIP_DURATION_MILLIS = 100;
  private static final int FLIP_DURATION_MILLIS = 500;
  private static final int FLIP_PERIOD_MILLIS = 2000;

  private ImageWallView imageWallView;
  private Handler flipDelayHandler;

  private FlippingView flippingView;
  private YouTubeThumbnailView thumbnailView;
  private YouTubeThumbnailLoader thumbnailLoader;

  private YouTubePlayerFragment playerFragment;
  private View playerView;
  private YouTubePlayer player;

  private Dialog errorDialog;

  private int flippingCol;
  private int flippingRow;
  private int videoCol;
  private int videoRow;

  private boolean nextThumbnailLoaded;
  private boolean activityResumed;
  private State state;

  private enum State {
    UNINITIALIZED,
    LOADING_THUMBNAILS,
    VIDEO_FLIPPED_OUT,
    VIDEO_LOADING,
    VIDEO_CUED,
    VIDEO_PLAYING,
    VIDEO_ENDED,
    VIDEO_BEING_FLIPPED_OUT,
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    state = State.UNINITIALIZED;

    ViewGroup viewFrame = new FrameLayout(this);

    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
    int maxAllowedNumberOfRows = (int) Math.floor(
        (displayMetrics.heightPixels / displayMetrics.density) / PLAYER_VIEW_MINIMUM_HEIGHT_DP);
    int numberOfRows = Math.min(maxAllowedNumberOfRows, MAX_NUMBER_OF_ROWS_WANTED);
    int interImagePaddingPx = (int) displayMetrics.density * INTER_IMAGE_PADDING_DP;
    int imageHeight = (displayMetrics.heightPixels / numberOfRows) - interImagePaddingPx;
    int imageWidth = (int) (imageHeight * THUMBNAIL_ASPECT_RATIO);

    imageWallView = new ImageWallView(this, imageWidth, imageHeight, interImagePaddingPx);
    viewFrame.addView(imageWallView, MATCH_PARENT, MATCH_PARENT);

    thumbnailView = new YouTubeThumbnailView(this);
    thumbnailView.initialize(DeveloperKey.DEVELOPER_KEY, this);

    flippingView = new FlippingView(this, this, imageWidth, imageHeight);
    flippingView.setFlipDuration(INITIAL_FLIP_DURATION_MILLIS);
    viewFrame.addView(flippingView, imageWidth, imageHeight);

    playerView = new FrameLayout(this);
    playerView.setId(R.id.player_view);
    playerView.setVisibility(View.INVISIBLE);
    viewFrame.addView(playerView, imageWidth, imageHeight);

    playerFragment = YouTubePlayerFragment.newInstance();
    playerFragment.initialize(DeveloperKey.DEVELOPER_KEY, this);
    getFragmentManager().beginTransaction().add(R.id.player_view, playerFragment).commit();

    flipDelayHandler = new FlipDelayHandler();

    setContentView(viewFrame);
  }

  @Override
  public void onInitializationSuccess(YouTubeThumbnailView thumbnailView,
      YouTubeThumbnailLoader thumbnailLoader) {
    this.thumbnailLoader = thumbnailLoader;
    thumbnailLoader.setOnThumbnailLoadedListener(new ThumbnailListener());
    maybeStartDemo();
  }

  @Override
  public void onInitializationFailure(
      YouTubeThumbnailView thumbnailView, YouTubeInitializationResult errorReason) {
    if (errorReason.isUserRecoverableError()) {
      if (errorDialog == null || !errorDialog.isShowing()) {
        errorDialog = errorReason.getErrorDialog(this, RECOVERY_DIALOG_REQUEST);
        errorDialog.show();
      }
    } else {
      String errorMessage =
          String.format(getString(R.string.error_thumbnail_view), errorReason.toString());
      Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
      boolean wasResumed) {
    VideoWallDemoActivity.this.player = player;
    player.setPlayerStyle(PlayerStyle.CHROMELESS);
    player.setPlayerStateChangeListener(new VideoListener());
    maybeStartDemo();
  }

  @Override
  public void onInitializationFailure(
      YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
    if (errorReason.isUserRecoverableError()) {
      if (errorDialog == null || !errorDialog.isShowing()) {
        errorDialog = errorReason.getErrorDialog(this, RECOVERY_DIALOG_REQUEST);
        errorDialog.show();
      }
    } else {
      String errorMessage = String.format(getString(R.string.error_player), errorReason.toString());
      Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
  }

  private void maybeStartDemo() {
    if (activityResumed && player != null && thumbnailLoader != null
        && state.equals(State.UNINITIALIZED)) {
      thumbnailLoader.setPlaylist(PLAYLIST_ID); // loading the first thumbnail will kick off demo
      state = State.LOADING_THUMBNAILS;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == RECOVERY_DIALOG_REQUEST) {
      // Retry initialization if user performed a recovery action
      if (errorDialog != null && errorDialog.isShowing()) {
        errorDialog.dismiss();
      }
      errorDialog = null;
      playerFragment.initialize(DeveloperKey.DEVELOPER_KEY, this);
      thumbnailView.initialize(DeveloperKey.DEVELOPER_KEY, this);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    activityResumed = true;
    if (thumbnailLoader != null && player != null) {
      if (state.equals(State.UNINITIALIZED)) {
        maybeStartDemo();
      } else if (state.equals(State.LOADING_THUMBNAILS)) {
        loadNextThumbnail();
      } else {
        if (state.equals(State.VIDEO_PLAYING)) {
          player.play();
        }
        flipDelayHandler.sendEmptyMessageDelayed(0, FLIP_DURATION_MILLIS);
      }
    }
  }

  @Override
  protected void onPause() {
    flipDelayHandler.removeCallbacksAndMessages(null);
    activityResumed = false;
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (thumbnailLoader != null) {
      thumbnailLoader.release();
    }
    super.onDestroy();
  }

  private void flipNext() {
    if (!nextThumbnailLoaded || state.equals(State.VIDEO_LOADING)) {
      return;
    }

    if (state.equals(State.VIDEO_ENDED)) {
      flippingCol = videoCol;
      flippingRow = videoRow;
      state = State.VIDEO_BEING_FLIPPED_OUT;
    } else {
      Pair<Integer, Integer> nextTarget = imageWallView.getNextLoadTarget();
      flippingCol = nextTarget.first;
      flippingRow = nextTarget.second;
    }

    flippingView.setX(imageWallView.getXPosition(flippingCol, flippingRow));
    flippingView.setY(imageWallView.getYPosition(flippingCol, flippingRow));
    flippingView.setFlipInDrawable(thumbnailView.getDrawable());
    flippingView.setFlipOutDrawable(imageWallView.getImageDrawable(flippingCol, flippingRow));
    imageWallView.setImageDrawable(flippingCol, flippingRow, thumbnailView.getDrawable());
    imageWallView.hideImage(flippingCol, flippingRow);
    flippingView.setVisibility(View.VISIBLE);
    flippingView.flip();
  }

  @Override
  public void onFlipped(FlippingView view) {
    imageWallView.showImage(flippingCol, flippingRow);
    flippingView.setVisibility(View.INVISIBLE);

    if (activityResumed) {
      loadNextThumbnail();

      if (state.equals(State.VIDEO_BEING_FLIPPED_OUT)) {
        state = State.VIDEO_FLIPPED_OUT;
      } else if (state.equals(State.VIDEO_CUED)) {
        videoCol = flippingCol;
        videoRow = flippingRow;
        playerView.setX(imageWallView.getXPosition(flippingCol, flippingRow));
        playerView.setY(imageWallView.getYPosition(flippingCol, flippingRow));
        imageWallView.hideImage(flippingCol, flippingRow);
        playerView.setVisibility(View.VISIBLE);
        player.play();
        state = State.VIDEO_PLAYING;
      } else if (state.equals(State.LOADING_THUMBNAILS) && imageWallView.allImagesLoaded()) {
        state = State.VIDEO_FLIPPED_OUT; // trigger flip in of an initial video
        flippingView.setFlipDuration(FLIP_DURATION_MILLIS);
        flipDelayHandler.sendEmptyMessage(0);
      }
    }
  }

  private void loadNextThumbnail() {
    nextThumbnailLoaded = false;
    if (thumbnailLoader.hasNext()) {
      thumbnailLoader.next();
    } else {
      thumbnailLoader.first();
    }
  }

  /**
   * A handler that periodically flips an element on the video wall.
   */
  private final class FlipDelayHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      flipNext();
      sendEmptyMessageDelayed(0, FLIP_PERIOD_MILLIS);
    }

  }

  /**
   * An internal listener which listens to thumbnail loading events from the
   * {@link YouTubeThumbnailView}.
   */
  private final class ThumbnailListener implements
      YouTubeThumbnailLoader.OnThumbnailLoadedListener {

    @Override
    public void onThumbnailLoaded(YouTubeThumbnailView thumbnail, String videoId) {
      nextThumbnailLoaded = true;

      if (activityResumed) {
        if (state.equals(State.LOADING_THUMBNAILS)) {
          flipNext();
        } else if (state.equals(State.VIDEO_FLIPPED_OUT)) {
          // load player with the video of the next thumbnail being flipped in
          state = State.VIDEO_LOADING;
          player.cueVideo(videoId);
        }
      }
    }

    @Override
    public void onThumbnailError(YouTubeThumbnailView thumbnail,
        YouTubeThumbnailLoader.ErrorReason reason) {
      loadNextThumbnail();
    }

  }

  private final class VideoListener implements YouTubePlayer.PlayerStateChangeListener {

    @Override
    public void onLoaded(String videoId) {
      state = State.VIDEO_CUED;
    }

    @Override
    public void onVideoEnded() {
      state = State.VIDEO_ENDED;
      imageWallView.showImage(videoCol, videoRow);
      playerView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {
      if (errorReason == YouTubePlayer.ErrorReason.UNEXPECTED_SERVICE_DISCONNECTION) {
        // player has encountered an unrecoverable error - stop the demo
        flipDelayHandler.removeCallbacksAndMessages(null);
        state = State.UNINITIALIZED;
        thumbnailLoader.release();
        thumbnailLoader = null;
        player = null;
      } else {
        state = State.VIDEO_ENDED;
      }
    }

    // ignored callbacks

    @Override
    public void onVideoStarted() { }

    @Override
    public void onAdStarted() { }

    @Override
    public void onLoading() { }

  }

}

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

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

/**
 * A simple YouTube Android API demo application which shows how to use a
 * {@link YouTubeStandalonePlayer} intent to start a YouTube video playback.
 */
public class StandalonePlayerDemoActivity extends Activity implements View.OnClickListener {

  private static final int REQ_START_STANDALONE_PLAYER = 1;
  private static final int REQ_RESOLVE_SERVICE_MISSING = 2;

  private static final String VIDEO_ID = "cdgQpa1pUUE";
  private static final String PLAYLIST_ID =  "7E952A67F31C58A3";

  private Button playVideoButton;
  private Button playPlaylistButton;

  private EditText startIndexEditText;
  private EditText startTimeEditText;
  private CheckBox autoplayCheckBox;
  private CheckBox lightboxModeCheckBox;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.standalone_player_demo);

    playVideoButton = (Button) findViewById(R.id.start_video_button);
    playPlaylistButton = (Button) findViewById(R.id.start_playlist_button);
    startIndexEditText = (EditText) findViewById(R.id.start_index_text);
    startTimeEditText = (EditText) findViewById(R.id.start_time_text);
    autoplayCheckBox = (CheckBox) findViewById(R.id.autoplay_checkbox);
    lightboxModeCheckBox = (CheckBox) findViewById(R.id.lightbox_checkbox);

    playVideoButton.setOnClickListener(this);
    playPlaylistButton.setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {

    int startIndex = parseInt(startIndexEditText.getText().toString(), 0);
    int startTimeMillis = parseInt(startTimeEditText.getText().toString(), 0) * 1000;
    boolean autoplay = autoplayCheckBox.isChecked();
    boolean lightboxMode = lightboxModeCheckBox.isChecked();

    Intent intent = null;
    if (v == playVideoButton) {
      intent = YouTubeStandalonePlayer.createVideoIntent(
          this, DeveloperKey.DEVELOPER_KEY, VIDEO_ID, startTimeMillis, autoplay, lightboxMode);
    } else if (v == playPlaylistButton) {
      intent = YouTubeStandalonePlayer.createPlaylistIntent(this, DeveloperKey.DEVELOPER_KEY,
          PLAYLIST_ID, startIndex, startTimeMillis, autoplay, lightboxMode);
    }

    if (intent != null) {
      if (canResolveIntent(intent)) {
        startActivityForResult(intent, REQ_START_STANDALONE_PLAYER);
      } else {
        // Could not resolve the intent - must need to install or update the YouTube API service.
        YouTubeInitializationResult.SERVICE_MISSING
            .getErrorDialog(this, REQ_RESOLVE_SERVICE_MISSING).show();
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQ_START_STANDALONE_PLAYER && resultCode != RESULT_OK) {
      YouTubeInitializationResult errorReason =
          YouTubeStandalonePlayer.getReturnedInitializationResult(data);
      if (errorReason.isUserRecoverableError()) {
        errorReason.getErrorDialog(this, 0).show();
      } else {
        String errorMessage =
            String.format(getString(R.string.error_player), errorReason.toString());
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
      }
    }
  }

  private boolean canResolveIntent(Intent intent) {
    List<ResolveInfo> resolveInfo = getPackageManager().queryIntentActivities(intent, 0);
    return resolveInfo != null && !resolveInfo.isEmpty();
  }

  private int parseInt(String text, int defaultValue) {
    if (!TextUtils.isEmpty(text)) {
      try {
        return Integer.parseInt(text);
      } catch (NumberFormatException e) {
        // fall through
      }
    }
    return defaultValue;
  }

}

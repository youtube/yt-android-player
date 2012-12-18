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

import com.google.android.youtube.player.YouTubeIntents;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.examples.youtubeapidemo.adapter.DemoArrayAdapter;
import com.examples.youtubeapidemo.adapter.DemoListViewItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A sample activity which shows how to use the {@link YouTubeIntents} static methods to create
 * Intents that navigate the user to Activities within the main YouTube application.
 */
public final class IntentsDemoActivity extends Activity implements OnItemClickListener {

  // This is the value of Intent.EXTRA_LOCAL_ONLY for API level 11 and above.
  private static final String EXTRA_LOCAL_ONLY = "android.intent.extra.LOCAL_ONLY";
  private static final String VIDEO_ID = "-Uwjt32NvVA";
  private static final String PLAYLIST_ID = "PLF3DFB800F05F551A";
  private static final String USER_ID = "Google";
  private static final int SELECT_VIDEO_REQUEST = 1000;

  private List<DemoListViewItem> intentItems;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.intents_demo);

    intentItems = new ArrayList<DemoListViewItem>();
    intentItems.add(new IntentItem("Play Video", IntentType.PLAY_VIDEO));
    intentItems.add(new IntentItem("Open Playlist", IntentType.OPEN_PLAYLIST));
    intentItems.add(new IntentItem("Play Playlist", IntentType.PLAY_PLAYLIST));
    intentItems.add(new IntentItem("Open User", IntentType.OPEN_USER));
    intentItems.add(new IntentItem("Open Search Results", IntentType.OPEN_SEARCH));
    intentItems.add(new IntentItem("Upload Video", IntentType.UPLOAD_VIDEO));

    ListView listView = (ListView) findViewById(R.id.intent_list);
    DemoArrayAdapter adapter =
        new DemoArrayAdapter(this, R.layout.list_item, intentItems);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(this);

    TextView youTubeVersionText = (TextView) findViewById(R.id.youtube_version_text);
    String version = YouTubeIntents.getInstalledYouTubeVersionName(this);
    if (version != null) {
      String text = String.format(getString(R.string.youtube_currently_installed), version);
      youTubeVersionText.setText(text);
    } else {
      youTubeVersionText.setText(getString(R.string.youtube_not_installed));
    }
  }

  public boolean isIntentTypeEnabled(IntentType type) {
    switch (type) {
      case PLAY_VIDEO:
        return YouTubeIntents.canResolvePlayVideoIntent(this);
      case OPEN_PLAYLIST:
        return YouTubeIntents.canResolveOpenPlaylistIntent(this);
      case PLAY_PLAYLIST:
        return YouTubeIntents.canResolvePlayPlaylistIntent(this);
      case OPEN_SEARCH:
        return YouTubeIntents.canResolveSearchIntent(this);
      case OPEN_USER:
        return YouTubeIntents.canResolveUserIntent(this);
      case UPLOAD_VIDEO:
        return YouTubeIntents.canResolveUploadIntent(this);
    }

    return false;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    IntentItem clickedIntentItem = (IntentItem) intentItems.get(position);

    Intent intent;
    switch (clickedIntentItem.type) {
      case PLAY_VIDEO:
        intent = YouTubeIntents.createPlayVideoIntentWithOptions(this, VIDEO_ID, true, false);
        startActivity(intent);
        break;
      case OPEN_PLAYLIST:
        intent = YouTubeIntents.createOpenPlaylistIntent(this, PLAYLIST_ID);
        startActivity(intent);
        break;
      case PLAY_PLAYLIST:
        intent = YouTubeIntents.createPlayPlaylistIntent(this, PLAYLIST_ID);
        startActivity(intent);
        break;
      case OPEN_SEARCH:
        intent = YouTubeIntents.createSearchIntent(this, USER_ID);
        startActivity(intent);
        break;
      case OPEN_USER:
        intent = YouTubeIntents.createUserIntent(this, USER_ID);
        startActivity(intent);
        break;
      case UPLOAD_VIDEO:
        // This will load a picker view in the users' gallery.
        // The upload activity is started in the function onActivityResult.
        intent = new Intent(Intent.ACTION_PICK, null).setType("video/*");
        intent.putExtra(EXTRA_LOCAL_ONLY, true);
        startActivityForResult(intent, SELECT_VIDEO_REQUEST);
        break;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {
    if (resultCode == RESULT_OK) {
      switch (requestCode) {
        case SELECT_VIDEO_REQUEST:
          Intent intent = YouTubeIntents.createUploadIntent(this, returnedIntent.getData());
          startActivity(intent);
          break;
      }
    }
    super.onActivityResult(requestCode, resultCode, returnedIntent);
  }

  private enum IntentType {
    PLAY_VIDEO,
    OPEN_PLAYLIST,
    PLAY_PLAYLIST,
    OPEN_USER,
    OPEN_SEARCH,
    UPLOAD_VIDEO;
  }

  private final class IntentItem implements DemoListViewItem {

    public final String title;
    public final IntentType type;

    public IntentItem(String title, IntentType type) {
      this.title = title;
      this.type = type;
    }

    @Override
    public String getTitle() {
      return title;
    }

    @Override
    public boolean isEnabled() {
      return isIntentTypeEnabled(type);
    }

    @Override
    public String getDisabledText() {
      return getString(R.string.intent_disabled);
    }

  }

}

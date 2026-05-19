package com.gametest.guessplaylist.modules.playlist.infrastructure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SpotifyPlaylistIdExtractor {

  private static final Pattern PLAYLIST_ID_PATTERN =
      Pattern.compile("(?:open\\.spotify\\.com/playlist/|spotify:playlist:)([a-zA-Z0-9]+)");

  public String extract(String playlistUrl) {
    Matcher matcher = PLAYLIST_ID_PATTERN.matcher(playlistUrl);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Invalid Spotify playlist URL");
    }
    return matcher.group(1);
  }
}

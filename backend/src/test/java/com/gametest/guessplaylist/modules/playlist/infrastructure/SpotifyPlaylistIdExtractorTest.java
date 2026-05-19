package com.gametest.guessplaylist.modules.playlist.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SpotifyPlaylistIdExtractorTest {

  private final SpotifyPlaylistIdExtractor extractor = new SpotifyPlaylistIdExtractor();

  @Test
  void shouldExtractFromWebUrl() {
    String result = extractor.extract("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc123");
    assertEquals("37i9dQZF1DXcBWIGoYBM5M", result);
  }

  @Test
  void shouldExtractFromUriFormat() {
    String result = extractor.extract("spotify:playlist:37i9dQZF1DX0XUsuxWHRQd");
    assertEquals("37i9dQZF1DX0XUsuxWHRQd", result);
  }

  @Test
  void shouldThrowForInvalidUrl() {
    assertThrows(IllegalArgumentException.class, () -> extractor.extract("https://example.com/not-spotify"));
  }
}

package com.gametest.guessplaylist.modules.spotify.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record SpotifyPlaylistResponse(String id, String name) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record SpotifyTokenResponse(@JsonProperty("access_token") String accessToken) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record SpotifyPlaylistItemsPageResponse(String next, List<SpotifyPlaylistItem> items) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record SpotifyPlaylistItem(SpotifyTrack track, @JsonProperty("added_by") SpotifyUser addedBy) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record SpotifyTrack(String id, String name) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record SpotifyUser(String id, @JsonProperty("display_name") String displayName) {}

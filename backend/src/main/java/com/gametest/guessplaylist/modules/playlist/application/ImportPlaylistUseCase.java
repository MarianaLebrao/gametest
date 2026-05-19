package com.gametest.guessplaylist.modules.playlist.application;

import com.gametest.guessplaylist.modules.playlist.domain.PlaylistImportResult;
import com.gametest.guessplaylist.modules.playlist.domain.PlaylistMember;
import com.gametest.guessplaylist.modules.playlist.domain.TrackOwner;
import com.gametest.guessplaylist.modules.playlist.infrastructure.SpotifyPlaylistIdExtractor;
import com.gametest.guessplaylist.modules.spotify.domain.SpotifyTrackOwnership;
import com.gametest.guessplaylist.modules.spotify.infrastructure.SpotifyApiClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ImportPlaylistUseCase {

  private final SpotifyPlaylistIdExtractor playlistIdExtractor;
  private final SpotifyApiClient spotifyApiClient;

  public ImportPlaylistUseCase(
      SpotifyPlaylistIdExtractor playlistIdExtractor,
      SpotifyApiClient spotifyApiClient) {
    this.playlistIdExtractor = playlistIdExtractor;
    this.spotifyApiClient = spotifyApiClient;
  }

  public PlaylistImportResult execute(String playlistUrl, String spotifyAccessToken) {
    String playlistId = playlistIdExtractor.extract(playlistUrl);

    var spotifyData = (spotifyAccessToken == null || spotifyAccessToken.isBlank())
        ? spotifyApiClient.fetchPlaylistData(playlistId)
        : spotifyApiClient.fetchPlaylistDataWithUserToken(playlistId, spotifyAccessToken);

    List<TrackOwner> tracks = spotifyData.trackOwnerships().stream()
        .map(t -> new TrackOwner(t.trackName(), t.addedByDisplayName()))
        .toList();

    Map<String, PlaylistMember> uniqueMembers = new LinkedHashMap<>();
    for (SpotifyTrackOwnership track : spotifyData.trackOwnerships()) {
      uniqueMembers.putIfAbsent(
          track.addedBySpotifyUserId(),
          new PlaylistMember(track.addedBySpotifyUserId(), track.addedByDisplayName()));
    }

    return new PlaylistImportResult(
        spotifyData.playlistId(),
        spotifyData.playlistName(),
        tracks,
        uniqueMembers.values().stream().toList());
  }
}

package com.gametest.guessplaylist.modules.playlist.api;

import java.util.List;

public record PlaylistImportResponse(
    String playlistId,
    String playlistName,
    List<TrackOwnerView> tracks,
    List<PlaylistMemberView> members) {

  public record TrackOwnerView(String trackName, String ownerDisplayName) {}

  public record PlaylistMemberView(String spotifyUserId, String displayName) {}
}

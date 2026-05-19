package com.gametest.guessplaylist.modules.spotify.domain;

import java.util.List;

public record SpotifyPlaylistData(
    String playlistId,
    String playlistName,
    List<SpotifyTrackOwnership> trackOwnerships) {}

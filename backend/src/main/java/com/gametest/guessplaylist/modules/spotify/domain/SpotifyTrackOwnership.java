package com.gametest.guessplaylist.modules.spotify.domain;

public record SpotifyTrackOwnership(
    String trackName,
    String addedBySpotifyUserId,
    String addedByDisplayName) {}

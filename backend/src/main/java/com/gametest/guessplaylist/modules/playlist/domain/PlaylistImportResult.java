package com.gametest.guessplaylist.modules.playlist.domain;

import java.util.List;

public record PlaylistImportResult(
    String playlistId,
    String playlistName,
    List<TrackOwner> tracks,
    List<PlaylistMember> members) {}

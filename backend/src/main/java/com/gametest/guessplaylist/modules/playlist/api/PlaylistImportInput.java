package com.gametest.guessplaylist.modules.playlist.api;

import jakarta.validation.constraints.NotBlank;

public record PlaylistImportInput(@NotBlank String playlistUrl, String spotifyAccessToken) {}

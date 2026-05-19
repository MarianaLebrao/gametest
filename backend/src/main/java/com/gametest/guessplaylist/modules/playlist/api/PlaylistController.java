package com.gametest.guessplaylist.modules.playlist.api;

import com.gametest.guessplaylist.modules.playlist.application.ImportPlaylistUseCase;
import com.gametest.guessplaylist.modules.playlist.domain.PlaylistImportResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

  private final ImportPlaylistUseCase importPlaylistUseCase;

  public PlaylistController(ImportPlaylistUseCase importPlaylistUseCase) {
    this.importPlaylistUseCase = importPlaylistUseCase;
  }

  @PostMapping("/import")
  public ResponseEntity<PlaylistImportResponse> importPlaylist(@Valid @RequestBody PlaylistImportInput input) {
    PlaylistImportResult result = importPlaylistUseCase.execute(input.playlistUrl(), input.spotifyAccessToken());

    PlaylistImportResponse response = new PlaylistImportResponse(
        result.playlistId(),
        result.playlistName(),
        result.tracks().stream()
            .map(t -> new PlaylistImportResponse.TrackOwnerView(t.trackName(), t.ownerDisplayName()))
            .toList(),
        result.members().stream()
            .map(m -> new PlaylistImportResponse.PlaylistMemberView(m.spotifyUserId(), m.displayName()))
            .toList());

    return ResponseEntity.ok(response);
  }
}

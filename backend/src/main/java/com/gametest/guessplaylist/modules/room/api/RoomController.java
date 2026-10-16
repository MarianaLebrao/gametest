package com.gametest.guessplaylist.modules.room.api;

import com.gametest.guessplaylist.modules.room.application.RoomService;
import com.gametest.guessplaylist.modules.room.domain.GameRoom;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  @PostMapping
  public ResponseEntity<RoomView> create(@Valid @RequestBody CreateRoomInput input) {
    GameRoom room = roomService.create(input.hostName(), input.roundCount(), input.roundTimer(), input.gameMode());
    return ResponseEntity.status(HttpStatus.CREATED).body(RoomView.from(room));
  }

  @GetMapping("/{code}")
  public RoomView find(@PathVariable String code) {
    return RoomView.from(roomService.find(code));
  }

  @PostMapping("/{code}/players")
  public PlayerView join(@PathVariable String code, @Valid @RequestBody JoinRoomInput input) {
    return PlayerView.from(roomService.join(code, input.playerName()));
  }

  @PostMapping("/{code}/tracks")
  public TrackView addTrack(@PathVariable String code, @Valid @RequestBody AddTrackInput input) {
    return TrackView.from(roomService.addTrack(code, input.playerId(), input.videoUrl(), input.previewStartSeconds()), true);
  }

  @PostMapping("/{code}/start")
  public RoomView start(@PathVariable String code, @Valid @RequestBody PlayerActionInput input) {
    return RoomView.from(roomService.start(code, input.playerId()));
  }

  @PostMapping("/{code}/votes")
  public RoomView vote(@PathVariable String code, @Valid @RequestBody VoteInput input) {
    return RoomView.from(roomService.vote(code, input.playerId(), input.targetPlayerId()));
  }

  @PostMapping("/{code}/advance")
  public RoomView advance(@PathVariable String code, @Valid @RequestBody PlayerActionInput input) {
    return RoomView.from(roomService.advance(code, input.playerId()));
  }

  public record CreateRoomInput(
      @NotBlank @Size(max = 30) String hostName,
      @Min(1) @Max(50) int roundCount,
      @Min(10) @Max(120) int roundTimer,
      @NotBlank @Pattern(regexp = "preview|blind") String gameMode) {}

  public record JoinRoomInput(@NotBlank @Size(max = 30) String playerName) {}
  public record AddTrackInput(@NotBlank String playerId, @NotBlank String videoUrl,
                              @Min(0) @Max(3600) int previewStartSeconds) {}
  public record PlayerActionInput(@NotBlank String playerId) {}
  public record VoteInput(@NotBlank String playerId, @NotBlank String targetPlayerId) {}

  public record RoomView(String code, String hostPlayerId, int roundCount, int roundTimer, String gameMode,
                         String status, int currentRound, int totalRounds, List<PlayerView> players,
                         List<TrackView> tracks, TrackView currentTrack, List<String> votedPlayerIds,
                         java.util.Map<String, Integer> scores, Instant roundEndsAt) {
    static RoomView from(GameRoom room) {
      boolean revealCurrentTrack = !"PLAYING".equals(room.status());
      List<TrackView> visibleTracks = "LOBBY".equals(room.status())
          ? room.tracks().stream().map(track -> TrackView.from(track, true)).toList()
          : List.of();
      return new RoomView(room.code(), room.hostPlayerId(), room.roundCount(), room.roundTimer(), room.gameMode(),
          room.status(), room.currentRound(), room.roundTrackIds().size(),
          room.players().stream().map(PlayerView::from).toList(), visibleTracks,
          TrackView.from(room.currentTrack(), revealCurrentTrack),
          room.votes().keySet().stream().toList(), room.scores(), room.roundEndsAt());
    }
  }

  public record PlayerView(String id, String name) {
    static PlayerView from(GameRoom.Player player) { return new PlayerView(player.id(), player.name()); }
  }

  public record TrackView(String id, String videoId, String title, String channelTitle, String thumbnailUrl,
                          String ownerPlayerId, String ownerName, int previewStartSeconds) {
    static TrackView from(GameRoom.Track track, boolean reveal) {
      if (track == null) return null;
      return new TrackView(track.id(), track.videoId(), reveal ? track.title() : null,
          reveal ? track.channelTitle() : null, reveal ? track.thumbnailUrl() : null,
          reveal ? track.ownerPlayerId() : null, reveal ? track.ownerName() : null,
          track.previewStartSeconds());
    }
  }
}

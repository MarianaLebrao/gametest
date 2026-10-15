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
    return TrackView.from(roomService.addTrack(code, input.playerId(), input.videoUrl()));
  }

  public record CreateRoomInput(
      @NotBlank @Size(max = 30) String hostName,
      @Min(1) @Max(50) int roundCount,
      @Min(10) @Max(120) int roundTimer,
      @NotBlank @Pattern(regexp = "preview|blind") String gameMode) {}

  public record JoinRoomInput(@NotBlank @Size(max = 30) String playerName) {}
  public record AddTrackInput(@NotBlank String playerId, @NotBlank String videoUrl) {}

  public record RoomView(String code, String hostPlayerId, int roundCount, int roundTimer, String gameMode,
                         List<PlayerView> players, List<TrackView> tracks) {
    static RoomView from(GameRoom room) {
      return new RoomView(room.code(), room.hostPlayerId(), room.roundCount(), room.roundTimer(), room.gameMode(),
          room.players().stream().map(PlayerView::from).toList(),
          room.tracks().stream().map(TrackView::from).toList());
    }
  }

  public record PlayerView(String id, String name) {
    static PlayerView from(GameRoom.Player player) { return new PlayerView(player.id(), player.name()); }
  }

  public record TrackView(String id, String videoId, String title, String channelTitle, String thumbnailUrl,
                          String ownerPlayerId, String ownerName) {
    static TrackView from(GameRoom.Track track) {
      return new TrackView(track.id(), track.videoId(), track.title(), track.channelTitle(), track.thumbnailUrl(),
          track.ownerPlayerId(), track.ownerName());
    }
  }
}

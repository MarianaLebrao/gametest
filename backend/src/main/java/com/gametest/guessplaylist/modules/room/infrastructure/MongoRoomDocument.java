package com.gametest.guessplaylist.modules.room.infrastructure;

import com.gametest.guessplaylist.modules.room.domain.GameRoom;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("rooms")
public record MongoRoomDocument(
    @Id String code,
    String hostPlayerId,
    int roundCount,
    int roundTimer,
    String gameMode,
    List<PlayerDocument> players,
    List<TrackDocument> tracks) {

  static MongoRoomDocument from(GameRoom room) {
    return new MongoRoomDocument(room.code(), room.hostPlayerId(), room.roundCount(), room.roundTimer(), room.gameMode(),
        room.players().stream().map(player -> new PlayerDocument(player.id(), player.name())).toList(),
        room.tracks().stream().map(track -> new TrackDocument(track.id(), track.videoId(), track.title(),
            track.channelTitle(), track.thumbnailUrl(), track.ownerPlayerId(), track.ownerName())).toList());
  }

  GameRoom toDomain() {
    return new GameRoom(code, hostPlayerId, roundCount, roundTimer, gameMode,
        players.stream().map(player -> new GameRoom.Player(player.id(), player.name())).toList(),
        tracks.stream().map(track -> new GameRoom.Track(track.id(), track.videoId(), track.title(),
            track.channelTitle(), track.thumbnailUrl(), track.ownerPlayerId(), track.ownerName())).toList());
  }

  public record PlayerDocument(String id, String name) {}
  public record TrackDocument(String id, String videoId, String title, String channelTitle, String thumbnailUrl,
                              String ownerPlayerId, String ownerName) {}
}

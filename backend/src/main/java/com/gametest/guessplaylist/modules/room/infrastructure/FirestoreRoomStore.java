package com.gametest.guessplaylist.modules.room.infrastructure;

import com.gametest.guessplaylist.modules.room.domain.GameRoom;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.room-store", havingValue = "firestore")
public class FirestoreRoomStore implements RoomStore {
  private final Firestore firestore;

  public FirestoreRoomStore(@Value("${app.google-cloud-project:}") String projectId) {
    FirestoreOptions.Builder builder = FirestoreOptions.getDefaultInstance().toBuilder();
    if (!projectId.isBlank()) builder.setProjectId(projectId);
    this.firestore = builder.build().getService();
  }

  @Override
  public void save(GameRoom room) {
    Map<String, Object> document = Map.of(
        "code", room.code(),
        "hostPlayerId", room.hostPlayerId(),
        "roundCount", room.roundCount(),
        "roundTimer", room.roundTimer(),
        "gameMode", room.gameMode(),
        "players", room.players().stream().map(player -> Map.of("id", player.id(), "name", player.name())).toList(),
        "tracks", room.tracks().stream().map(track -> Map.of(
            "id", track.id(), "videoId", track.videoId(), "title", track.title(),
            "channelTitle", track.channelTitle(), "thumbnailUrl", track.thumbnailUrl(),
            "ownerPlayerId", track.ownerPlayerId(), "ownerName", track.ownerName())).toList());
    await(firestore.collection("rooms").document(room.code()).set(document));
  }

  @Override
  public Optional<GameRoom> findByCode(String code) {
    DocumentSnapshot document = await(firestore.collection("rooms").document(code).get());
    if (!document.exists()) return Optional.empty();

    List<GameRoom.Player> players = list(document, "players").stream()
        .map(item -> castMap(item))
        .map(item -> new GameRoom.Player(string(item, "id"), string(item, "name")))
        .toList();
    List<GameRoom.Track> tracks = list(document, "tracks").stream()
        .map(item -> castMap(item))
        .map(item -> new GameRoom.Track(string(item, "id"), string(item, "videoId"), string(item, "title"),
            string(item, "channelTitle"), string(item, "thumbnailUrl"), string(item, "ownerPlayerId"),
            string(item, "ownerName")))
        .toList();

    return Optional.of(new GameRoom(code, document.getString("hostPlayerId"),
        Math.toIntExact(document.getLong("roundCount")), Math.toIntExact(document.getLong("roundTimer")),
        document.getString("gameMode"), players, tracks));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> castMap(Object value) { return (Map<String, Object>) value; }
  @SuppressWarnings("unchecked")
  private List<Object> list(DocumentSnapshot document, String key) {
    Object value = document.get(key);
    return value instanceof List<?> items ? (List<Object>) items : List.of();
  }
  private String string(Map<String, Object> values, String key) { return String.valueOf(values.getOrDefault(key, "")); }

  private <T> T await(com.google.api.core.ApiFuture<T> future) {
    try {
      return future.get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Firestore operation was interrupted.", ex);
    } catch (ExecutionException ex) {
      throw new IllegalStateException("Firestore operation failed.", ex.getCause());
    }
  }
}

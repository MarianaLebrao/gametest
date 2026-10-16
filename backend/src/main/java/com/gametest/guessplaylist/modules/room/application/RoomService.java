package com.gametest.guessplaylist.modules.room.application;

import com.gametest.guessplaylist.modules.room.domain.GameRoom;
import com.gametest.guessplaylist.modules.room.infrastructure.RoomStore;
import com.gametest.guessplaylist.modules.youtube.YouTubeClient;
import com.gametest.guessplaylist.modules.youtube.YouTubeVideo;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  private static final String CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private final SecureRandom random = new SecureRandom();
  private final YouTubeClient youTubeClient;
  private final RoomStore roomStore;

  public RoomService(YouTubeClient youTubeClient, RoomStore roomStore) {
    this.youTubeClient = youTubeClient;
    this.roomStore = roomStore;
  }

  public GameRoom create(String hostName, int roundCount, int roundTimer, String gameMode) {
    String code;
    do {
      code = randomCode();
    } while (roomStore.findByCode(code).isPresent());
    GameRoom room = new GameRoom(code, hostName.trim(), roundCount, roundTimer, gameMode);
    roomStore.save(room);
    return room;
  }

  public GameRoom find(String code) {
    return roomStore.findByCode(normalize(code))
        .orElseThrow(() -> new IllegalArgumentException("Room not found."));
  }

  public synchronized GameRoom.Player join(String code, String playerName) {
    GameRoom room = find(code);
    GameRoom.Player player = room.addPlayer(playerName.trim());
    roomStore.save(room);
    return player;
  }

  public synchronized GameRoom.Track addTrack(String code, String playerId, String videoUrl, int previewStartSeconds) {
    GameRoom room = find(code);
    YouTubeVideo video = youTubeClient.findVideo(videoUrl);
    GameRoom.Track track = room.addTrack(playerId, video.videoId(), video.title(), video.channelTitle(),
        video.thumbnailUrl(), previewStartSeconds);
    roomStore.save(room);
    return track;
  }

  public synchronized GameRoom start(String code, String playerId) {
    GameRoom room = find(code);
    List<String> trackIds = new ArrayList<>(room.tracks().stream().map(GameRoom.Track::id).toList());
    Collections.shuffle(trackIds, random);
    room.start(playerId, trackIds);
    roomStore.save(room);
    return room;
  }

  public synchronized GameRoom vote(String code, String playerId, String targetPlayerId) {
    GameRoom room = find(code);
    room.submitVote(playerId, targetPlayerId);
    roomStore.save(room);
    return room;
  }

  public synchronized GameRoom advance(String code, String playerId) {
    GameRoom room = find(code);
    room.advance(playerId);
    roomStore.save(room);
    return room;
  }

  private String randomCode() {
    StringBuilder code = new StringBuilder(6);
    for (int index = 0; index < 6; index++) {
      code.append(CODE_CHARACTERS.charAt(random.nextInt(CODE_CHARACTERS.length())));
    }
    return code.toString();
  }

  private String normalize(String code) {
    return code == null ? "" : code.trim().toUpperCase();
  }
}

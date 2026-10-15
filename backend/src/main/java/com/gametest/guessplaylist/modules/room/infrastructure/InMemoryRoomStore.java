package com.gametest.guessplaylist.modules.room.infrastructure;

import com.gametest.guessplaylist.modules.room.domain.GameRoom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.room-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryRoomStore implements RoomStore {
  private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

  @Override
  public void save(GameRoom room) { rooms.put(room.code(), room); }

  @Override
  public Optional<GameRoom> findByCode(String code) { return Optional.ofNullable(rooms.get(code)); }
}

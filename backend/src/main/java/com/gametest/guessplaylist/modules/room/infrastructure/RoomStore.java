package com.gametest.guessplaylist.modules.room.infrastructure;

import com.gametest.guessplaylist.modules.room.domain.GameRoom;
import java.util.Optional;

public interface RoomStore {
  void save(GameRoom room);
  Optional<GameRoom> findByCode(String code);
  void deleteByCode(String code);
}

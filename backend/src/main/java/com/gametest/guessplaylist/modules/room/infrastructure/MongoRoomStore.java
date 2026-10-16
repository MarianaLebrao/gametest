package com.gametest.guessplaylist.modules.room.infrastructure;

import com.gametest.guessplaylist.modules.room.domain.GameRoom;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.room-store", havingValue = "mongodb")
public class MongoRoomStore implements RoomStore {
  private final MongoRoomRepository repository;

  public MongoRoomStore(MongoRoomRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(GameRoom room) {
    repository.save(MongoRoomDocument.from(room));
  }

  @Override
  public Optional<GameRoom> findByCode(String code) {
    return repository.findById(code).map(MongoRoomDocument::toDomain);
  }

  @Override
  public void deleteByCode(String code) {
    repository.deleteById(code);
  }
}

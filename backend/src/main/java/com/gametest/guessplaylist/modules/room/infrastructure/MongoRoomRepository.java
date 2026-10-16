package com.gametest.guessplaylist.modules.room.infrastructure;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoRoomRepository extends MongoRepository<MongoRoomDocument, String> {}

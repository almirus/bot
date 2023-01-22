package com.almirus.kvartalyBot.dal.repository;

import com.almirus.kvartalyBot.dal.entity.LOG;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;

public interface LogRepository extends CrudRepository<LOG, Long>, JpaSpecificationExecutor<LOG> {
    @Query("SELECT COUNT(l) FROM LOG l WHERE l.telegramId=?1 AND l.logBody=?2 AND l.createDate >= ?3")
    Long getCountOfCommand(String telegramUserId, String command, Instant time);
}

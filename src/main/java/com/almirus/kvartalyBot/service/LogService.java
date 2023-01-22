package com.almirus.kvartalyBot.service;

import com.almirus.kvartalyBot.dal.entity.LOG;
import com.almirus.kvartalyBot.dal.repository.LogRepository;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
@Data
public class LogService {
    private final LogRepository logRepository;

    public void save(String telegramUserId, String logBody) {
        LOG log = new LOG();
        log.setLogBody(logBody);
        log.setTelegramId(telegramUserId);
        log.setCreateDate(LocalDateTime.now().toInstant(ZoneOffset.UTC));
        logRepository.save(log);
    }

    public Boolean checkUserSendMoreOneCommand(String telegramUserId, String logBody) {
        return logRepository.getCountOfCommand(telegramUserId, logBody, LocalDateTime.now().toInstant(ZoneOffset.UTC).minus(1, ChronoUnit.HOURS)) >= 1;
    }
}

package com.almirus.kvartalyBot.service;

import com.almirus.kvartalyBot.dal.entity.TempOwner;

import com.almirus.kvartalyBot.dal.repository.TempOwnerRepository;
import lombok.Data;
import org.springframework.stereotype.Service;


@Service
@Data
public class TempOwnerService {
    private final TempOwnerRepository userRepository;


    public boolean isUserExist(String chatId) {
        TempOwner user = userRepository.getOwnerByTelegramId(chatId);
        return user != null;
    }

    public TempOwner getUser(String chatId) {
        return userRepository.getOwnerByTelegramId(chatId);
    }

    public TempOwner add(TempOwner owner) {
        userRepository.save(owner);
        return owner;
    }

    public TempOwner delete(TempOwner owner) {
        userRepository.delete(owner);
        return owner;
    }

}

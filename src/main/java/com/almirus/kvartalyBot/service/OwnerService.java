package com.almirus.kvartalyBot.service;

import com.almirus.kvartalyBot.dal.entity.Owner;

import com.almirus.kvartalyBot.dal.repository.OwnerRepository;
import lombok.Data;
import org.springframework.stereotype.Service;


@Service
@Data
public class OwnerService {
    private final OwnerRepository userRepository;


    public boolean isUserExist(String chatId) {
        Owner user = userRepository.getOwnerByTelegramId(chatId);
        return user != null;
    }

    public Owner getUser(String chatId) {
        return userRepository.getOwnerByTelegramId(chatId);
    }

    public Owner add(Owner owner) {
        userRepository.save(owner);
        return owner;
    }

    public Owner delete(Owner owner) {
        userRepository.delete(owner);
        return owner;
    }
}

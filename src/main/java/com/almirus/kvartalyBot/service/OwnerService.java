package com.example.buns.service;

import com.example.buns.dal.entity.Owner;
import com.example.buns.dal.repository.OwnerRepository;
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

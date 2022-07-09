package com.almirus.kvartalyBot.dal.repository;


import com.almirus.kvartalyBot.dal.entity.TempOwner;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface TempOwnerRepository extends CrudRepository<TempOwner, Long>,
        JpaSpecificationExecutor<TempOwner> {
    TempOwner getOwnerByTelegramId(String chatId);

}
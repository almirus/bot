package com.almirus.kvartalyBot.dal.repository;


import com.almirus.kvartalyBot.dal.entity.Owner;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface OwnerRepository extends CrudRepository<Owner, Long>,
        JpaSpecificationExecutor<Owner> {
    Owner getOwnerByTelegramId(String chatId);

}
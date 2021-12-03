package com.example.buns.dal.repository;


import com.example.buns.dal.entity.Owner;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface OwnerRepository extends CrudRepository<Owner, Long>,
        JpaSpecificationExecutor<Owner> {
    Owner getOwnerByTelegramId(String chatId);

}
package com.example.buns.dal.repository;


import com.example.buns.dal.entity.Owner;
import com.example.buns.dal.entity.TempOwner;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface TempOwnerRepository extends CrudRepository<TempOwner, Long>,
        JpaSpecificationExecutor<TempOwner> {
    TempOwner getOwnerByTelegramId(String chatId);

}
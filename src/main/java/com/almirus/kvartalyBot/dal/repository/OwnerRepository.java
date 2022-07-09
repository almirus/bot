package com.almirus.kvartalyBot.dal.repository;


import com.almirus.kvartalyBot.dal.entity.Owner;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OwnerRepository extends CrudRepository<Owner, Long>,
        JpaSpecificationExecutor<Owner> {
    Owner getOwnerByTelegramId(String chatId);

    //ищем в строке машиноместа указанное
    @Query(value = "select * from owner where to_tsvector(\"car_place\") @@ plainto_tsquery(:objectNum)", nativeQuery = true)
    List<Owner> findByCarPlace(@Param("objectNum") String objectNum);
}
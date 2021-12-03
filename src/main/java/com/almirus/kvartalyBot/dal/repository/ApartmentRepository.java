package com.almirus.kvartalyBot.dal.repository;

import com.almirus.kvartalyBot.dal.entity.Apartment;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ApartmentRepository extends CrudRepository<Apartment, Long>,
        JpaSpecificationExecutor<Apartment> {
    Apartment getApartmentById(Integer roomNum);
}

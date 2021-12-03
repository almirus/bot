package com.example.buns.dal.repository;

import com.example.buns.dal.entity.Apartment;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ApartmentRepository extends CrudRepository<Apartment, Long>,
        JpaSpecificationExecutor<Apartment> {
    Apartment getApartmentById(Integer roomNum);
}

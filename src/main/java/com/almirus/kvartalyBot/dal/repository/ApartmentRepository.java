package com.almirus.kvartalyBot.dal.repository;

import com.almirus.kvartalyBot.dal.entity.Apartment;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApartmentRepository extends CrudRepository<Apartment, Long>,
        JpaSpecificationExecutor<Apartment> {
    Apartment getApartmentById(Integer roomNum);
    Apartment getApartmentByDduNum(Integer roomNum);

    @Query("select a from Apartment a where a.floor = ?1 and a.entrance = ?2 order by a.id")
    List<Apartment> getFloorApartments(Integer floor, Integer entrance);

    @Query("select a from Apartment a where a.entrance = ?1 order by a.id")
    List<Apartment> getEntranceApartments(Integer entrance);

    @Query("select distinct a.floor from Apartment a where a.entrance = ?1 order by a.floor")
    List<Integer> getEntranceFloors(Integer entrance);
}

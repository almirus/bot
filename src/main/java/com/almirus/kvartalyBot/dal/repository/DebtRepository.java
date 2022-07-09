package com.almirus.kvartalyBot.dal.repository;

import com.almirus.kvartalyBot.dal.entity.Debt;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DebtRepository extends CrudRepository<Debt, Long>,
        JpaSpecificationExecutor<Debt> {
    List<Debt> getDebtorsById(Integer roomNum);
    List<Debt> findAll();

}
package com.almirus.kvartalyBot.dal.repository;


import com.almirus.kvartalyBot.dal.entity.News;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface NewsRepository extends CrudRepository<News, Long>, JpaSpecificationExecutor<News> {
    List<News> findAll();
}

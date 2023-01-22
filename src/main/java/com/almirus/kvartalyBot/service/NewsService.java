package com.almirus.kvartalyBot.service;

import com.almirus.kvartalyBot.dal.entity.News;
import com.almirus.kvartalyBot.dal.repository.NewsRepository;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@Data
public class NewsService {
    private final NewsRepository newsRepository;

    public List<News> getNewsList() {
        return newsRepository.findAll();
    }


}

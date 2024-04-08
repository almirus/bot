package com.almirus.kvartalyBot.dal.entity;

import lombok.Data;

import java.time.OffsetDateTime;


@Data

public class Environment {
    private Integer id;
    private OffsetDateTime updated;
    private Double temperature;
    private Double humidity;
    private Double pressure;
    private Double pm1;
    private Double pm25;
    private Double pm10;
    private Integer rainCount;
    private Integer uv;
    private Double uvIndex;
    private Double lux;
}

package com.almirus.kvartalyBot.dal.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@Table(name = "tmp_owner", schema = "public")
//временная таблица, хранят пользователей которые или не завершили ввод всех данных или не активировали
public class TempOwner implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "real_num")
    private Integer realNum;

    @Column(name = "phone_num")
    private String phoneNum;

    @Column(name = "nick")
    private String name;

    @Column(name = "telegram_id")
    private String telegramId;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "car_place")
    private String carPlace;

    @Column(name = "chat_id")
    private String chatId;
}

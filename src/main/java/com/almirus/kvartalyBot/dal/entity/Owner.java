package com.almirus.kvartalyBot.dal.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@Table(name = "owner")
//таблица с владельцами, зарегистрированными
public class Owner implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_num")
    private String phoneNum;

    @Column(name = "nick")
    private String name;

    @Column(name = "telegram_id")
    private String telegramId;

    @ManyToMany
    @JoinTable(name="apartment_owner",
    joinColumns = @JoinColumn(name = "owner_id"),
    inverseJoinColumns = @JoinColumn(name = "apartment_real_num"))
    private List<Apartment> apartmentList = new java.util.ArrayList<>();


    @Column(name = "activated_telegram_id")
    private String activatedTelegramId;

    @Column(name = "car_place")
    private String carPlace;
}

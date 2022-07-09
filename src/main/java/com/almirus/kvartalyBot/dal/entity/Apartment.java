package com.almirus.kvartalyBot.dal.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Table(name = "apartment")
//таблица от застройщика с номерами всех квартир
public class Apartment {
    @Id
    @Column(name = "real_num")
    //первичный ключ номер квартиры!
    private Integer id;

    @Column(name = "building")
    private int building;

    @Column(name = "entrance")
    private int entrance;

    @Column(name = "floor")
    private int floor;

    @Column(name = "section")
    private int section;

    @Column(name = "ddu_num")
    private int dduNum;

    @Column(name = "room")
    private String room;

    @Column(name = "bti_area")
    private Float btiArea;

    @Column(name = "real_area")
    private Float realArea;

    @Column(name = "difference")
    private Float difference;

    @ManyToMany
    @JoinTable(name = "apartment_owner",
            joinColumns = @JoinColumn(name = "apartment_real_num"),
            inverseJoinColumns = @JoinColumn(name = "owner_id"))
    private List<Owner> ownerList = new java.util.ArrayList<>();

    @OneToMany
    @JoinColumn(name = "apartment_or_car_place")
    private List<Debt> debtorList = new java.util.ArrayList<>();
}

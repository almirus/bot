package com.almirus.kvartalyBot.dal.entity;

import lombok.Data;

import javax.persistence.*;
import java.sql.Date;

@Data
@Entity
@Table(name = "debt")
public class Debt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "debt_id")
    private Long id;
    @Column(name = "apartment_or_car_place")
    private Integer objectNum;
    @Column(name = "actual_date")
    private Date actualDate;
    @Column(name = "sum")
    private String sum;
    @Column(name = "alerted")
    private Boolean alerted;
    @Column(name = "debt_type")
    private Byte debt_type;
}

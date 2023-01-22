package com.almirus.kvartalyBot.dal.entity;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.Instant;
@Data
@Entity
@Table(name = "news")
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id", nullable = false)
    private Integer id;

    @Column(name = "news_body")
    @Type(type = "org.hibernate.type.TextType")
    private String newsBody;

    @Column(name = "send_type")
    private Integer sendType;

    @Column(name = "create_date")
    private Instant createDate;

    @Column(name = "send_date")
    private Instant sendDate;

}
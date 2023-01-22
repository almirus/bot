package com.almirus.kvartalyBot.dal.entity;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "user_log")
public class LOG {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id", nullable = false)
    private Integer id;

    @Column(name = "telegram_id")
    private String telegramId;
    @Column(name = "log")
    @Type(type = "org.hibernate.type.TextType")
    private String logBody;
    @Column(name = "create_date")
    private Instant createDate;
    ;
}

-- we don't know how to generate root <with-no-name> (class Root) :(
create sequence owner_id_seq
    as integer;

alter sequence owner_id_seq owner to postgres;

create sequence tmp_owner_id_seq
    as integer;

alter sequence tmp_owner_id_seq owner to postgres;

create sequence tmp_owner_id_seq1
    as integer;

alter sequence tmp_owner_id_seq1 owner to postgres;

create table apartment
(
    building   integer not null,
    entrance   integer,
    section    integer,
    floor      integer,
    ddu_num    integer,
    room       varchar,
    ddu_area   real,
    real_num   integer not null,
    bti_area   real,
    real_area  real,
    difference real,
    primary key (building, real_num)
);

comment on table apartment is 'таблица от застройщика с номерами всех квартир';

alter table apartment
    owner to postgres;

create table apartment_owner
(
    apartment_real_num integer,
    owner_id           integer
);

comment on table apartment_owner is 'таблица связки квартиры и владельца, связь многие ко многим';

alter table apartment_owner
    owner to postgres;

create table owner
(
    id                    bigint    default nextval('owner_id_seq'::regclass) not null
        constraint users_pk
            primary key,
    phone_num             varchar,
    nick                  varchar,
    telegram_id           varchar,
    car_place             varchar,
    timestamp             timestamp default CURRENT_TIMESTAMP,
    activated_telegram_id varchar
);

comment on table owner is 'таблица с владельцами, зарегистрированными';

alter table owner
    owner to postgres;

alter sequence owner_id_seq owned by owner.id;

create table tmp_owner
(
    id          integer   default nextval('tmp_owner_id_seq1'::regclass) not null
        constraint tmp_owner_pk
            primary key,
    real_num    integer,
    phone_num   varchar,
    nick        varchar,
    telegram_id varchar,
    floor       integer,
    car_place   varchar,
    timestamp   timestamp default CURRENT_TIMESTAMP
);

comment on table tmp_owner is 'временная таблица, хранят пользователей которые или не завершили ввод всех данных или не активировали';

alter table tmp_owner
    owner to postgres;

alter sequence tmp_owner_id_seq1 owned by tmp_owner.id;

create unique index tmp_owner_id_uindex
    on tmp_owner (id);

create table debt
(
    apartment_or_car_place integer,
    actual_date            date,
    sum                    varchar,
    alerted                boolean,
    debt_id                bigserial,
    debt_type              integer
);

comment on table debt is 'долги';

alter table debt
    owner to postgres;

create table user_log
(
    telegram_id varchar,
    log         text,
    create_date timestamp,
    log_id      serial
);

alter table user_log
    owner to postgres;

create table news
(
    news_id     serial,
    news_body   text,
    send_type   integer,
    create_date timestamp,
    send_date   timestamp
);

alter table news
    owner to postgres;


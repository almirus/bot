create sequence owner_id_seq
    as integer;

alter sequence owner_id_seq owner to postgres;

create sequence tmp_owner_id_seq
    as integer;

alter sequence tmp_owner_id_seq owner to postgres;


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
    id          serial
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

create unique index tmp_owner_id_uindex
    on tmp_owner (id);

alter sequence tmp_owner_id_seq owned by tmp_owner.id;

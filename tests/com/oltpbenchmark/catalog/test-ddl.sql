CREATE TABLE CUSTOMER (
    C_ID           BIGINT NOT NULL,
    C_ID_STR       VARCHAR(64) UNIQUE NOT NULL,
    C_BALANCE      FLOAT NOT NULL,
    C_SATTR00      VARCHAR(32),
    C_SATTR01      VARCHAR(16),
    C_SATTR02      VARCHAR(8),
    C_IATTR00      TINYINT,
    C_IATTR01      SMALLINT,
    C_IATTR02      INT,
    C_IATTR03      BIGINT,
    PRIMARY KEY (C_ID)
);

CREATE TABLE FLIGHT (
    F_ID            BIGINT NOT NULL,
    F_DEPART_TIME   TIMESTAMP NOT NULL,
    F_ARRIVE_TIME   TIMESTAMP NOT NULL,
    F_STATUS        TINYINT NOT NULL,
    F_BASE_PRICE    FLOAT NOT NULL,
    PRIMARY KEY (F_ID)
);

CREATE TABLE RESERVATION (
    R_ID            BIGINT NOT NULL,
    R_C_ID          BIGINT NOT NULL REFERENCES CUSTOMER (C_ID),
    R_F_ID          BIGINT NOT NULL REFERENCES FLIGHT (F_ID),
    R_SEAT          BIGINT NOT NULL,
    UNIQUE (R_F_ID, R_SEAT),
    PRIMARY KEY (R_ID, R_C_ID, R_F_ID)
);
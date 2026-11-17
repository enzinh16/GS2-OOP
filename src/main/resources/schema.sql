CREATE TABLE IF NOT EXISTS medicao (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(30) NOT NULL,
    valor     DOUBLE      NOT NULL,
    unidade   VARCHAR(10) NOT NULL,
    data_hora TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS alerta (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id       VARCHAR(30) NOT NULL,
    tipo            VARCHAR(20) NOT NULL,
    nivel           VARCHAR(10) NOT NULL,
    valor_previsto  DOUBLE      NOT NULL,
    limite_atingido DOUBLE      NOT NULL,
    unidade         VARCHAR(10) NOT NULL,
    estrategia      VARCHAR(40) NOT NULL,
    data_hora       TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_medicao_sensor ON medicao (sensor_id, data_hora);

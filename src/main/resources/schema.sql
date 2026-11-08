CREATE TABLE IF NOT EXISTS medicao (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id  VARCHAR(20)      NOT NULL,
    tipo       VARCHAR(20)      NOT NULL,
    regiao     VARCHAR(60)      NOT NULL,
    valor      DOUBLE PRECISION NOT NULL,
    data_hora  TIMESTAMP        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_medicao_sensor_data ON medicao (sensor_id, data_hora);

CREATE TABLE IF NOT EXISTS alerta (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id      VARCHAR(20)      NOT NULL,
    tipo           VARCHAR(20)      NOT NULL,
    regiao         VARCHAR(60)      NOT NULL,
    nivel          VARCHAR(10)      NOT NULL,
    valor_previsto DOUBLE PRECISION NOT NULL,
    limite         DOUBLE PRECISION NOT NULL,
    estrategia     VARCHAR(120)     NOT NULL,
    justificativa  VARCHAR(1000)    NOT NULL,
    data_hora      TIMESTAMP        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_alerta_data ON alerta (data_hora);

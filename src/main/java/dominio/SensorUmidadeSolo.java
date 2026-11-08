package dominio;

import java.util.Random;

/**
 * Mede a umidade do solo em %, indicador de risco de deslizamento em encostas:
 * solo encharcado perde resistência.
 * Simulação: tende a 70% e varia com ruído aleatório.
 */
public class SensorUmidadeSolo extends SensorSimulado {

    private static final double UMIDADE_TIPICA = 70.0;

    public SensorUmidadeSolo(String id, Random random) {
        super(id, TipoSensor.UMIDADE_SOLO, 65.0, random);
    }

    @Override
    protected double proximoValor(double anterior, Random random) {
        return anterior + 0.1 * (UMIDADE_TIPICA - anterior) + random.nextGaussian() * 6.0;
    }
}

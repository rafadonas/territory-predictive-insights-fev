package dominio;

import java.util.Random;

/**
 * Mede o nível do córrego em metros.
 * Simulação: o nível tende a voltar ao normal (1,8 m) e oscila com variação aleatória,
 * o que às vezes leva a subidas seguidas, como numa cheia.
 */
public class ReguaNivelCorrego extends SensorSimulado {

    private static final double NIVEL_NORMAL = 1.8;

    public ReguaNivelCorrego(String id, Random random) {
        super(id, TipoSensor.NIVEL_CORREGO, NIVEL_NORMAL, random);
    }

    @Override
    protected double proximoValor(double anterior, Random random) {
        return anterior + 0.15 * (NIVEL_NORMAL - anterior) + random.nextGaussian() * 0.4;
    }
}

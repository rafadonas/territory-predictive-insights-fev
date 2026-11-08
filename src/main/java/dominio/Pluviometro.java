package dominio;

import java.util.Random;

/**
 * Mede a intensidade da chuva em mm/h.
 * Simulação: a chuva diminui aos poucos (80% do valor anterior) e, em 35% das leituras,
 * chega uma nova pancada de até 25 mm/h. Isso gera períodos secos e picos de temporal.
 */
public class Pluviometro extends SensorSimulado {

    public Pluviometro(String id, Random random) {
        super(id, TipoSensor.PLUVIOMETRO, 5.0, random);
    }

    @Override
    protected double proximoValor(double anterior, Random random) {
        double novaPancada = random.nextDouble() < 0.35 ? random.nextDouble() * 25.0 : 0.0;
        return anterior * 0.8 + novaPancada;
    }
}

package dominio;

import excecao.FonteIndisponivelException;

import java.util.Random;

/**
 * Base comum dos sensores simulados: concentra a parte repetida (falha de comunicação,
 * leitura espúria, guardar o último valor) e deixa para cada filho só a "física" do sensor.
 *
 * A cada leitura:
 *  - 5% de chance de o sensor não responder (FonteIndisponivelException);
 *  - 4% de chance de devolver um valor impossível (-1), como um equipamento com defeito,
 *    que será barrado pela validação da Medicao (DadosInvalidosException);
 *  - nos demais casos, o valor evolui a partir do anterior, em faixas realistas.
 */
public abstract class SensorSimulado implements Sensor {

    static final double CHANCE_FALHA = 0.05;
    static final double CHANCE_LEITURA_ESPURIA = 0.04;
    static final double VALOR_ESPURIO = -1.0;

    private final String id;
    private final TipoSensor tipo;
    private final Random random;
    private double ultimoValor;
    private boolean funcionando = true;

    protected SensorSimulado(String id, TipoSensor tipo, double valorInicial, Random random) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O sensor precisa de um identificador.");
        }
        this.id = id;
        this.tipo = tipo;
        this.ultimoValor = valorInicial;
        this.random = random;
    }

    @Override
    public final double lerValor() throws FonteIndisponivelException {
        double sorteio = random.nextDouble();
        if (sorteio < CHANCE_FALHA) {
            funcionando = false;
            throw new FonteIndisponivelException(
                    "Sensor " + id + " (" + tipo.getDescricao() + ") não respondeu à leitura.");
        }
        funcionando = true;
        if (sorteio < CHANCE_FALHA + CHANCE_LEITURA_ESPURIA) {
            return VALOR_ESPURIO;
        }
        ultimoValor = limitar(proximoValor(ultimoValor, random));
        return Math.round(ultimoValor * 10.0) / 10.0;
    }

    /** Cada tipo de sensor define como o valor evolui entre duas leituras. */
    protected abstract double proximoValor(double anterior, Random random);

    private double limitar(double valor) {
        return Math.max(tipo.getMinimoValido(), Math.min(tipo.getMaximoValido(), valor));
    }

    @Override
    public String getId() { return id; }

    @Override
    public TipoSensor getTipo() { return tipo; }

    @Override
    public boolean estaFuncionando() { return funcionando; }

    @Override
    public String toString() {
        return id + " - " + tipo.getDescricao();
    }
}

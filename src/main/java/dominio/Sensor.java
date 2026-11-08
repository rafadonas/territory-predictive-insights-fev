package dominio;

import excecao.FonteIndisponivelException;

/**
 * Contrato de qualquer sensor do território.
 * O resto do sistema (serviço, menu, testes) só conhece esta interface,
 * por isso um sensor novo entra sem mudar nenhuma outra classe.
 */
public interface Sensor {

    /** Identificador único do sensor (ex.: PLU-01). */
    String getId();

    /** Retorna o tipo do sensor, que define unidade e faixa válida. */
    TipoSensor getTipo();

    /**
     * Lê o valor atual. O valor bruto ainda não foi validado:
     * quem cria a Medicao é que decide se ele é fisicamente possível.
     *
     * @throws FonteIndisponivelException se o sensor não responder
     */
    double lerValor() throws FonteIndisponivelException;

    /** Indica se a última tentativa de leitura teve resposta. */
    boolean estaFuncionando();
}

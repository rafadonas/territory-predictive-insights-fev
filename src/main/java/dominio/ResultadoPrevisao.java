package dominio;

/**
 * Resultado de uma estratégia de previsão: o valor previsto (quando há dados suficientes)
 * e a explicação em linguagem simples do que ele significa.
 */
public final class ResultadoPrevisao {

    /** INSUFICIENTE: poucas leituras. APTO: pré-condição ok, filho ainda vai calcular. CALCULADA: há valor. */
    private enum Situacao { INSUFICIENTE, APTO, CALCULADA }

    private final String estrategia;
    private final Double valorPrevisto;
    private final Situacao situacao;
    private final int leiturasUsadas;
    private final String explicacao;

    private ResultadoPrevisao(String estrategia, Double valorPrevisto, Situacao situacao, int leiturasUsadas, String explicacao) {
        this.estrategia = estrategia;
        this.valorPrevisto = valorPrevisto;
        this.situacao = situacao;
        this.leiturasUsadas = leiturasUsadas;
        this.explicacao = explicacao;
    }

    /** Previsão calculada com histórico suficiente. */
    public static ResultadoPrevisao calculada(String estrategia, double valor, int leiturasUsadas, String explicacao) {
        return new ResultadoPrevisao(estrategia, valor, Situacao.CALCULADA, leiturasUsadas, explicacao);
    }

    /** O sistema se cala: há poucas leituras para afirmar qualquer coisa. */
    public static ResultadoPrevisao insuficiente(String estrategia, int leiturasDisponiveis, int minimo) {
        return new ResultadoPrevisao(estrategia, null, Situacao.INSUFICIENTE, leiturasDisponiveis, String.format(
                "Sem previsão: há %d leitura(s) no banco e são necessárias pelo menos %d. Com tão pouco histórico o resultado seria um chute.",
                leiturasDisponiveis, minimo));
    }

    /** Pré-condição atendida; usado pela classe-mãe antes de o filho calcular. Ainda não é confiável: não tem valor. */
    static ResultadoPrevisao apto(String estrategia, int leiturasDisponiveis) {
        return new ResultadoPrevisao(estrategia, null, Situacao.APTO, leiturasDisponiveis, "Histórico suficiente.");
    }

    /** Usado pelas estratégias-filhas: true quando a classe-mãe recusou o histórico. */
    boolean historicoInsuficiente() {
        return situacao == Situacao.INSUFICIENTE;
    }

    public String getEstrategia() { return estrategia; }
    public Double getValorPrevisto() { return valorPrevisto; }
    /** Só é confiável quando existe valor calculado com histórico suficiente. */
    public boolean isConfiavel() { return situacao == Situacao.CALCULADA; }
    public int getLeiturasUsadas() { return leiturasUsadas; }
    public String getExplicacao() { return explicacao; }
}

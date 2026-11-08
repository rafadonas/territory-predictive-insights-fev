package dominio;

import java.util.List;

/**
 * Família de algoritmos de previsão. O serviço trata todas as estratégias do mesmo jeito
 * (chama prever()); cada filho decide COMO prever. A classe-mãe faz a parte comum:
 * conferir se há histórico suficiente, porque prever com poucas leituras é chute.
 */
public abstract class EstrategiaPrevisao {

    private final String codigo;
    private final String nome;
    private final int minimoLeituras;

    protected EstrategiaPrevisao(String codigo, String nome, int minimoLeituras) {
        if (minimoLeituras < 1) {
            throw new IllegalArgumentException("A estratégia precisa de pelo menos 1 leitura.");
        }
        this.codigo = codigo;
        this.nome = nome;
        this.minimoLeituras = minimoLeituras;
    }

    /**
     * Regra comum a todas as estratégias. Os filhos sobrescrevem este método e chamam
     * super.prever() primeiro: se o histórico for insuficiente, devolvem esse resultado.
     *
     * @param historico medições de UM sensor, da mais antiga para a mais recente
     */
    public ResultadoPrevisao prever(List<Medicao> historico) {
        int disponiveis = historico == null ? 0 : historico.size();
        if (disponiveis < minimoLeituras) {
            return ResultadoPrevisao.insuficiente(nome, disponiveis, minimoLeituras);
        }
        return ResultadoPrevisao.apto(nome, disponiveis);
    }

    /** Média dos valores de medicoes[inicio, fim). */
    protected static double media(List<Medicao> medicoes, int inicio, int fim) {
        double soma = 0;
        for (int i = inicio; i < fim; i++) {
            soma += medicoes.get(i).getValor();
        }
        return soma / (fim - inicio);
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public int getMinimoLeituras() { return minimoLeituras; }
}

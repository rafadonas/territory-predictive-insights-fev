package dominio;

import java.util.List;

/**
 * Compara a média do período recente (últimas N leituras) com a do período anterior
 * (as N antes delas) e projeta que a mesma variação se repete.
 * Reage rápido a tendências (ex.: córrego subindo), mas exagera se a variação for ruído.
 */
public class PrevisaoComparacao extends EstrategiaPrevisao {

    public static final String CODIGO = "COMPARACAO";
    private final int janela;

    public PrevisaoComparacao(int janela) {
        super(CODIGO, "Comparação com o período anterior (" + janela + " + " + janela + " leituras)", janela * 2);
        this.janela = janela;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) {
        ResultadoPrevisao verificacao = super.prever(historico);
        if (verificacao.historicoInsuficiente()) {
            return verificacao;
        }
        int fim = historico.size();
        double mediaRecente = media(historico, fim - janela, fim);
        double mediaAnterior = media(historico, fim - 2 * janela, fim - janela);
        double variacao = mediaRecente - mediaAnterior;
        TipoSensor tipo = historico.get(fim - 1).getTipo();

        // Projeção limitada à faixa física: chuva negativa ou umidade acima de 100% não existem.
        double previsto = Math.max(tipo.getMinimoValido(), Math.min(tipo.getMaximoValido(), mediaRecente + variacao));

        String sentido = variacao > 0 ? "subiu" : variacao < 0 ? "caiu" : "ficou estável em";
        String explicacao = String.format(
                "Período recente: %s; período anterior: %s; %s %s. Se a mesma variação se repetir, deve chegar a %s.",
                tipo.formatar(mediaRecente), tipo.formatar(mediaAnterior), sentido,
                tipo.formatar(Math.abs(variacao)), tipo.formatar(previsto));
        return ResultadoPrevisao.calculada(getNome(), previsto, janela * 2, explicacao);
    }
}

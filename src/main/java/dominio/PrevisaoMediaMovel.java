package dominio;

import java.util.List;

/**
 * Prevê a próxima leitura como a média das últimas N medições do histórico.
 * Suaviza picos isolados; reage devagar a mudanças bruscas.
 */
public class PrevisaoMediaMovel extends EstrategiaPrevisao {

    public static final String CODIGO = "MEDIA_MOVEL";
    private final int janela;

    public PrevisaoMediaMovel(int janela) {
        super(CODIGO, "Média móvel (" + janela + " leituras)", janela);
        this.janela = janela;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) {
        ResultadoPrevisao verificacao = super.prever(historico);
        if (verificacao.historicoInsuficiente()) {
            return verificacao;
        }
        int fim = historico.size();
        double previsto = media(historico, fim - janela, fim);
        TipoSensor tipo = historico.get(fim - 1).getTipo();
        String explicacao = String.format(
                "Média das últimas %d leituras de %s: %s. Mantido o padrão recente, a próxima leitura deve ficar perto desse valor.",
                janela, tipo.getGrandeza(), tipo.formatar(previsto));
        return ResultadoPrevisao.calculada(getNome(), previsto, janela, explicacao);
    }
}

package servico;

import dominio.DadosTerritorio;
import dominio.Limiares;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;

import java.util.List;

/**
 * SINGLETON: única instância com a configuração do sistema (território, limiares,
 * tamanho das janelas de previsão e estratégias ativas).
 *
 * Por que Singleton aqui: limiar é uma regra que precisa ser a MESMA para o serviço que gera
 * alertas, para o menu que os exibe e para quem os altera. Com várias instâncias, uma tela
 * poderia mostrar 30 mm/h enquanto o serviço usa 50 mm/h. O construtor é privado:
 * a única forma de obter a configuração é getInstancia().
 */
public final class ConfiguracaoSistema {

    /** Código padrão para alterar limiares; pode ser trocado pela variável de ambiente VIGIACHUVA_CODIGO. */
    public static final String CODIGO_PADRAO = "GS2026";

    private static ConfiguracaoSistema instancia;

    private DadosTerritorio territorio;
    private final int janelaMediaMovel = 5;
    private final int janelaComparacao = 3;
    private final int limiteHistorico = 24;
    private final List<String> estrategiasAtivas = List.of(PrevisaoMediaMovel.CODIGO, PrevisaoComparacao.CODIGO);

    private ConfiguracaoSistema() {
        restaurarPadroes();
    }

    public static synchronized ConfiguracaoSistema getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracaoSistema();
        }
        return instancia;
    }

    /** Volta ao território e aos limiares padrão (usado na inicialização e nos testes). */
    public synchronized void restaurarPadroes() {
        String codigo = System.getenv().getOrDefault("VIGIACHUVA_CODIGO", CODIGO_PADRAO);
        try {
            territorio = new DadosTerritorio(
                    "Várzea Leste (território-piloto fictício)", "São Paulo/SP", 48_000, 6.5, 12, codigo);
        } catch (DadosInvalidosException e) {
            // Os valores acima são constantes do programa: se forem inválidos, é erro de programação.
            throw new IllegalStateException("Configuração padrão inválida: " + e.getMessage(), e);
        }
    }

    public DadosTerritorio getTerritorio() { return territorio; }
    public Limiares getLimiares(TipoSensor tipo) { return territorio.getLimiares(tipo); }
    public int getJanelaMediaMovel() { return janelaMediaMovel; }
    public int getJanelaComparacao() { return janelaComparacao; }
    public int getLimiteHistorico() { return limiteHistorico; }
    public List<String> getEstrategiasAtivas() { return estrategiasAtivas; }
}

package servico;

import dominio.EstrategiaPrevisao;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * FACTORY: cria a estratégia de previsão a partir do código configurado.
 *
 * Por que Factory aqui: cada estratégia precisa de parâmetros diferentes (janela de 5 ou 3+3)
 * vindos da configuração. Sem a fábrica, quem usa teria um "if (tipo == ...) new ..." espalhado.
 * Aqui a escolha é uma consulta a um mapa: para criar uma estratégia nova basta registrar
 * mais uma linha no CATALOGO, sem if/else nem instanceof em lugar nenhum.
 */
public final class FabricaPrevisao {

    private static final Map<String, Function<ConfiguracaoSistema, EstrategiaPrevisao>> CATALOGO = new LinkedHashMap<>();

    static {
        CATALOGO.put(PrevisaoMediaMovel.CODIGO, config -> new PrevisaoMediaMovel(config.getJanelaMediaMovel()));
        CATALOGO.put(PrevisaoComparacao.CODIGO, config -> new PrevisaoComparacao(config.getJanelaComparacao()));
    }

    private FabricaPrevisao() {
    }

    public static EstrategiaPrevisao criar(String codigo) throws DadosInvalidosException {
        Function<ConfiguracaoSistema, EstrategiaPrevisao> construtor = CATALOGO.get(codigo);
        if (construtor == null) {
            throw new DadosInvalidosException("Estratégia de previsão desconhecida: '" + codigo
                    + "'. Disponíveis: " + CATALOGO.keySet() + ".");
        }
        return construtor.apply(ConfiguracaoSistema.getInstancia());
    }

    /** Cria todas as estratégias marcadas como ativas na configuração. */
    public static List<EstrategiaPrevisao> criarEstrategiasAtivas() throws DadosInvalidosException {
        List<EstrategiaPrevisao> estrategias = new ArrayList<>();
        for (String codigo : ConfiguracaoSistema.getInstancia().getEstrategiasAtivas()) {
            estrategias.add(criar(codigo));
        }
        return estrategias;
    }

    public static Set<String> codigosDisponiveis() {
        return CATALOGO.keySet();
    }
}

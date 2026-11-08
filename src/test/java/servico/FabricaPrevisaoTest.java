package servico;

import dominio.EstrategiaPrevisao;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricaPrevisaoTest {

    @Test
    @DisplayName("A fábrica cria cada estratégia a partir do código, com a janela da configuração")
    void criaPorCodigo() throws DadosInvalidosException {
        // Act
        EstrategiaPrevisao media = FabricaPrevisao.criar(PrevisaoMediaMovel.CODIGO);
        EstrategiaPrevisao comparacao = FabricaPrevisao.criar(PrevisaoComparacao.CODIGO);

        // Assert
        assertEquals(PrevisaoMediaMovel.class, media.getClass());
        assertEquals(5, media.getMinimoLeituras());
        assertEquals(PrevisaoComparacao.class, comparacao.getClass());
        assertEquals(6, comparacao.getMinimoLeituras());
    }

    @Test
    @DisplayName("Código desconhecido lança DadosInvalidosException listando as opções")
    void codigoDesconhecido() {
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () -> FabricaPrevisao.criar("BOLA_DE_CRISTAL"));
        assertTrue(erro.getMessage().contains("MEDIA_MOVEL"), erro.getMessage());
    }

    @Test
    @DisplayName("Cria todas as estratégias ativas, e o Singleton devolve sempre a mesma configuração")
    void estrategiasAtivasESingleton() throws DadosInvalidosException {
        List<EstrategiaPrevisao> ativas = FabricaPrevisao.criarEstrategiasAtivas();
        assertEquals(2, ativas.size());
        assertSame(ConfiguracaoSistema.getInstancia(), ConfiguracaoSistema.getInstancia());
    }
}

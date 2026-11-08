package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoMediaMovelTest {

    @Test
    @DisplayName("Prevê a média das últimas 5 leituras, ignorando as mais antigas")
    void calculaMediaDasUltimasLeituras() throws DadosInvalidosException {
        // Arrange: 7 leituras; as 5 últimas são 10, 20, 30, 40, 50 (média 30)
        List<Medicao> historico = Historico.de("PLU-01", TipoSensor.PLUVIOMETRO, 100, 100, 10, 20, 30, 40, 50);
        EstrategiaPrevisao estrategia = new PrevisaoMediaMovel(5);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(historico);

        // Assert
        assertTrue(resultado.isConfiavel());
        assertEquals(30.0, resultado.getValorPrevisto(), 1e-9);
        assertEquals(5, resultado.getLeiturasUsadas());
    }

    @Test
    @DisplayName("Com menos de 5 leituras o sistema se cala (sem valor previsto)")
    void historicoInsuficienteNaoPreve() throws DadosInvalidosException {
        // Arrange
        List<Medicao> historico = Historico.de("PLU-01", TipoSensor.PLUVIOMETRO, 60, 70, 80, 90);

        // Act
        ResultadoPrevisao resultado = new PrevisaoMediaMovel(5).prever(historico);

        // Assert
        assertFalse(resultado.isConfiavel());
        assertNull(resultado.getValorPrevisto());
        assertTrue(resultado.getExplicacao().contains("4 leitura"), resultado.getExplicacao());
    }

    @Test
    @DisplayName("Histórico nulo ou vazio é tratado como insuficiente, sem exceção")
    void historicoVazio() {
        assertFalse(new PrevisaoMediaMovel(5).prever(null).isConfiavel());
        assertFalse(new PrevisaoMediaMovel(5).prever(List.of()).isConfiavel());
    }
}

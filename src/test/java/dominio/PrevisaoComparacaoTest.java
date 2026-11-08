package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoComparacaoTest {

    @Test
    @DisplayName("Projeta a tendência de alta: recente 3,0 m, anterior 2,0 m, previsão 4,0 m")
    void projetaTendenciaDeAlta() throws DadosInvalidosException {
        // Arrange
        List<Medicao> historico = Historico.de("NIV-01", TipoSensor.NIVEL_CORREGO, 1.9, 2.0, 2.1, 2.9, 3.0, 3.1);
        EstrategiaPrevisao estrategia = new PrevisaoComparacao(3);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(historico);

        // Assert
        assertTrue(resultado.isConfiavel());
        assertEquals(4.0, resultado.getValorPrevisto(), 1e-9);
        assertTrue(resultado.getExplicacao().contains("subiu"), resultado.getExplicacao());
    }

    @Test
    @DisplayName("Tendência de queda não gera previsão abaixo do mínimo físico (chuva negativa)")
    void naoPreveAbaixoDoMinimoFisico() throws DadosInvalidosException {
        // Arrange: média anterior 40, recente 5 -> projeção seria -30
        List<Medicao> historico = Historico.de("PLU-01", TipoSensor.PLUVIOMETRO, 40, 40, 40, 5, 5, 5);

        // Act
        ResultadoPrevisao resultado = new PrevisaoComparacao(3).prever(historico);

        // Assert
        assertEquals(0.0, resultado.getValorPrevisto(), 1e-9);
    }

    @Test
    @DisplayName("Precisa de duas janelas completas (6 leituras) para comparar")
    void exigeDuasJanelas() throws DadosInvalidosException {
        List<Medicao> historico = Historico.de("PLU-01", TipoSensor.PLUVIOMETRO, 1, 2, 3, 4, 5);
        assertFalse(new PrevisaoComparacao(3).prever(historico).isConfiavel());
    }
}

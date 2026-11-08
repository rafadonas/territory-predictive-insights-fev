package dominio;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 7, 15, 0);

    @Test
    @DisplayName("Cria medição válida com os dados informados")
    void criaMedicaoValida() throws DadosInvalidosException {
        // Arrange
        double chuva = 42.5;

        // Act
        Medicao medicao = new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, "Baixada", chuva, AGORA);

        // Assert
        assertEquals(42.5, medicao.getValor());
        assertEquals("PLU-01", medicao.getSensorId());
    }

    @Test
    @DisplayName("Chuva negativa lança DadosInvalidosException com mensagem explicativa")
    void chuvaNegativaLancaExcecao() {
        // Arrange
        double chuvaNegativa = -1.0;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, "Baixada", chuvaNegativa, AGORA));

        // Assert
        assertTrue(erro.getMessage().contains("chuva"), erro.getMessage());
        assertTrue(erro.getMessage().contains("PLU-01"), erro.getMessage());
    }

    @Test
    @DisplayName("Umidade acima de 100% e valor NaN são rejeitados")
    void valoresImpossiveisSaoRejeitados() {
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao("UMI-01", TipoSensor.UMIDADE_SOLO, "Encosta", 100.1, AGORA));
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao("NIV-01", TipoSensor.NIVEL_CORREGO, "Baixada", Double.NaN, AGORA));
    }

    @Test
    @DisplayName("toString formata data e hora mesmo com segundos zerados")
    void toStringComSegundosZerados() throws DadosInvalidosException {
        // Arrange: LocalDateTime com segundos = 0 (o toString() nativo omitiria os segundos)
        Medicao medicao = new Medicao("NIV-01", TipoSensor.NIVEL_CORREGO, "Baixada", 2.5, AGORA);

        // Act
        String texto = medicao.toString();

        // Assert
        assertEquals("07/11/2026 15:00:00 | NIV-01 | " + TipoSensor.NIVEL_CORREGO.formatar(2.5), texto);
    }

    @Test
    @DisplayName("Medição sem região é rejeitada")
    void semRegiaoLancaExcecao() {
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao("NIV-01", TipoSensor.NIVEL_CORREGO, " ", 2.0, AGORA));
    }
}

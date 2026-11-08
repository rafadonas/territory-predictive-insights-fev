package dominio;

import excecao.AcessoNegadoException;
import excecao.DadosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DadosTerritorioTest {

    private DadosTerritorio territorio;

    @BeforeEach
    void criarTerritorio() throws DadosInvalidosException {
        territorio = new DadosTerritorio("Várzea Teste", "São Paulo/SP", 1000, 2.0, 3, "1234");
    }

    @Test
    @DisplayName("População negativa é rejeitada no setter")
    void populacaoNegativa() {
        assertThrows(DadosInvalidosException.class, () -> territorio.setPopulacaoEstimada(-5));
        assertEquals(1000, territorio.getPopulacaoEstimada());
    }

    @Test
    @DisplayName("Código de acesso errado impede a alteração dos limiares")
    void codigoErradoNegaAcesso() throws DadosInvalidosException {
        // Arrange
        Limiares novos = new Limiares(1, 2, 3);
        Limiares anteriores = territorio.getLimiares(TipoSensor.PLUVIOMETRO);

        // Act + Assert
        assertThrows(AcessoNegadoException.class,
                () -> territorio.alterarLimiares("errado", TipoSensor.PLUVIOMETRO, novos));
        assertEquals(anteriores, territorio.getLimiares(TipoSensor.PLUVIOMETRO));
    }

    @Test
    @DisplayName("Com o código certo os limiares mudam; acima da faixa física são rejeitados")
    void alteraLimiaresComCodigo() throws Exception {
        territorio.alterarLimiares("1234", TipoSensor.NIVEL_CORREGO, new Limiares(2.0, 3.0, 4.0));
        assertEquals(4.0, territorio.getLimiares(TipoSensor.NIVEL_CORREGO).getCritico());

        assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiares("1234", TipoSensor.UMIDADE_SOLO, new Limiares(90, 95, 120)));
    }

    @Test
    @DisplayName("Limiares fora de ordem lançam DadosInvalidosException")
    void limiaresForaDeOrdem() {
        assertThrows(DadosInvalidosException.class, () -> new Limiares(30, 10, 50));
    }

    @Test
    @DisplayName("Limiares classificam o valor no nível mais grave atingido")
    void classificacaoPorNivel() throws DadosInvalidosException {
        Limiares limiares = new Limiares(10, 30, 50);
        assertEquals(Optional.empty(), limiares.classificar(9.9));
        assertEquals(Optional.of(NivelAlerta.ATENCAO), limiares.classificar(10));
        assertEquals(Optional.of(NivelAlerta.ALERTA), limiares.classificar(49.9));
        assertEquals(Optional.of(NivelAlerta.CRITICO), limiares.classificar(80));
    }
}

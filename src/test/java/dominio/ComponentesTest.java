package dominio;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentesTest {

    /** Sensor controlado pelo teste: responde ou não conforme o campo. */
    private static final class SensorFixo implements Sensor {
        private final boolean responde;
        SensorFixo(boolean responde) { this.responde = responde; }
        public String getId() { return "FIX"; }
        public TipoSensor getTipo() { return TipoSensor.PLUVIOMETRO; }
        public double lerValor() throws FonteIndisponivelException {
            if (!responde) throw new FonteIndisponivelException("sem resposta");
            return 1.0;
        }
        public boolean estaFuncionando() { return responde; }
    }

    @Test
    @DisplayName("Polimorfismo: cada componente avalia o próprio status")
    void statusPolimorfico() throws DadosInvalidosException {
        // Arrange
        PontoMonitoramento ponto = new PontoMonitoramento("PM", "Ponte", "Baixada");
        ponto.instalar(new SensorFixo(true));
        ponto.instalar(new SensorFixo(false));
        Piscinao piscinao = new Piscinao("PSC", "Piscinão", "Baixada", 1000);
        piscinao.setVolumeAtualM3(980);
        List<ComponenteTerritorial> componentes = List.of(ponto, piscinao);

        // Act
        List<StatusComponente> status = componentes.stream().map(ComponenteTerritorial::verificarStatus).toList();

        // Assert
        assertEquals(List.of(StatusComponente.COM_RESTRICAO, StatusComponente.INOPERANTE), status);
    }

    @Test
    @DisplayName("Sensor nulo e capacidade infinita são rejeitados")
    void rejeitaSensorNuloECapacidadeInfinita() throws DadosInvalidosException {
        PontoMonitoramento ponto = new PontoMonitoramento("PM", "Ponte", "Baixada");
        assertThrows(DadosInvalidosException.class, () -> ponto.instalar(null));
        assertThrows(DadosInvalidosException.class, () -> new Piscinao("PSC", "Piscinão", "Baixada", Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("Volume acima da capacidade do piscinão é rejeitado")
    void volumeInvalido() throws DadosInvalidosException {
        Piscinao piscinao = new Piscinao("PSC", "Piscinão", "Baixada", 1000);
        assertThrows(DadosInvalidosException.class, () -> piscinao.setVolumeAtualM3(1001));
    }

    @Test
    @DisplayName("Sensores simulados ficam sempre na faixa válida ou falham de forma explícita")
    void sensoresSimuladosRespeitamFaixa() {
        List<Sensor> sensores = List.of(
                new Pluviometro("P", new Random(7)), new ReguaNivelCorrego("N", new Random(7)), new SensorUmidadeSolo("U", new Random(7)));
        for (Sensor s : sensores) {
            for (int i = 0; i < 500; i++) {
                try {
                    double v = s.lerValor();
                    boolean valido = s.getTipo().aceita(v) || v == SensorSimulado.VALOR_ESPURIO;
                    assertTrue(valido, s.getId() + " leu " + v);
                } catch (FonteIndisponivelException e) {
                    assertFalse(s.estaFuncionando());
                }
            }
        }
    }
}

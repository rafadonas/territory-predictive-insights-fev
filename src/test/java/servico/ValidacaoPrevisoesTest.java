package servico;

import dominio.EstrategiaPrevisao;
import dominio.Limiares;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.Pluviometro;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import dominio.ReguaNivelCorrego;
import dominio.ResultadoPrevisao;
import dominio.Sensor;
import dominio.SensorUmidadeSolo;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PB-16: valida as duas estratégias sobre séries simuladas (semente fixa, resultado reprodutível).
 * Para cada instante t, prevê a leitura t+1 com as leituras até t e compara com o valor real:
 *  - MAE (erro médio absoluto);
 *  - acerto do alerta: o nível previsto (nenhum/ATENÇÃO/ALERTA/CRÍTICO) é igual ao nível real?
 *  - falso alarme: previu alerta e o real ficou abaixo de ATENÇÃO;
 *  - alerta perdido: não previu alerta e o real atingiu ATENÇÃO ou mais.
 * Os números vão para o README (seção "Validação das previsões").
 */
class ValidacaoPrevisoesTest {

    private static final int LEITURAS = 600;

    private record Resultado(double mae, int previsoes, int acertos, int falsosAlarmes, int perdidos) {
        double taxaAcerto() { return 100.0 * acertos / previsoes; }
    }

    @Test
    @DisplayName("PB-16: erro médio absoluto e acerto dos alertas das duas estratégias")
    void validarEstrategias() throws Exception {
        List<String> linhas = new ArrayList<>();
        linhas.add("| Sensor | Estratégia | Previsões | MAE | Acerto do nível | Falsos alarmes | Alertas perdidos |");
        linhas.add("|---|---|---|---|---|---|---|");
        Object[][] cenarios = {
                {new Pluviometro("PLU-V", new Random(42)), new Limiares(10, 30, 50)},
                {new ReguaNivelCorrego("NIV-V", new Random(42)), new Limiares(2.5, 3.2, 3.8)},
                {new SensorUmidadeSolo("UMI-V", new Random(42)), new Limiares(80, 90, 95)},
        };
        for (Object[] c : cenarios) {
            Sensor sensor = (Sensor) c[0];
            Limiares limiares = (Limiares) c[1];
            List<Medicao> serie = gerarSerie(sensor);
            for (EstrategiaPrevisao e : List.of(new PrevisaoMediaMovel(5), new PrevisaoComparacao(3))) {
                Resultado r = avaliar(serie, e, limiares);
                // Teste de verdade: o erro precisa ser finito e menor que a amplitude total da série.
                double amplitude = serie.stream().mapToDouble(Medicao::getValor).max().orElse(0)
                        - serie.stream().mapToDouble(Medicao::getValor).min().orElse(0);
                assertTrue(Double.isFinite(r.mae()) && r.mae() < amplitude, e.getNome() + " MAE=" + r.mae());
                linhas.add(String.format("| %s | %s | %d | %.2f %s | %.1f%% | %d | %d |",
                        sensor.getTipo().getGrandeza(), e.getNome(), r.previsoes(), r.mae(), sensor.getTipo().getUnidade(),
                        r.taxaAcerto(), r.falsosAlarmes(), r.perdidos()).replace('.', ','));
            }
        }
        System.out.println(String.join(System.lineSeparator(), linhas));
    }

    /** Lê o sensor até juntar LEITURAS medições válidas (falhas e leituras espúrias são descartadas, como no sistema). */
    private static List<Medicao> gerarSerie(Sensor sensor) throws DadosInvalidosException {
        List<Medicao> serie = new ArrayList<>();
        LocalDateTime t = LocalDateTime.of(2026, 11, 1, 0, 0);
        while (serie.size() < LEITURAS) {
            try {
                double v = sensor.lerValor();
                if (sensor.getTipo().aceita(v)) {
                    serie.add(new Medicao(sensor.getId(), sensor.getTipo(), "Validação", v, t));
                }
            } catch (FonteIndisponivelException e) {
                // sensor sem resposta: nada a gravar, segue para a próxima leitura
            }
            t = t.plusMinutes(10);
        }
        return serie;
    }

    private static Resultado avaliar(List<Medicao> serie, EstrategiaPrevisao estrategia, Limiares limiares) {
        double somaErro = 0;
        int n = 0, acertos = 0, falsos = 0, perdidos = 0;
        for (int t = 1; t < serie.size(); t++) {
            ResultadoPrevisao p = estrategia.prever(serie.subList(0, t));
            if (!p.isConfiavel()) continue;
            double real = serie.get(t).getValor();
            somaErro += Math.abs(p.getValorPrevisto() - real);
            Optional<NivelAlerta> previsto = limiares.classificar(p.getValorPrevisto());
            Optional<NivelAlerta> observado = limiares.classificar(real);
            if (previsto.equals(observado)) acertos++;
            if (previsto.isPresent() && observado.isEmpty()) falsos++;
            if (previsto.isEmpty() && observado.isPresent()) perdidos++;
            n++;
        }
        return new Resultado(somaErro / n, n, acertos, falsos, perdidos);
    }
}

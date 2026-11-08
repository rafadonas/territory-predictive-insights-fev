package servico;

import dominio.Alerta;
import dominio.EstrategiaPrevisao;
import dominio.Limiares;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.ResultadoPrevisao;
import dominio.Sensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import persistencia.AlertaDAO;
import persistencia.MedicaoDAO;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Regras de negócio: ler sensores, gravar medições, rodar previsões e emitir alertas.
 * Recebe DAOs e estratégias pelo construtor (injeção de dependência): assim os testes
 * passam DAOs falsos (Mockito) e testam a lógica de alerta sem banco de dados.
 */
public class ServicoMonitoramento {

    private final MedicaoDAO medicaoDAO;
    private final AlertaDAO alertaDAO;
    private final List<EstrategiaPrevisao> estrategias;
    private final ConfiguracaoSistema configuracao;
    private final Clock relogio;

    public ServicoMonitoramento(MedicaoDAO medicaoDAO, AlertaDAO alertaDAO, List<EstrategiaPrevisao> estrategias) {
        this(medicaoDAO, alertaDAO, estrategias, ConfiguracaoSistema.getInstancia(), Clock.systemDefaultZone());
    }

    public ServicoMonitoramento(MedicaoDAO medicaoDAO, AlertaDAO alertaDAO, List<EstrategiaPrevisao> estrategias,
                                ConfiguracaoSistema configuracao, Clock relogio) {
        if (estrategias == null || estrategias.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma estratégia de previsão.");
        }
        this.medicaoDAO = medicaoDAO;
        this.alertaDAO = alertaDAO;
        this.estrategias = List.copyOf(estrategias);
        this.configuracao = configuracao;
        this.relogio = relogio;
    }

    /**
     * Lê o sensor, valida e grava a medição.
     *
     * @throws FonteIndisponivelException sensor não respondeu (nada é gravado)
     * @throws DadosInvalidosException    valor fisicamente impossível (descartado, nada é gravado)
     */
    public Medicao registrarLeitura(Sensor sensor, String regiao) throws FonteIndisponivelException, DadosInvalidosException {
        double valor = sensor.lerValor();
        Medicao medicao = new Medicao(sensor.getId(), sensor.getTipo(), regiao, valor, LocalDateTime.now(relogio));
        return medicaoDAO.salvar(medicao);
    }

    /**
     * Roda todas as estratégias sobre o histórico do sensor guardado no banco.
     * Se alguma previsão confiável atingir um limiar, grava UM alerta com o nível mais grave
     * e a justificativa completa (sensor, valor previsto, limite e estratégia).
     */
    public ResultadoAnalise analisarSensor(Sensor sensor, String regiao) {
        List<Medicao> historico = medicaoDAO.buscarHistoricoPorSensor(sensor.getId(), configuracao.getLimiteHistorico());
        Limiares limiares = configuracao.getLimiares(sensor.getTipo());

        List<ResultadoPrevisao> previsoes = new ArrayList<>();
        ResultadoPrevisao maisGrave = null;
        NivelAlerta nivelMaisGrave = null;

        for (EstrategiaPrevisao estrategia : estrategias) {
            ResultadoPrevisao previsao = estrategia.prever(historico);
            previsoes.add(previsao);
            if (!previsao.isConfiavel()) {
                continue; // sem histórico suficiente: o sistema se cala para esta estratégia
            }
            Optional<NivelAlerta> nivel = limiares.classificar(previsao.getValorPrevisto());
            if (nivel.isPresent() && (nivelMaisGrave == null || nivel.get().compareTo(nivelMaisGrave) > 0)) {
                nivelMaisGrave = nivel.get();
                maisGrave = previsao;
            }
        }

        Alerta alerta = null;
        if (maisGrave != null) {
            alerta = alertaDAO.salvar(criarAlerta(sensor, regiao, maisGrave, nivelMaisGrave, limiares, previsoes));
        }
        return new ResultadoAnalise(sensor.getId(), sensor.getTipo(), regiao, previsoes, alerta);
    }

    private Alerta criarAlerta(Sensor sensor, String regiao, ResultadoPrevisao base, NivelAlerta nivel,
                               Limiares limiares, List<ResultadoPrevisao> todas) {
        double limite = limiares.limiteDe(nivel);
        String outras = todas.stream()
                .filter(p -> p != base && p.isConfiavel())
                .map(p -> p.getEstrategia() + " = " + sensor.getTipo().formatar(p.getValorPrevisto()))
                .collect(Collectors.joining("; "));
        String justificativa = String.format(
                "Sensor %s (%s) na região %s: a previsão de %s por %s atingiu o limite de %s (%s).%s "
                        + "Ação sugerida: %s A decisão final é da equipe responsável.",
                sensor.getId(), sensor.getTipo().getGrandeza(), regiao,
                sensor.getTipo().formatar(base.getValorPrevisto()), base.getEstrategia(),
                nivel.getRotulo(), sensor.getTipo().formatar(limite),
                outras.isEmpty() ? "" : " Outra estimativa: " + outras + ".",
                nivel.getAcaoSugerida());
        return new Alerta(null, sensor.getId(), sensor.getTipo(), regiao, nivel, base.getValorPrevisto(), limite,
                base.getEstrategia(), justificativa, LocalDateTime.now(relogio));
    }

    public List<Medicao> historicoPorSensor(String sensorId) {
        return medicaoDAO.buscarHistoricoPorSensor(sensorId, configuracao.getLimiteHistorico());
    }

    public List<Alerta> alertasEmitidos(int limite) {
        return alertaDAO.listarRecentes(limite);
    }

    public int totalMedicoes() { return medicaoDAO.contarTodas(); }
    public int totalAlertas() { return alertaDAO.contarTodos(); }
    public int totalMedicoesDoSensor(String sensorId) { return medicaoDAO.contarPorSensor(sensorId); }
    public List<EstrategiaPrevisao> getEstrategias() { return estrategias; }
}

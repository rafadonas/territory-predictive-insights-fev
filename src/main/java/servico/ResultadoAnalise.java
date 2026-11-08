package servico;

import dominio.Alerta;
import dominio.ResultadoPrevisao;
import dominio.TipoSensor;

import java.util.List;
import java.util.Optional;

/** Resultado da análise de um sensor: as previsões de cada estratégia e o alerta, se houve. */
public final class ResultadoAnalise {

    private final String sensorId;
    private final TipoSensor tipo;
    private final String regiao;
    private final List<ResultadoPrevisao> previsoes;
    private final Alerta alerta;

    public ResultadoAnalise(String sensorId, TipoSensor tipo, String regiao, List<ResultadoPrevisao> previsoes, Alerta alerta) {
        this.sensorId = sensorId;
        this.tipo = tipo;
        this.regiao = regiao;
        this.previsoes = List.copyOf(previsoes);
        this.alerta = alerta;
    }

    public String getSensorId() { return sensorId; }
    public TipoSensor getTipo() { return tipo; }
    public String getRegiao() { return regiao; }
    public List<ResultadoPrevisao> getPrevisoes() { return previsoes; }
    public Optional<Alerta> getAlerta() { return Optional.ofNullable(alerta); }
}

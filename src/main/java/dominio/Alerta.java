package dominio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Alerta emitido quando uma previsão atinge um limiar.
 * Guarda tudo o que explica o porquê: sensor, valor previsto, limite ultrapassado e estratégia.
 */
public class Alerta {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long id;
    private final String sensorId;
    private final TipoSensor tipo;
    private final String regiao;
    private final NivelAlerta nivel;
    private final double valorPrevisto;
    private final double limite;
    private final String estrategia;
    private final String justificativa;
    private final LocalDateTime dataHora;

    public Alerta(Long id, String sensorId, TipoSensor tipo, String regiao, NivelAlerta nivel, double valorPrevisto,
                  double limite, String estrategia, String justificativa, LocalDateTime dataHora) {
        this.id = id;
        this.sensorId = Objects.requireNonNull(sensorId, "sensorId");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.regiao = Objects.requireNonNull(regiao, "regiao");
        this.nivel = Objects.requireNonNull(nivel, "nivel");
        this.valorPrevisto = valorPrevisto;
        this.limite = limite;
        this.estrategia = Objects.requireNonNull(estrategia, "estrategia");
        this.justificativa = Objects.requireNonNull(justificativa, "justificativa");
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora");
    }

    public Long getId() { return id; }
    public void definirIdGerado(long idGerado) { this.id = idGerado; }
    public String getSensorId() { return sensorId; }
    public TipoSensor getTipo() { return tipo; }
    public String getRegiao() { return regiao; }
    public NivelAlerta getNivel() { return nivel; }
    public double getValorPrevisto() { return valorPrevisto; }
    public double getLimite() { return limite; }
    public String getEstrategia() { return estrategia; }
    public String getJustificativa() { return justificativa; }
    public LocalDateTime getDataHora() { return dataHora; }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s (%s) | previsto %s, limite %s | %s",
                nivel.getRotulo(), dataHora.format(FORMATO), sensorId, regiao,
                tipo.formatar(valorPrevisto), tipo.formatar(limite), estrategia);
    }
}

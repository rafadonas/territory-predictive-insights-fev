package dominio;

import excecao.DadosInvalidosException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Uma leitura de sensor já validada. Se o objeto existe, o dado é fisicamente possível:
 * a validação acontece no construtor e não há setters para "estragar" depois.
 */
public class Medicao {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Long id;
    private final String sensorId;
    private final TipoSensor tipo;
    private final String regiao;
    private final double valor;
    private final LocalDateTime dataHora;

    public Medicao(String sensorId, TipoSensor tipo, String regiao, double valor, LocalDateTime dataHora)
            throws DadosInvalidosException {
        this(null, sensorId, tipo, regiao, valor, dataHora);
    }

    /** Usado pelo DAO ao reconstruir a medição lida do banco. */
    public Medicao(Long id, String sensorId, TipoSensor tipo, String regiao, double valor, LocalDateTime dataHora)
            throws DadosInvalidosException {
        if (sensorId == null || sensorId.isBlank()) {
            throw new DadosInvalidosException("Medição sem identificação do sensor.");
        }
        if (tipo == null) {
            throw new DadosInvalidosException("Medição do sensor " + sensorId + " sem tipo definido.");
        }
        if (regiao == null || regiao.isBlank()) {
            throw new DadosInvalidosException("Medição do sensor " + sensorId + " sem região.");
        }
        if (!tipo.aceita(valor)) {
            throw new DadosInvalidosException(String.format(
                    "Medição de %s inválida no sensor %s: %s (faixa aceita: %s a %s).",
                    tipo.getGrandeza(), sensorId, Double.isFinite(valor) ? tipo.formatar(valor) : String.valueOf(valor),
                    tipo.formatar(tipo.getMinimoValido()), tipo.formatar(tipo.getMaximoValido())));
        }
        if (dataHora == null) {
            throw new DadosInvalidosException("Medição do sensor " + sensorId + " sem data e hora.");
        }
        this.id = id;
        this.sensorId = sensorId;
        this.tipo = tipo;
        this.regiao = regiao;
        this.valor = valor;
        this.dataHora = dataHora;
    }

    public Long getId() { return id; }
    public void definirIdGerado(long idGerado) { this.id = idGerado; }
    public String getSensorId() { return sensorId; }
    public TipoSensor getTipo() { return tipo; }
    public String getRegiao() { return regiao; }
    public double getValor() { return valor; }
    public LocalDateTime getDataHora() { return dataHora; }

    @Override
    public String toString() {
        return String.format("%s | %s | %s", dataHora.format(FORMATO), sensorId, tipo.formatar(valor));
    }
}

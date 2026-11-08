package dominio;

/**
 * Tipos de sensor do recorte "chuva intensa e risco de alagamento",
 * com a unidade e a faixa fisicamente possível de cada medição.
 */
public enum TipoSensor {

    PLUVIOMETRO("Pluviômetro", "chuva", "mm/h", 0.0, 200.0),
    NIVEL_CORREGO("Régua de nível", "nível do córrego", "m", 0.0, 10.0),
    UMIDADE_SOLO("Sensor de umidade do solo", "umidade do solo", "%", 0.0, 100.0);

    private final String descricao;
    private final String grandeza;
    private final String unidade;
    private final double minimoValido;
    private final double maximoValido;

    TipoSensor(String descricao, String grandeza, String unidade, double minimoValido, double maximoValido) {
        this.descricao = descricao;
        this.grandeza = grandeza;
        this.unidade = unidade;
        this.minimoValido = minimoValido;
        this.maximoValido = maximoValido;
    }

    public boolean aceita(double valor) {
        return Double.isFinite(valor) && valor >= minimoValido && valor <= maximoValido;
    }

    public String formatar(double valor) {
        return String.format("%.1f %s", valor, unidade);
    }

    public String getDescricao() { return descricao; }
    public String getGrandeza() { return grandeza; }
    public String getUnidade() { return unidade; }
    public double getMinimoValido() { return minimoValido; }
    public double getMaximoValido() { return maximoValido; }
}

package dominio;

public enum StatusComponente {
    NAO_VERIFICADO("não verificado"),
    OPERANTE("operante"),
    COM_RESTRICAO("operando com restrição"),
    INOPERANTE("inoperante");

    private final String rotulo;

    StatusComponente(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}

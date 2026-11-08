package dominio;

/**
 * Níveis de alerta em ordem crescente de gravidade, com a ação SUGERIDA.
 * O sistema sugere; quem decide é a pessoa responsável (Defesa Civil, liderança comunitária).
 */
public enum NivelAlerta {

    ATENCAO("ATENÇÃO", "Acompanhar a evolução e verificar bueiros e bocas de lobo da região."),
    ALERTA("ALERTA", "Avisar lideranças comunitárias e deixar equipes de apoio de prontidão."),
    CRITICO("CRÍTICO", "Avaliar com a Defesa Civil a retirada preventiva de moradores das áreas de risco.");

    private final String rotulo;
    private final String acaoSugerida;

    NivelAlerta(String rotulo, String acaoSugerida) {
        this.rotulo = rotulo;
        this.acaoSugerida = acaoSugerida;
    }

    public String getRotulo() { return rotulo; }
    public String getAcaoSugerida() { return acaoSugerida; }
}

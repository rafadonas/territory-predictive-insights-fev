package dominio;

import excecao.DadosInvalidosException;

import java.util.Optional;

/**
 * Os três limites de um tipo de sensor. Imutável: para mudar, cria-se um novo objeto,
 * que passa de novo pela validação (atenção < alerta < crítico, todos não negativos).
 */
public final class Limiares {

    private final double atencao;
    private final double alerta;
    private final double critico;

    public Limiares(double atencao, double alerta, double critico) throws DadosInvalidosException {
        if (!Double.isFinite(atencao) || !Double.isFinite(alerta) || !Double.isFinite(critico) || atencao < 0) {
            throw new DadosInvalidosException("Limiar inválido: os valores devem ser números não negativos.");
        }
        if (!(atencao < alerta && alerta < critico)) {
            throw new DadosInvalidosException(String.format(
                    "Limiar inválido: é preciso ATENÇÃO < ALERTA < CRÍTICO (recebido %.1f, %.1f, %.1f).", atencao, alerta, critico));
        }
        this.atencao = atencao;
        this.alerta = alerta;
        this.critico = critico;
    }

    /** Devolve o nível mais grave cujo limite o valor atingiu, ou vazio se ficou abaixo de todos. */
    public Optional<NivelAlerta> classificar(double valor) {
        if (valor >= critico) return Optional.of(NivelAlerta.CRITICO);
        if (valor >= alerta) return Optional.of(NivelAlerta.ALERTA);
        if (valor >= atencao) return Optional.of(NivelAlerta.ATENCAO);
        return Optional.empty();
    }

    public double limiteDe(NivelAlerta nivel) {
        switch (nivel) {
            case CRITICO: return critico;
            case ALERTA: return alerta;
            default: return atencao;
        }
    }

    public double getAtencao() { return atencao; }
    public double getAlerta() { return alerta; }
    public double getCritico() { return critico; }
}

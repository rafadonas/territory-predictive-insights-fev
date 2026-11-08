package dominio;

import excecao.DadosInvalidosException;

/**
 * Reservatório de retenção de águas pluviais (piscinão): segura a água da chuva
 * para o córrego não transbordar. O status depende de quanto da capacidade já foi usada.
 */
public class Piscinao extends ComponenteTerritorial {

    /** Acima disso, sobra pouca margem para uma nova chuva forte. */
    public static final double OCUPACAO_RESTRICAO = 70.0;
    /** Acima disso, o piscinão praticamente não retém mais água. */
    public static final double OCUPACAO_ESGOTADA = 95.0;

    private final double capacidadeM3;
    private double volumeAtualM3;

    public Piscinao(String id, String nome, String regiao, double capacidadeM3) throws DadosInvalidosException {
        super(id, nome, regiao);
        if (!Double.isFinite(capacidadeM3) || capacidadeM3 <= 0) {
            throw new DadosInvalidosException("Capacidade do piscinão deve ser um número finito maior que zero.");
        }
        this.capacidadeM3 = capacidadeM3;
    }

    public void setVolumeAtualM3(double volume) throws DadosInvalidosException {
        if (!Double.isFinite(volume) || volume < 0 || volume > capacidadeM3) {
            throw new DadosInvalidosException(String.format(
                    "Volume inválido para o piscinão %s: %.0f m³ (capacidade: 0 a %.0f m³).", getId(), volume, capacidadeM3));
        }
        this.volumeAtualM3 = volume;
    }

    /**
     * Atualiza o volume a partir da chuva média medida no território:
     * escoa 10% do volume por ciclo e recebe 1% da capacidade a cada mm/h de chuva.
     */
    public void atualizarComChuva(double chuvaMediaMmH) throws DadosInvalidosException {
        double novoVolume = volumeAtualM3 * 0.9 + chuvaMediaMmH * capacidadeM3 * 0.01;
        setVolumeAtualM3(Math.min(capacidadeM3, Math.max(0, novoVolume)));
    }

    public double getOcupacaoPercentual() {
        return volumeAtualM3 / capacidadeM3 * 100.0;
    }

    @Override
    public StatusComponente verificarStatus() {
        double ocupacao = getOcupacaoPercentual();
        StatusComponente novo;
        if (ocupacao >= OCUPACAO_ESGOTADA) {
            novo = StatusComponente.INOPERANTE;
        } else if (ocupacao >= OCUPACAO_RESTRICAO) {
            novo = StatusComponente.COM_RESTRICAO;
        } else {
            novo = StatusComponente.OPERANTE;
        }
        setStatus(novo);
        return novo;
    }

    @Override
    public String detalhar() {
        return String.format("ocupação de %.0f%% (%.0f de %.0f m³)", getOcupacaoPercentual(), volumeAtualM3, capacidadeM3);
    }
}

package dominio;

import excecao.AcessoNegadoException;
import excecao.DadosInvalidosException;

import java.util.EnumMap;
import java.util.Map;

/**
 * Dados do território monitorado. Todos os atributos são privados e só mudam por métodos
 * que validam o valor. Os limiares de alerta são protegidos por código de acesso:
 * mudar um limiar muda quando a população é avisada, então não pode ser feito por qualquer um.
 *
 * O território é descrito por região e equipamento, nunca por pessoas (sem dados pessoais).
 */
public class DadosTerritorio {

    private String nome;
    private final String municipio;
    private int populacaoEstimada;
    private double areaKm2;
    private int pontosVulneraveis;
    private final String codigoAcesso;
    private final Map<TipoSensor, Limiares> limiares = new EnumMap<>(TipoSensor.class);

    public DadosTerritorio(String nome, String municipio, int populacaoEstimada, double areaKm2,
                           int pontosVulneraveis, String codigoAcesso) throws DadosInvalidosException {
        if (municipio == null || municipio.isBlank()) {
            throw new DadosInvalidosException("Informe o município do território.");
        }
        if (codigoAcesso == null || codigoAcesso.length() < 4) {
            throw new DadosInvalidosException("O código de acesso deve ter pelo menos 4 caracteres.");
        }
        this.municipio = municipio;
        this.codigoAcesso = codigoAcesso;
        setNome(nome);
        setPopulacaoEstimada(populacaoEstimada);
        setAreaKm2(areaKm2);
        setPontosVulneraveis(pontosVulneraveis);

        // Valores de referência adotados pela equipe para a simulação (ajustáveis no menu).
        limiares.put(TipoSensor.PLUVIOMETRO, new Limiares(10.0, 30.0, 50.0));
        limiares.put(TipoSensor.NIVEL_CORREGO, new Limiares(2.5, 3.2, 3.8));
        limiares.put(TipoSensor.UMIDADE_SOLO, new Limiares(80.0, 90.0, 95.0));
    }

    public void setNome(String nome) throws DadosInvalidosException {
        if (nome == null || nome.isBlank() || nome.length() > 80) {
            throw new DadosInvalidosException("O nome do território deve ter de 1 a 80 caracteres.");
        }
        this.nome = nome.trim();
    }

    public void setPopulacaoEstimada(int populacaoEstimada) throws DadosInvalidosException {
        if (populacaoEstimada < 0) {
            throw new DadosInvalidosException("População não pode ser negativa (recebido: " + populacaoEstimada + ").");
        }
        this.populacaoEstimada = populacaoEstimada;
    }

    public void setAreaKm2(double areaKm2) throws DadosInvalidosException {
        if (!Double.isFinite(areaKm2) || areaKm2 <= 0) {
            throw new DadosInvalidosException("A área do território deve ser maior que zero km².");
        }
        this.areaKm2 = areaKm2;
    }

    public void setPontosVulneraveis(int pontosVulneraveis) throws DadosInvalidosException {
        if (pontosVulneraveis < 0) {
            throw new DadosInvalidosException("O número de pontos vulneráveis não pode ser negativo.");
        }
        this.pontosVulneraveis = pontosVulneraveis;
    }

    /**
     * Altera os limiares de um tipo de sensor. Exige o código de acesso e que os
     * limites caibam na faixa física do sensor (não adianta alerta de chuva em 500 mm/h).
     */
    public void alterarLimiares(String codigoInformado, TipoSensor tipo, Limiares novos)
            throws AcessoNegadoException, DadosInvalidosException {
        conferirCodigoAcesso(codigoInformado);
        if (tipo == null || novos == null) {
            throw new DadosInvalidosException("Informe o tipo de sensor e os novos limiares.");
        }
        if (novos.getCritico() > tipo.getMaximoValido()) {
            throw new DadosInvalidosException(String.format(
                    "Limiar inválido: o limite CRÍTICO de %s não pode passar de %s.",
                    tipo.getGrandeza(), tipo.formatar(tipo.getMaximoValido())));
        }
        limiares.put(tipo, novos);
    }

    /** Confere o código antes de qualquer alteração protegida (o menu chama antes de pedir os valores). */
    public void conferirCodigoAcesso(String codigoInformado) throws AcessoNegadoException {
        if (!codigoAcesso.equals(codigoInformado)) {
            throw new AcessoNegadoException("Código de acesso incorreto: limiares não alterados.");
        }
    }

    public Limiares getLimiares(TipoSensor tipo) {
        return limiares.get(tipo);
    }

    /** Densidade demográfica: ajuda a dimensionar quantas pessoas um alerta alcança. */
    public double getDensidadeHabKm2() {
        return populacaoEstimada / areaKm2;
    }

    public String getNome() { return nome; }
    public String getMunicipio() { return municipio; }
    public int getPopulacaoEstimada() { return populacaoEstimada; }
    public double getAreaKm2() { return areaKm2; }
    public int getPontosVulneraveis() { return pontosVulneraveis; }
}

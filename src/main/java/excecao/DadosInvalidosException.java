package excecao;

/**
 * Lançada quando um dado do território não pode ser aceito:
 * medição fora da faixa física, população negativa, limiar inválido etc.
 * É checada: quem chama é obrigado a decidir o que fazer com o dado ruim.
 */
public class DadosInvalidosException extends Exception {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}

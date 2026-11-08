package excecao;

/**
 * Lançada quando alguém tenta alterar os limiares de alerta sem o código de acesso correto.
 */
public class AcessoNegadoException extends Exception {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}

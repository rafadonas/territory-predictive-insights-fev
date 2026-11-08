package excecao;

/**
 * Envolve falhas de banco de dados (SQLException) com uma mensagem compreensível.
 * É não checada porque, se o banco cair, nenhuma camada intermediária consegue
 * se recuperar; quem trata é o menu, informando o usuário.
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

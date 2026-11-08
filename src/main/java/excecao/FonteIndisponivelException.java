package excecao;

/**
 * Lançada quando a fonte de dados (sensor ou gerador simulado) não responde.
 * Diferente de DadosInvalidosException: aqui não há dado nenhum para validar.
 */
public class FonteIndisponivelException extends Exception {

    public FonteIndisponivelException(String mensagem) {
        super(mensagem);
    }
}

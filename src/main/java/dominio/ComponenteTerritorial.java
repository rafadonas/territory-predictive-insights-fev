package dominio;

import excecao.DadosInvalidosException;

/**
 * Qualquer equipamento ou estrutura do território que precisa ser acompanhado.
 * Cada tipo concreto sabe avaliar o próprio estado de um jeito diferente (polimorfismo):
 * um ponto de monitoramento olha os sensores; um piscinão olha quanto já encheu.
 */
public abstract class ComponenteTerritorial {

    private final String id;
    private final String nome;
    private final String regiao;
    private StatusComponente status = StatusComponente.NAO_VERIFICADO;

    protected ComponenteTerritorial(String id, String nome, String regiao) throws DadosInvalidosException {
        if (id == null || id.isBlank() || nome == null || nome.isBlank() || regiao == null || regiao.isBlank()) {
            throw new DadosInvalidosException("Componente territorial precisa de id, nome e região preenchidos.");
        }
        this.id = id;
        this.nome = nome;
        this.regiao = regiao;
    }

    /** Avalia o estado atual do componente, atualiza o status e o devolve. */
    public abstract StatusComponente verificarStatus();

    /** Detalhe específico do tipo, exibido no status completo. */
    public abstract String detalhar();

    protected void setStatus(StatusComponente status) {
        this.status = status;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getRegiao() { return regiao; }
    public StatusComponente getStatus() { return status; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s): %s", id, nome, regiao, status.getRotulo());
    }
}

package dominio;

import excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Local físico com um ou mais sensores instalados (ex.: uma ponte sobre o córrego).
 * Status: todos os sensores respondendo = operante; parte deles = com restrição;
 * nenhum = inoperante.
 */
public class PontoMonitoramento extends ComponenteTerritorial {

    private final List<Sensor> sensores = new ArrayList<>();

    public PontoMonitoramento(String id, String nome, String regiao) throws DadosInvalidosException {
        super(id, nome, regiao);
    }

    public void instalar(Sensor sensor) throws DadosInvalidosException {
        if (sensor == null) {
            throw new DadosInvalidosException("Não é possível instalar um sensor nulo no ponto " + getId() + ".");
        }
        sensores.add(sensor);
    }

    public List<Sensor> getSensores() {
        return Collections.unmodifiableList(sensores);
    }

    @Override
    public StatusComponente verificarStatus() {
        long funcionando = sensores.stream().filter(Sensor::estaFuncionando).count();
        StatusComponente novo;
        if (sensores.isEmpty() || funcionando == 0) {
            novo = StatusComponente.INOPERANTE;
        } else if (funcionando < sensores.size()) {
            novo = StatusComponente.COM_RESTRICAO;
        } else {
            novo = StatusComponente.OPERANTE;
        }
        setStatus(novo);
        return novo;
    }

    @Override
    public String detalhar() {
        long funcionando = sensores.stream().filter(Sensor::estaFuncionando).count();
        return funcionando + " de " + sensores.size() + " sensores respondendo";
    }
}

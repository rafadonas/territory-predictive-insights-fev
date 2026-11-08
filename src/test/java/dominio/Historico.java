package dominio;

import excecao.DadosInvalidosException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Ajuda os testes a montar um histórico de medições em ordem cronológica. */
public final class Historico {

    private Historico() {
    }

    public static List<Medicao> de(String sensorId, TipoSensor tipo, double... valores) throws DadosInvalidosException {
        List<Medicao> lista = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 7, 8, 0);
        for (int i = 0; i < valores.length; i++) {
            lista.add(new Medicao((long) i + 1, sensorId, tipo, "Baixada do Córrego", valores[i], inicio.plusMinutes(10L * i)));
        }
        return lista;
    }
}

package servico;

import dominio.ComponenteTerritorial;
import dominio.Piscinao;
import dominio.Pluviometro;
import dominio.PontoMonitoramento;
import dominio.ReguaNivelCorrego;
import dominio.SensorUmidadeSolo;
import excecao.DadosInvalidosException;

import java.util.List;
import java.util.Random;

/**
 * Monta os componentes do território-piloto FICTÍCIO usado na simulação:
 * dois pontos de monitoramento com sensores e um piscinão.
 * Nomes de lugares são inventados; não há dados de pessoas.
 */
public final class CenarioPiloto {

    private final PontoMonitoramento ponteCorrego;
    private final PontoMonitoramento encostaNorte;
    private final Piscinao piscinao;

    public CenarioPiloto(Random random) throws DadosInvalidosException {
        ponteCorrego = new PontoMonitoramento("PM-01", "Ponte do Córrego das Garças", "Baixada do Córrego");
        ponteCorrego.instalar(new Pluviometro("PLU-01", random));
        ponteCorrego.instalar(new ReguaNivelCorrego("NIV-01", random));

        encostaNorte = new PontoMonitoramento("PM-02", "Escola da Encosta Norte", "Encosta Norte");
        encostaNorte.instalar(new Pluviometro("PLU-02", random));
        encostaNorte.instalar(new SensorUmidadeSolo("UMI-01", random));

        piscinao = new Piscinao("PSC-01", "Piscinão Jardim das Águas", "Baixada do Córrego", 50_000);
    }

    public List<PontoMonitoramento> getPontos() {
        return List.of(ponteCorrego, encostaNorte);
    }

    public Piscinao getPiscinao() {
        return piscinao;
    }

    public List<ComponenteTerritorial> getComponentes() {
        return List.of(ponteCorrego, encostaNorte, piscinao);
    }
}

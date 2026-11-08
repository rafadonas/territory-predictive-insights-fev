package servico;

import dominio.Alerta;
import dominio.ComponenteTerritorial;
import dominio.DadosTerritorio;
import dominio.Limiares;
import dominio.TipoSensor;

import java.util.List;

/**
 * BUILDER: monta o relatório de "status completo" passo a passo.
 *
 * Por que Builder aqui: o relatório tem várias partes opcionais (território, componentes,
 * contagens, limiares, alertas) e a ordem e a presença delas podem variar. Em vez de um
 * construtor com muitos parâmetros ou de um método gigante, o menu encadeia só as partes
 * que quer e chama construir() no fim.
 */
public class RelatorioBuilder {

    private static final String LINHA = "-".repeat(72);
    private final StringBuilder texto = new StringBuilder();

    public RelatorioBuilder titulo(String titulo) {
        texto.append(LINHA).append('\n').append(titulo.toUpperCase()).append('\n').append(LINHA).append('\n');
        return this;
    }

    public RelatorioBuilder territorio(DadosTerritorio t) {
        secao("Território");
        texto.append(String.format("%s (%s)%n", t.getNome(), t.getMunicipio()));
        texto.append(String.format("População estimada: %,d hab. | Área: %.1f km² | Densidade: %,.0f hab/km² | Pontos vulneráveis: %d%n",
                t.getPopulacaoEstimada(), t.getAreaKm2(), t.getDensidadeHabKm2(), t.getPontosVulneraveis()));
        return this;
    }

    public RelatorioBuilder componentes(List<ComponenteTerritorial> componentes) {
        secao("Componentes territoriais");
        for (ComponenteTerritorial c : componentes) {
            c.verificarStatus();
            texto.append(String.format("  %s - %s%n", c, c.detalhar()));
        }
        return this;
    }

    public RelatorioBuilder contagens(int medicoes, int alertas) {
        secao("Banco de dados");
        texto.append(String.format("  Medições gravadas: %d | Alertas emitidos: %d%n", medicoes, alertas));
        return this;
    }

    public RelatorioBuilder limiares(DadosTerritorio t) {
        secao("Limiares de alerta (ATENÇÃO / ALERTA / CRÍTICO)");
        for (TipoSensor tipo : TipoSensor.values()) {
            Limiares l = t.getLimiares(tipo);
            texto.append(String.format("  %-26s %s / %s / %s%n", tipo.getDescricao(),
                    tipo.formatar(l.getAtencao()), tipo.formatar(l.getAlerta()), tipo.formatar(l.getCritico())));
        }
        return this;
    }

    public RelatorioBuilder ultimosAlertas(List<Alerta> alertas) {
        secao("Últimos alertas");
        if (alertas.isEmpty()) {
            texto.append("  Nenhum alerta emitido até agora.\n");
        }
        for (Alerta a : alertas) {
            texto.append("  ").append(a).append('\n');
        }
        return this;
    }

    public RelatorioBuilder observacao(String observacao) {
        texto.append('\n').append(observacao).append('\n');
        return this;
    }

    public String construir() {
        return texto.toString();
    }

    private void secao(String nome) {
        texto.append('\n').append(">> ").append(nome).append('\n');
    }
}

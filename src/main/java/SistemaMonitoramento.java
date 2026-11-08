import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.EstrategiaPrevisao;
import dominio.Limiares;
import dominio.Medicao;
import dominio.PontoMonitoramento;
import dominio.ResultadoPrevisao;
import dominio.Sensor;
import dominio.TipoSensor;
import excecao.AcessoNegadoException;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import excecao.PersistenciaException;
import persistencia.AlertaDAO;
import persistencia.ConexaoBanco;
import persistencia.MedicaoDAO;
import servico.CenarioPiloto;
import servico.ConfiguracaoSistema;
import servico.ExportadorCsv;
import servico.FabricaPrevisao;
import servico.RelatorioBuilder;
import servico.ResultadoAnalise;
import servico.ServicoMonitoramento;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Menu principal (console) do VigiaChuva: Territory Predictive Insights.
 * Aqui as exceções do sistema são capturadas e viram mensagens claras para o usuário.
 */
public class SistemaMonitoramento {

    private final Scanner entrada = new Scanner(System.in);
    private final ConfiguracaoSistema configuracao = ConfiguracaoSistema.getInstancia();
    private final ServicoMonitoramento servico;
    private final CenarioPiloto cenario;
    private final String urlBanco;

    public SistemaMonitoramento(ServicoMonitoramento servico, CenarioPiloto cenario, String urlBanco) {
        this.servico = servico;
        this.cenario = cenario;
        this.urlBanco = urlBanco;
    }

    public static void main(String[] args) {
        try {
            ConexaoBanco banco = ConexaoBanco.padrao();
            banco.inicializarEsquema();
            ServicoMonitoramento servico = new ServicoMonitoramento(
                    new MedicaoDAO(banco), new AlertaDAO(banco), FabricaPrevisao.criarEstrategiasAtivas());
            new SistemaMonitoramento(servico, new CenarioPiloto(new Random()), banco.getUrl()).executar();
        } catch (PersistenciaException e) {
            System.out.println("Não foi possível iniciar o banco de dados: " + e.getMessage());
            System.out.println("Verifique se outra janela do programa já está aberta (o H2 em arquivo aceita um processo por vez).");
        } catch (DadosInvalidosException e) {
            System.out.println("Configuração inicial inválida: " + e.getMessage());
        }
    }

    void executar() {
        System.out.println("=".repeat(72));
        System.out.println(" VigiaChuva | Territory Predictive Insights (chuva intensa e alagamento)");
        System.out.println(" Território: " + configuracao.getTerritorio().getNome());
        System.out.println(" Banco: " + urlBanco + " | medições já gravadas: " + servico.totalMedicoes());
        System.out.println("=".repeat(72));

        int opcao;
        do {
            System.out.println();
            System.out.println("1) Verificar sensores (ler e gravar medições)");
            System.out.println("2) Rodar previsões e alertas");
            System.out.println("3) Ver histórico (medições por sensor, alertas, exportar CSV)");
            System.out.println("4) Gerenciar dados do território");
            System.out.println("5) Status completo");
            System.out.println("0) Sair");
            opcao = lerInteiro("Escolha uma opção: ", 0, 5);
            try {
                switch (opcao) {
                    case 1: verificarSensores(); break;
                    case 2: rodarPrevisoes(); break;
                    case 3: menuHistorico(); break;
                    case 4: menuTerritorio(); break;
                    case 5: statusCompleto(); break;
                    default: System.out.println("Até logo! O histórico ficou salvo no banco.");
                }
            } catch (PersistenciaException e) {
                System.out.println("[ERRO NO BANCO] " + e.getMessage());
                System.out.println("A operação não foi concluída. Tente novamente; se persistir, reinicie o programa.");
            }
        } while (opcao != 0);
    }

    // ---------------------------------------------------------------- 1) Sensores

    private void verificarSensores() {
        int ciclos = lerInteiro("Quantos ciclos de leitura? (1 a 24; cada ciclo lê todos os sensores): ", 1, 24);
        int gravadas = 0;
        int falhas = 0;
        int descartadas = 0;

        for (int ciclo = 1; ciclo <= ciclos; ciclo++) {
            List<Double> chuvasDoCiclo = new ArrayList<>();
            for (PontoMonitoramento ponto : cenario.getPontos()) {
                for (Sensor sensor : ponto.getSensores()) {
                    try {
                        Medicao m = servico.registrarLeitura(sensor, ponto.getRegiao());
                        gravadas++;
                        if (sensor.getTipo() == TipoSensor.PLUVIOMETRO) {
                            chuvasDoCiclo.add(m.getValor());
                        }
                        if (ciclos == 1) {
                            System.out.println("  [OK]         " + m);
                        }
                    } catch (FonteIndisponivelException e) {
                        falhas++;
                        System.out.println("  [FALHA]      Ciclo " + ciclo + ": " + e.getMessage());
                    } catch (DadosInvalidosException e) {
                        descartadas++;
                        System.out.println("  [DESCARTADA] Ciclo " + ciclo + ": " + e.getMessage());
                    }
                }
            }
            atualizarPiscinao(chuvasDoCiclo);
        }

        System.out.printf("%nResumo: %d medições gravadas, %d falhas de comunicação, %d leituras descartadas por valor inválido.%n",
                gravadas, falhas, descartadas);
        System.out.println("Status dos componentes:");
        cenario.getComponentes().forEach(c -> {
            c.verificarStatus();
            System.out.println("  " + c + " - " + c.detalhar());
        });
    }

    private void atualizarPiscinao(List<Double> chuvas) {
        double media = chuvas.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        try {
            cenario.getPiscinao().atualizarComChuva(media);
        } catch (DadosInvalidosException e) {
            System.out.println("  [AVISO] Volume do piscinão não atualizado: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 2) Previsões

    private void rodarPrevisoes() {
        System.out.println("Estratégias ativas:");
        for (EstrategiaPrevisao e : servico.getEstrategias()) {
            System.out.println("  - " + e.getNome() + " (mínimo de " + e.getMinimoLeituras() + " leituras)");
        }
        int alertas = 0;
        for (PontoMonitoramento ponto : cenario.getPontos()) {
            for (Sensor sensor : ponto.getSensores()) {
                ResultadoAnalise r = servico.analisarSensor(sensor, ponto.getRegiao());
                System.out.printf("%n%s (%s) - %s%n", sensor.getId(), sensor.getTipo().getGrandeza(), ponto.getRegiao());
                for (ResultadoPrevisao p : r.getPrevisoes()) {
                    String valor = p.isConfiavel() ? sensor.getTipo().formatar(p.getValorPrevisto()) : "sem previsão";
                    System.out.printf("  * %s: %s%n    %s%n", p.getEstrategia(), valor, p.getExplicacao());
                }
                if (r.getAlerta().isPresent()) {
                    alertas++;
                    Alerta a = r.getAlerta().get();
                    System.out.println("  >>> [" + a.getNivel().getRotulo() + "] " + a.getJustificativa());
                } else {
                    System.out.println("  Nenhum limiar atingido.");
                }
            }
        }
        System.out.printf("%n%d alerta(s) emitido(s) e gravado(s) no banco.%n", alertas);
        System.out.println("Lembrete: os alertas apoiam a decisão; quem decide é a equipe responsável.");
    }

    // ---------------------------------------------------------------- 3) Histórico

    private void menuHistorico() {
        System.out.println("1) Medições por sensor   2) Alertas emitidos   0) Voltar");
        int opcao = lerInteiro("Opção: ", 0, 2);
        if (opcao == 1) {
            historicoPorSensor();
        } else if (opcao == 2) {
            List<Alerta> alertas = servico.alertasEmitidos(20);
            if (alertas.isEmpty()) {
                System.out.println("Nenhum alerta emitido ainda. Leia os sensores (opção 1) e rode as previsões (opção 2).");
            }
            for (Alerta a : alertas) {
                System.out.println(a);
                System.out.println("    Por quê: " + a.getJustificativa());
            }
        }
    }

    private void historicoPorSensor() {
        List<Sensor> sensores = new ArrayList<>();
        cenario.getPontos().forEach(p -> sensores.addAll(p.getSensores()));
        for (int i = 0; i < sensores.size(); i++) {
            Sensor s = sensores.get(i);
            System.out.printf("%d) %s (%d medições no banco)%n", i + 1, s, servico.totalMedicoesDoSensor(s.getId()));
        }
        Sensor escolhido = sensores.get(lerInteiro("Sensor: ", 1, sensores.size()) - 1);

        List<Medicao> historico = servico.historicoPorSensor(escolhido.getId());
        if (historico.isEmpty()) {
            System.out.println("Ainda não há medições deste sensor. Use a opção 1 do menu principal.");
            return;
        }
        System.out.println("Últimas " + historico.size() + " medições (mais antiga primeiro):");
        historico.forEach(m -> System.out.println("  " + m));

        if (lerTexto("Exportar este histórico para CSV? (s/n): ").equalsIgnoreCase("s")) {
            try {
                Path arquivo = ExportadorCsv.exportarHistorico(escolhido.getId(), historico, Path.of("exportacoes"));
                System.out.println("Arquivo gerado: " + arquivo.toAbsolutePath());
            } catch (IOException e) {
                System.out.println("Não foi possível gravar o CSV: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- 4) Território

    private void menuTerritorio() {
        DadosTerritorio t = configuracao.getTerritorio();
        System.out.println(new RelatorioBuilder().territorio(t).limiares(t).construir());
        System.out.println("1) Alterar população   2) Alterar área   3) Alterar pontos vulneráveis   4) Alterar limiares   0) Voltar");
        int opcao = lerInteiro("Opção: ", 0, 4);
        try {
            switch (opcao) {
                case 1: t.setPopulacaoEstimada(lerInteiro("Nova população estimada: ", Integer.MIN_VALUE, Integer.MAX_VALUE)); break;
                case 2: t.setAreaKm2(lerDecimal("Nova área (km²): ")); break;
                case 3: t.setPontosVulneraveis(lerInteiro("Novo número de pontos vulneráveis: ", Integer.MIN_VALUE, Integer.MAX_VALUE)); break;
                case 4: alterarLimiares(t); break;
                default: return;
            }
            System.out.println("Dados atualizados.");
        } catch (DadosInvalidosException e) {
            System.out.println("[DADO INVÁLIDO] " + e.getMessage() + " Nada foi alterado.");
        } catch (AcessoNegadoException e) {
            System.out.println("[ACESSO NEGADO] " + e.getMessage());
        }
    }

    private void alterarLimiares(DadosTerritorio t) throws DadosInvalidosException, AcessoNegadoException {
        String codigo = lerTexto("Código de acesso: ");
        t.conferirCodigoAcesso(codigo);
        TipoSensor[] tipos = TipoSensor.values();
        for (int i = 0; i < tipos.length; i++) {
            System.out.printf("%d) %s (%s)%n", i + 1, tipos[i].getDescricao(), tipos[i].getUnidade());
        }
        TipoSensor tipo = tipos[lerInteiro("Tipo de sensor: ", 1, tipos.length) - 1];
        double atencao = lerDecimal("Limite de ATENÇÃO: ");
        double alerta = lerDecimal("Limite de ALERTA: ");
        double critico = lerDecimal("Limite CRÍTICO: ");
        t.alterarLimiares(codigo, tipo, new Limiares(atencao, alerta, critico));
    }

    // ---------------------------------------------------------------- 5) Status

    private void statusCompleto() {
        DadosTerritorio t = configuracao.getTerritorio();
        System.out.println(new RelatorioBuilder()
                .titulo("Status completo - VigiaChuva")
                .territorio(t)
                .componentes(cenario.getComponentes())
                .contagens(servico.totalMedicoes(), servico.totalAlertas())
                .limiares(t)
                .ultimosAlertas(servico.alertasEmitidos(5))
                .observacao("Dados SIMULADOS de um território fictício. Os alertas apoiam, mas não substituem, a decisão humana.")
                .construir());
    }

    // ---------------------------------------------------------------- Entrada

    private int lerInteiro(String mensagem, int minimo, int maximo) {
        while (true) {
            String texto = lerTexto(mensagem);
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.printf("Digite um número entre %d e %d.%n", minimo, maximo);
            } catch (NumberFormatException e) {
                System.out.println("'" + texto + "' não é um número inteiro. Tente de novo.");
            }
        }
    }

    private double lerDecimal(String mensagem) {
        while (true) {
            String texto = lerTexto(mensagem).replace(',', '.');
            try {
                return Double.parseDouble(texto);
            } catch (NumberFormatException e) {
                System.out.println("'" + texto + "' não é um número. Use, por exemplo, 12,5.");
            }
        }
    }

    private String lerTexto(String mensagem) {
        System.out.print(mensagem);
        if (!entrada.hasNextLine()) {
            System.out.println();
            System.out.println("Entrada encerrada. Saindo.");
            System.exit(0);
        }
        return entrada.nextLine().trim();
    }
}

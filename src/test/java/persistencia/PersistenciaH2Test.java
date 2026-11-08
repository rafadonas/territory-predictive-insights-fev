package persistencia;

import dominio.Alerta;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import servico.ExportadorCsv;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração do JDBC com H2. Cada teste usa um banco próprio.
 */
class PersistenciaH2Test {

    private ConexaoBanco banco;
    private MedicaoDAO medicaoDAO;
    private AlertaDAO alertaDAO;

    @BeforeEach
    void preparar() {
        banco = new ConexaoBanco("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        banco.inicializarEsquema();
        medicaoDAO = new MedicaoDAO(banco);
        alertaDAO = new AlertaDAO(banco);
    }

    @Test
    @DisplayName("Grava medições e devolve o histórico do sensor em ordem cronológica")
    void gravaEListaPorSensor() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 11, 7, 10, 0);
        for (int i = 0; i < 4; i++) {
            medicaoDAO.salvar(new Medicao("NIV-01", TipoSensor.NIVEL_CORREGO, "Baixada", 2.0 + i, base.plusMinutes(i)));
        }
        medicaoDAO.salvar(new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, "Baixada", 15.0, base));

        List<Medicao> ultimas3 = medicaoDAO.buscarHistoricoPorSensor("NIV-01", 3);

        assertEquals(List.of(3.0, 4.0, 5.0), ultimas3.stream().map(Medicao::getValor).toList());
        assertEquals(4, medicaoDAO.contarPorSensor("NIV-01"));
        assertEquals(5, medicaoDAO.contarTodas());
        assertNotNull(ultimas3.get(0).getId());
    }

    @Test
    @DisplayName("Dados sobrevivem ao 'reinício': um novo DAO no mesmo banco enxerga o que foi gravado")
    void dadosSobrevivemAoReinicio(@TempDir Path pasta) throws Exception {
        String url = "jdbc:h2:" + pasta.resolve("vigiachuva").toAbsolutePath();
        ConexaoBanco primeiraExecucao = new ConexaoBanco(url);
        primeiraExecucao.inicializarEsquema();
        new MedicaoDAO(primeiraExecucao).salvar(
                new Medicao("UMI-01", TipoSensor.UMIDADE_SOLO, "Encosta", 88.0, LocalDateTime.now()));

        ConexaoBanco segundaExecucao = new ConexaoBanco(url);
        segundaExecucao.inicializarEsquema(); // idempotente: não apaga nada

        assertEquals(1, new MedicaoDAO(segundaExecucao).contarPorSensor("UMI-01"));
    }

    @Test
    @DisplayName("Grava alertas e lista os mais recentes primeiro")
    void gravaEListaAlertas() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 7, 10, 0);
        alertaDAO.salvar(new Alerta(null, "PLU-01", TipoSensor.PLUVIOMETRO, "Baixada", NivelAlerta.ATENCAO, 12, 10, "Média móvel", "teste 1", base));
        alertaDAO.salvar(new Alerta(null, "NIV-01", TipoSensor.NIVEL_CORREGO, "Baixada", NivelAlerta.CRITICO, 3.9, 3.8, "Comparação", "teste 2", base.plusHours(1)));

        List<Alerta> alertas = alertaDAO.listarRecentes(10);

        assertEquals(2, alertaDAO.contarTodos());
        assertEquals(NivelAlerta.CRITICO, alertas.get(0).getNivel());
        assertEquals("teste 2", alertas.get(0).getJustificativa());
    }

    @Test
    @DisplayName("Bônus: exporta o histórico em CSV com cabeçalho e vírgula decimal")
    void exportaCsv(@TempDir Path pasta) throws Exception {
        medicaoDAO.salvar(new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, "Baixada", 12.5, LocalDateTime.of(2026, 11, 7, 10, 0)));

        Path arquivo = ExportadorCsv.exportarHistorico("PLU-01", medicaoDAO.buscarHistoricoPorSensor("PLU-01", 10), pasta);

        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        assertEquals(2, linhas.size());
        assertTrue(linhas.get(0).endsWith("id;sensor;tipo;regiao;valor;unidade;data_hora"));
        assertTrue(linhas.get(1).contains(";12,5;mm/h;07/11/2026 10:00:00"), linhas.get(1));
    }
}

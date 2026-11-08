package servico;

import dominio.Alerta;
import dominio.Historico;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import dominio.Sensor;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistencia.AlertaDAO;
import persistencia.MedicaoDAO;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa a lógica de alerta SEM banco: os DAOs são mocks do Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ServicoMonitoramentoTest {

    @Mock
    private MedicaoDAO medicaoDAO;
    @Mock
    private AlertaDAO alertaDAO;

    private ServicoMonitoramento servico;
    private final Sensor pluviometro = sensorQueLe(TipoSensor.PLUVIOMETRO, "PLU-01", 12.0);

    @BeforeEach
    void preparar() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes(); // chuva: 10 / 30 / 50 mm/h
        Clock relogio = Clock.fixed(Instant.parse("2026-11-07T18:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        servico = new ServicoMonitoramento(medicaoDAO, alertaDAO,
                List.of(new PrevisaoMediaMovel(5), new PrevisaoComparacao(3)),
                ConfiguracaoSistema.getInstancia(), relogio);
    }

    @Test
    @DisplayName("Previsão acima do limite CRÍTICO gera um alerta CRÍTICO e o grava no banco")
    void geraAlertaCritico() throws DadosInvalidosException {
        // Arrange: chuva subindo. Média móvel = 48 mm/h (ALERTA); comparação projeta 83,3 mm/h (CRÍTICO, limite 50)
        List<Medicao> historico = Historico.de("PLU-01", TipoSensor.PLUVIOMETRO, 20, 30, 40, 50, 60, 60);
        when(medicaoDAO.buscarHistoricoPorSensor(eq("PLU-01"), anyInt())).thenReturn(historico);
        when(alertaDAO.salvar(any(Alerta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        // Act
        ResultadoAnalise resultado = servico.analisarSensor(pluviometro, "Baixada do Córrego");

        // Assert
        ArgumentCaptor<Alerta> alertaGravado = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaDAO).salvar(alertaGravado.capture());
        Alerta alerta = alertaGravado.getValue();
        assertEquals(NivelAlerta.CRITICO, alerta.getNivel());
        assertEquals(50.0, alerta.getLimite(), 1e-9);
        assertTrue(alerta.getJustificativa().contains("PLU-01"));
        assertTrue(alerta.getJustificativa().contains("decisão final é da equipe responsável"));
        assertTrue(resultado.getAlerta().isPresent());
    }

    @Test
    @DisplayName("Com menos de 5 leituras não há previsão nem alerta (o sistema se cala)")
    void historicoInsuficienteNaoGeraAlerta() throws DadosInvalidosException {
        // Arrange: 4 leituras altíssimas, mas poucas para confiar
        when(medicaoDAO.buscarHistoricoPorSensor(eq("PLU-01"), anyInt()))
                .thenReturn(Historico.de("PLU-01", TipoSensor.PLUVIOMETRO, 90, 95, 99, 120));

        // Act
        ResultadoAnalise resultado = servico.analisarSensor(pluviometro, "Baixada do Córrego");

        // Assert
        assertFalse(resultado.getAlerta().isPresent());
        assertTrue(resultado.getPrevisoes().stream().noneMatch(p -> p.isConfiavel()));
        verify(alertaDAO, never()).salvar(any());
    }

    @Test
    @DisplayName("Previsões abaixo de todos os limiares não geram alerta")
    void semAlertaAbaixoDosLimiares() throws DadosInvalidosException {
        when(medicaoDAO.buscarHistoricoPorSensor(eq("PLU-01"), anyInt()))
                .thenReturn(Historico.de("PLU-01", TipoSensor.PLUVIOMETRO, 1, 2, 1, 2, 1, 2));

        ResultadoAnalise resultado = servico.analisarSensor(pluviometro, "Baixada do Córrego");

        assertFalse(resultado.getAlerta().isPresent());
        verify(alertaDAO, never()).salvar(any());
    }

    @Test
    @DisplayName("Leitura válida é gravada; sensor sem resposta propaga a exceção e nada é gravado")
    void registrarLeitura() throws Exception {
        // Leitura válida
        when(medicaoDAO.salvar(any(Medicao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        Medicao medicao = servico.registrarLeitura(pluviometro, "Baixada do Córrego");
        assertEquals(12.0, medicao.getValor());
        verify(medicaoDAO).salvar(any(Medicao.class));

        // Sensor em falha
        Sensor quebrado = mock(Sensor.class);
        when(quebrado.lerValor()).thenThrow(new FonteIndisponivelException("Sensor PLU-09 não respondeu à leitura."));
        assertThrows(FonteIndisponivelException.class, () -> servico.registrarLeitura(quebrado, "Baixada"));
    }

    @Test
    @DisplayName("Leitura impossível (chuva negativa) é descartada com DadosInvalidosException e não vai ao banco")
    void leituraInvalidaNaoEhGravada() {
        Sensor defeituoso = sensorQueLe(TipoSensor.PLUVIOMETRO, "PLU-02", -1.0);

        assertThrows(DadosInvalidosException.class, () -> servico.registrarLeitura(defeituoso, "Encosta Norte"));
        verify(medicaoDAO, never()).salvar(any());
    }

    private static Sensor sensorQueLe(TipoSensor tipo, String id, double valor) {
        return new Sensor() {
            public String getId() { return id; }
            public TipoSensor getTipo() { return tipo; }
            public double lerValor() { return valor; }
            public boolean estaFuncionando() { return true; }
        };
    }
}

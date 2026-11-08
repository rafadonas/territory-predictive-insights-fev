package persistencia;

import dominio.Medicao;
import dominio.TipoSensor;
import excecao.DadosInvalidosException;
import excecao.PersistenciaException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Acesso à tabela "medicao". Todo SQL usa PreparedStatement com parâmetros (?),
 * nunca concatenação de String, e todo recurso JDBC é fechado por try-with-resources.
 */
public class MedicaoDAO {

    private static final String INSERIR =
            "INSERT INTO medicao (sensor_id, tipo, regiao, valor, data_hora) VALUES (?, ?, ?, ?, ?)";
    private static final String HISTORICO_POR_SENSOR =
            "SELECT id, sensor_id, tipo, regiao, valor, data_hora FROM medicao WHERE sensor_id = ? "
                    + "ORDER BY data_hora DESC, id DESC LIMIT ?";
    private static final String CONTAR_POR_SENSOR = "SELECT COUNT(*) FROM medicao WHERE sensor_id = ?";
    private static final String CONTAR_TODAS = "SELECT COUNT(*) FROM medicao";

    private final ConexaoBanco banco;

    public MedicaoDAO(ConexaoBanco banco) {
        this.banco = banco;
    }

    /** Grava a medição e devolve a mesma instância com o id gerado pelo banco. */
    public Medicao salvar(Medicao medicao) {
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            comando.setString(1, medicao.getSensorId());
            comando.setString(2, medicao.getTipo().name());
            comando.setString(3, medicao.getRegiao());
            comando.setDouble(4, medicao.getValor());
            comando.setTimestamp(5, Timestamp.valueOf(medicao.getDataHora()));
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (chaves.next()) {
                    medicao.definirIdGerado(chaves.getLong(1));
                }
            }
            return medicao;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao gravar a medição do sensor " + medicao.getSensorId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Últimas {@code limite} medições do sensor, em ordem cronológica
     * (da mais antiga para a mais recente), que é a ordem que as previsões esperam.
     */
    public List<Medicao> buscarHistoricoPorSensor(String sensorId, int limite) {
        List<Medicao> medicoes = new ArrayList<>();
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(HISTORICO_POR_SENSOR)) {
            comando.setString(1, sensorId);
            comando.setInt(2, limite);
            try (ResultSet linhas = comando.executeQuery()) {
                while (linhas.next()) {
                    medicoes.add(mapear(linhas));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao consultar o histórico do sensor " + sensorId + ": " + e.getMessage(), e);
        }
        Collections.reverse(medicoes);
        return medicoes;
    }

    public int contarPorSensor(String sensorId) {
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(CONTAR_POR_SENSOR)) {
            comando.setString(1, sensorId);
            try (ResultSet linhas = comando.executeQuery()) {
                return linhas.next() ? linhas.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao contar medições do sensor " + sensorId + ": " + e.getMessage(), e);
        }
    }

    public int contarTodas() {
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(CONTAR_TODAS);
             ResultSet linhas = comando.executeQuery()) {
            return linhas.next() ? linhas.getInt(1) : 0;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao contar medições: " + e.getMessage(), e);
        }
    }

    private Medicao mapear(ResultSet linha) throws SQLException {
        try {
            return new Medicao(
                    linha.getLong("id"),
                    linha.getString("sensor_id"),
                    TipoSensor.valueOf(linha.getString("tipo")),
                    linha.getString("regiao"),
                    linha.getDouble("valor"),
                    linha.getTimestamp("data_hora").toLocalDateTime());
        } catch (DadosInvalidosException | IllegalArgumentException e) {
            // Só acontece se alguém editar o banco à mão com valores impossíveis.
            throw new PersistenciaException("Registro corrompido na tabela medicao (id " + linha.getLong("id") + "): " + e.getMessage(), e);
        }
    }
}

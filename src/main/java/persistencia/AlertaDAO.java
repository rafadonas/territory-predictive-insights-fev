package persistencia;

import dominio.Alerta;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import excecao.PersistenciaException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso à tabela "alerta" (PreparedStatement + try-with-resources).
 */
public class AlertaDAO {

    private static final String INSERIR =
            "INSERT INTO alerta (sensor_id, tipo, regiao, nivel, valor_previsto, limite, estrategia, justificativa, data_hora) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String LISTAR_RECENTES =
            "SELECT id, sensor_id, tipo, regiao, nivel, valor_previsto, limite, estrategia, justificativa, data_hora "
                    + "FROM alerta ORDER BY data_hora DESC, id DESC LIMIT ?";
    private static final String CONTAR_TODOS = "SELECT COUNT(*) FROM alerta";

    private final ConexaoBanco banco;

    public AlertaDAO(ConexaoBanco banco) {
        this.banco = banco;
    }

    public Alerta salvar(Alerta alerta) {
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            comando.setString(1, alerta.getSensorId());
            comando.setString(2, alerta.getTipo().name());
            comando.setString(3, alerta.getRegiao());
            comando.setString(4, alerta.getNivel().name());
            comando.setDouble(5, alerta.getValorPrevisto());
            comando.setDouble(6, alerta.getLimite());
            comando.setString(7, alerta.getEstrategia());
            comando.setString(8, alerta.getJustificativa());
            comando.setTimestamp(9, Timestamp.valueOf(alerta.getDataHora()));
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (chaves.next()) {
                    alerta.definirIdGerado(chaves.getLong(1));
                }
            }
            return alerta;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao gravar alerta do sensor " + alerta.getSensorId() + ": " + e.getMessage(), e);
        }
    }

    /** Alertas mais recentes primeiro. */
    public List<Alerta> listarRecentes(int limite) {
        List<Alerta> alertas = new ArrayList<>();
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(LISTAR_RECENTES)) {
            comando.setInt(1, limite);
            try (ResultSet linhas = comando.executeQuery()) {
                while (linhas.next()) {
                    alertas.add(new Alerta(
                            linhas.getLong("id"),
                            linhas.getString("sensor_id"),
                            TipoSensor.valueOf(linhas.getString("tipo")),
                            linhas.getString("regiao"),
                            NivelAlerta.valueOf(linhas.getString("nivel")),
                            linhas.getDouble("valor_previsto"),
                            linhas.getDouble("limite"),
                            linhas.getString("estrategia"),
                            linhas.getString("justificativa"),
                            linhas.getTimestamp("data_hora").toLocalDateTime()));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new PersistenciaException("Falha ao listar alertas: " + e.getMessage(), e);
        }
        return alertas;
    }

    public int contarTodos() {
        try (Connection conexao = banco.abrir();
             PreparedStatement comando = conexao.prepareStatement(CONTAR_TODOS);
             ResultSet linhas = comando.executeQuery()) {
            return linhas.next() ? linhas.getInt(1) : 0;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao contar alertas: " + e.getMessage(), e);
        }
    }
}

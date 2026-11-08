package persistencia;

import excecao.PersistenciaException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fornece conexões JDBC e cria as tabelas na primeira execução.
 *
 * Padrão: H2 em ARQUIVO (./dados/vigiachuva.mv.db), para o histórico sobreviver ao reinício.
 * A URL pode ser trocada com -Dvigiachuva.db.url=... (os testes usam H2 em memória).
 */
public class ConexaoBanco {

    public static final String URL_PADRAO = "jdbc:h2:./dados/vigiachuva";

    private final String url;

    public ConexaoBanco(String url) {
        this.url = url;
    }

    public static ConexaoBanco padrao() {
        return new ConexaoBanco(System.getProperty("vigiachuva.db.url", URL_PADRAO));
    }

    public Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    /** Executa o schema.sql (CREATE TABLE IF NOT EXISTS): seguro rodar a cada inicialização. */
    public void inicializarEsquema() {
        String script;
        try (InputStream entrada = ConexaoBanco.class.getResourceAsStream("/schema.sql")) {
            if (entrada == null) {
                throw new PersistenciaException("Arquivo schema.sql não encontrado no classpath.", null);
            }
            script = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível ler o schema.sql.", e);
        }

        try (Connection conexao = abrir(); Statement comando = conexao.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    comando.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível criar as tabelas no banco (" + url + "): " + e.getMessage(), e);
        }
    }

    public String getUrl() {
        return url;
    }
}

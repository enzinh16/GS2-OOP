package persistencia;

import excecao.FonteIndisponivelException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Abre conexões com o banco H2 em arquivo. Ao ser criada, aplica o schema.sql
 * (CREATE TABLE IF NOT EXISTS: não apaga dados já existentes).
 */
public class FabricaConexao {
    public static final String FONTE = "banco de dados";
    public static final String URL_PADRAO = "jdbc:h2:./dados/territorio";
    private static final String USUARIO = "sa";
    private static final String SENHA = "";
    private static final String RECURSO_SCHEMA = "/schema.sql";

    private final String url;

    public FabricaConexao() throws FonteIndisponivelException {
        this(URL_PADRAO);
    }

    /** A URL é recebida aqui para os testes poderem usar um banco em pasta temporária. */
    public FabricaConexao(String url) throws FonteIndisponivelException {
        if (url == null || url.isBlank()) {
            throw new FonteIndisponivelException(FONTE, "A URL do banco de dados não foi informada.");
        }
        this.url = url;
        aplicarSchema();
    }

    public Connection abrir() throws FonteIndisponivelException {
        try {
            return DriverManager.getConnection(url, USUARIO, SENHA);
        } catch (SQLException e) {
            throw new FonteIndisponivelException(FONTE,
                    "Não foi possível conectar ao banco de dados: " + e.getMessage(), e);
        }
    }

    private void aplicarSchema() throws FonteIndisponivelException {
        String script = lerSchema();
        try (Connection conexao = DriverManager.getConnection(url, USUARIO, SENHA);
             Statement comando = conexao.createStatement()) {
            for (String instrucao : script.split(";")) {
                if (!instrucao.isBlank()) {
                    comando.execute(instrucao);
                }
            }
        } catch (SQLException e) {
            throw new FonteIndisponivelException(FONTE,
                    "Não foi possível criar as tabelas do banco de dados: " + e.getMessage(), e);
        }
    }

    private String lerSchema() throws FonteIndisponivelException {
        try (InputStream entrada = FabricaConexao.class.getResourceAsStream(RECURSO_SCHEMA)) {
            if (entrada == null) {
                throw new FonteIndisponivelException(FONTE,
                        "Arquivo " + RECURSO_SCHEMA + " não encontrado nos recursos do projeto.");
            }
            try (BufferedReader leitor = new BufferedReader(
                    new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
                return leitor.lines()
                        .filter(linha -> !linha.strip().startsWith("--"))
                        .collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new FonteIndisponivelException(FONTE,
                    "Não foi possível ler o schema.sql: " + e.getMessage(), e);
        }
    }
}

package persistencia;

import dominio.Alerta;
import dominio.NivelAlerta;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Grava e lista os alertas emitidos (JDBC com PreparedStatement). */
public class AlertaDAO {
    private static final String SQL_INSERIR =
            "INSERT INTO alerta (sensor_id, tipo, nivel, valor_previsto, limite_atingido, unidade, "
                    + "estrategia, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_LISTAR =
            "SELECT id, sensor_id, tipo, nivel, valor_previsto, limite_atingido, unidade, estrategia, data_hora "
                    + "FROM alerta ORDER BY data_hora DESC, id DESC";

    private final FabricaConexao fabricaConexao;

    public AlertaDAO(FabricaConexao fabricaConexao) {
        if (fabricaConexao == null) {
            throw new IllegalArgumentException("A fábrica de conexões é obrigatória.");
        }
        this.fabricaConexao = fabricaConexao;
    }

    /** Grava o alerta e preenche o id gerado pelo banco no próprio objeto. */
    public void salvar(Alerta a) throws FonteIndisponivelException {
        if (a == null) {
            throw new IllegalArgumentException("O alerta a salvar não pode ser nulo.");
        }
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getSensorId());
            ps.setString(2, a.getTipo());
            ps.setString(3, a.getNivel().name());
            ps.setDouble(4, a.getValorPrevisto());
            ps.setDouble(5, a.getLimiteAtingido());
            ps.setString(6, a.getUnidade());
            ps.setString(7, a.getEstrategia());
            ps.setObject(8, a.getDataHora());
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    a.setId(chaves.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw erro("salvar o alerta", e);
        }
    }

    /** Todos os alertas já emitidos, do mais recente para o mais antigo. */
    public List<Alerta> listarAlertas() throws FonteIndisponivelException {
        List<Alerta> alertas = new ArrayList<>();
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    Alerta alerta = new Alerta(
                            rs.getString("sensor_id"),
                            rs.getString("tipo"),
                            NivelAlerta.valueOf(rs.getString("nivel")),
                            rs.getDouble("valor_previsto"),
                            rs.getDouble("limite_atingido"),
                            rs.getString("unidade"),
                            rs.getString("estrategia"),
                            rs.getObject("data_hora", LocalDateTime.class));
                    alerta.setId(rs.getLong("id"));
                    alertas.add(alerta);
                } catch (DadosInvalidosException | IllegalArgumentException e) {
                    throw new FonteIndisponivelException(FabricaConexao.FONTE,
                            "Linha corrompida na tabela alerta (id " + rs.getLong("id") + "): "
                                    + e.getMessage(), e);
                }
            }
        } catch (SQLException e) {
            throw erro("listar os alertas", e);
        }
        return alertas;
    }

    private static FonteIndisponivelException erro(String acao, SQLException causa) {
        return new FonteIndisponivelException(FabricaConexao.FONTE,
                "Falha no banco de dados ao " + acao + ": " + causa.getMessage(), causa);
    }
}

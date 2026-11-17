package persistencia;

import dominio.Medicao;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Grava e consulta medições no banco (JDBC com PreparedStatement). */
public class MedicaoDAO {
    private static final String SQL_INSERIR =
            "INSERT INTO medicao (sensor_id, valor, unidade, data_hora) VALUES (?, ?, ?, ?)";
    private static final String SQL_HISTORICO =
            "SELECT id, sensor_id, valor, unidade, data_hora FROM medicao "
                    + "WHERE sensor_id = ? ORDER BY data_hora ASC, id ASC";
    private static final String SQL_ULTIMAS =
            "SELECT id, sensor_id, valor, unidade, data_hora FROM medicao "
                    + "WHERE sensor_id = ? ORDER BY data_hora DESC, id DESC LIMIT ?";

    private final FabricaConexao fabricaConexao;

    public MedicaoDAO(FabricaConexao fabricaConexao) {
        if (fabricaConexao == null) {
            throw new IllegalArgumentException("A fábrica de conexões é obrigatória.");
        }
        this.fabricaConexao = fabricaConexao;
    }

    /** Grava a medição e preenche o id gerado pelo banco no próprio objeto. */
    public void salvar(Medicao m) throws FonteIndisponivelException {
        if (m == null) {
            throw new IllegalArgumentException("A medição a salvar não pode ser nula.");
        }
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getSensorId());
            ps.setDouble(2, m.getValor());
            ps.setString(3, m.getUnidade());
            ps.setObject(4, m.getDataHora());
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    m.setId(chaves.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw erro("salvar a medição", e);
        }
    }

    /** Todas as medições do sensor, da mais antiga para a mais recente. */
    public List<Medicao> buscarHistoricoPorSensor(String sensorId) throws FonteIndisponivelException {
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_HISTORICO)) {
            ps.setString(1, sensorId);
            return lerLinhas(ps);
        } catch (SQLException e) {
            throw erro("consultar o histórico do sensor " + sensorId, e);
        }
    }

    /** As N medições mais recentes do sensor, já em ordem cronológica. */
    public List<Medicao> buscarUltimas(String sensorId, int quantidade) throws FonteIndisponivelException {
        if (quantidade < 1) {
            throw new IllegalArgumentException("A quantidade de medições deve ser pelo menos 1: " + quantidade);
        }
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_ULTIMAS)) {
            ps.setString(1, sensorId);
            ps.setInt(2, quantidade);
            List<Medicao> medicoes = lerLinhas(ps);
            Collections.reverse(medicoes); // veio da mais nova para a mais antiga
            return medicoes;
        } catch (SQLException e) {
            throw erro("consultar as últimas medições do sensor " + sensorId, e);
        }
    }

    private List<Medicao> lerLinhas(PreparedStatement ps) throws SQLException, FonteIndisponivelException {
        List<Medicao> medicoes = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    Medicao medicao = new Medicao(
                            rs.getString("sensor_id"),
                            rs.getDouble("valor"),
                            rs.getString("unidade"),
                            rs.getObject("data_hora", LocalDateTime.class));
                    medicao.setId(rs.getLong("id"));
                    medicoes.add(medicao);
                } catch (DadosInvalidosException e) {
                    throw new FonteIndisponivelException(FabricaConexao.FONTE,
                            "Linha corrompida na tabela medicao (id " + rs.getLong("id") + "): "
                                    + e.getMessage(), e);
                }
            }
        }
        return medicoes;
    }

    private static FonteIndisponivelException erro(String acao, SQLException causa) {
        return new FonteIndisponivelException(FabricaConexao.FONTE,
                "Falha no banco de dados ao " + acao + ": " + causa.getMessage(), causa);
    }
}

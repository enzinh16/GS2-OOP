package persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dominio.Medicao;
import excecao.FonteIndisponivelException;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MedicaoDAOTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 16, 8, 0, 0);

    @TempDir
    Path pasta;

    private String url;
    private MedicaoDAO dao;

    @BeforeEach
    void preparar() throws Exception {
        url = "jdbc:h2:" + pasta.resolve("territorio");
        dao = new MedicaoDAO(new FabricaConexao(url));
    }

    private Medicao medicao(String sensor, double valor, int hora) throws Exception {
        return new Medicao(sensor, valor, "mm/h", INICIO.plusHours(hora));
    }

    @Test
    void salvaELeDeVoltaGerandoOId() throws Exception {
        // Arrange
        Medicao original = medicao("PLU-01", 12.5, 0);

        // Act
        dao.salvar(original);
        List<Medicao> lidas = dao.buscarHistoricoPorSensor("PLU-01");

        // Assert
        assertTrue(original.getId() > 0);
        assertEquals(1, lidas.size());
        Medicao lida = lidas.get(0);
        assertEquals(original.getId(), lida.getId());
        assertEquals("PLU-01", lida.getSensorId());
        assertEquals(12.5, lida.getValor());
        assertEquals("mm/h", lida.getUnidade());
        assertEquals(INICIO, lida.getDataHora());
    }

    @Test
    void historicoVemEmOrdemCronologicaMesmoSalvoDesordenado() throws Exception {
        // Arrange
        dao.salvar(medicao("PLU-01", 30, 2));
        dao.salvar(medicao("PLU-01", 10, 0));
        dao.salvar(medicao("PLU-01", 20, 1));

        // Act
        List<Medicao> historico = dao.buscarHistoricoPorSensor("PLU-01");

        // Assert
        assertEquals(List.of(10.0, 20.0, 30.0),
                historico.stream().map(Medicao::getValor).toList());
    }

    @Test
    void historicoSoTrazMedicoesDoSensorPedido() throws Exception {
        // Arrange
        dao.salvar(medicao("PLU-01", 10, 0));
        dao.salvar(medicao("NIV-01", 99, 0));

        // Act
        List<Medicao> historico = dao.buscarHistoricoPorSensor("NIV-01");

        // Assert
        assertEquals(1, historico.size());
        assertEquals("NIV-01", historico.get(0).getSensorId());
    }

    @Test
    void buscarUltimasDevolveSoAsMaisRecentesEmOrdemCronologica() throws Exception {
        // Arrange: 6 leituras de 0 a 5
        for (int i = 0; i < 6; i++) {
            dao.salvar(medicao("PLU-01", i, i));
        }

        // Act
        List<Medicao> ultimas = dao.buscarUltimas("PLU-01", 3);

        // Assert: as 3 mais recentes (3, 4, 5), da mais antiga para a mais nova
        assertEquals(List.of(3.0, 4.0, 5.0),
                ultimas.stream().map(Medicao::getValor).toList());
    }

    @Test
    void buscarUltimasComMenosLeiturasQueOPedidoDevolveTodas() throws Exception {
        // Arrange
        dao.salvar(medicao("PLU-01", 1, 0));
        dao.salvar(medicao("PLU-01", 2, 1));

        // Act
        List<Medicao> ultimas = dao.buscarUltimas("PLU-01", 5);

        // Assert
        assertEquals(2, ultimas.size());
    }

    @Test
    void buscarUltimasComQuantidadeInvalidaLancaExcecao() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> dao.buscarUltimas("PLU-01", 0));
    }

    @Test
    void dadosSobrevivemAReabrirOBanco() throws Exception {
        // Arrange
        dao.salvar(medicao("PLU-01", 42, 0));

        // Act: nova FabricaConexao na mesma pasta, como ao reiniciar o programa
        MedicaoDAO reaberto = new MedicaoDAO(new FabricaConexao(url));
        List<Medicao> lidas = reaberto.buscarHistoricoPorSensor("PLU-01");

        // Assert
        assertEquals(1, lidas.size());
        assertEquals(42.0, lidas.get(0).getValor());
    }

    @Test
    void urlInvalidaLancaFonteIndisponivelException() {
        // Act
        FonteIndisponivelException erro = assertThrows(FonteIndisponivelException.class,
                () -> new FabricaConexao("jdbc:invalido:xyz"));

        // Assert
        assertEquals(FabricaConexao.FONTE, erro.getFonte());
    }

    @Test
    void bancoInacessivelLancaFonteIndisponivelExceptionAoSalvar() throws Exception {
        // Arrange: fábrica cuja conexão sempre falha
        FabricaConexao quebrada = new FabricaConexao(url) {
            @Override
            public Connection abrir() throws FonteIndisponivelException {
                throw new FonteIndisponivelException(FONTE, "banco fora do ar");
            }
        };
        MedicaoDAO daoQuebrado = new MedicaoDAO(quebrada);
        Medicao medicao = medicao("PLU-01", 1, 0);

        // Act + Assert
        assertThrows(FonteIndisponivelException.class, () -> daoQuebrado.salvar(medicao));
        assertThrows(FonteIndisponivelException.class, () -> daoQuebrado.buscarHistoricoPorSensor("PLU-01"));
        assertThrows(FonteIndisponivelException.class, () -> daoQuebrado.buscarUltimas("PLU-01", 3));
    }
}

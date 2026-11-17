package persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dominio.Alerta;
import dominio.NivelAlerta;
import excecao.FonteIndisponivelException;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlertaDAOTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 16, 10, 0, 0);

    @TempDir
    Path pasta;

    private String url;
    private AlertaDAO dao;

    @BeforeEach
    void preparar() throws Exception {
        url = "jdbc:h2:" + pasta.resolve("territorio");
        dao = new AlertaDAO(new FabricaConexao(url));
    }

    private Alerta alerta(NivelAlerta nivel, double previsto, double limite, int hora) throws Exception {
        return new Alerta("PLU-01", "chuva", nivel, previsto, limite, "mm/h",
                "Média móvel", INICIO.plusHours(hora));
    }

    @Test
    void salvaELeDeVoltaTodosOsCamposGerandoOId() throws Exception {
        // Arrange
        Alerta original = alerta(NivelAlerta.CRITICO, 80, 70, 0);

        // Act
        dao.salvar(original);
        List<Alerta> lidos = dao.listarAlertas();

        // Assert
        assertTrue(original.getId() > 0);
        assertEquals(1, lidos.size());
        Alerta lido = lidos.get(0);
        assertEquals(original.getId(), lido.getId());
        assertEquals("PLU-01", lido.getSensorId());
        assertEquals("chuva", lido.getTipo());
        assertEquals(NivelAlerta.CRITICO, lido.getNivel());
        assertEquals(80.0, lido.getValorPrevisto());
        assertEquals(70.0, lido.getLimiteAtingido());
        assertEquals("mm/h", lido.getUnidade());
        assertEquals("Média móvel", lido.getEstrategia());
        assertEquals(INICIO, lido.getDataHora());
    }

    @Test
    void listaOsMaisRecentesPrimeiro() throws Exception {
        // Arrange
        dao.salvar(alerta(NivelAlerta.ATENCAO, 25, 20, 0));
        dao.salvar(alerta(NivelAlerta.CRITICO, 80, 70, 2));
        dao.salvar(alerta(NivelAlerta.ALERTA, 45, 40, 1));

        // Act
        List<Alerta> lidos = dao.listarAlertas();

        // Assert
        assertEquals(List.of(NivelAlerta.CRITICO, NivelAlerta.ALERTA, NivelAlerta.ATENCAO),
                lidos.stream().map(Alerta::getNivel).toList());
    }

    @Test
    void listaVaziaQuandoNaoHaAlertas() throws Exception {
        // Act + Assert
        assertTrue(dao.listarAlertas().isEmpty());
    }

    @Test
    void alertasSobrevivemAReabrirOBanco() throws Exception {
        // Arrange
        dao.salvar(alerta(NivelAlerta.ALERTA, 45, 40, 0));

        // Act
        AlertaDAO reaberto = new AlertaDAO(new FabricaConexao(url));
        List<Alerta> lidos = reaberto.listarAlertas();

        // Assert
        assertEquals(1, lidos.size());
        assertEquals(NivelAlerta.ALERTA, lidos.get(0).getNivel());
    }

    @Test
    void bancoInacessivelLancaFonteIndisponivelException() throws Exception {
        // Arrange
        FabricaConexao quebrada = new FabricaConexao(url) {
            @Override
            public Connection abrir() throws FonteIndisponivelException {
                throw new FonteIndisponivelException(FONTE, "banco fora do ar");
            }
        };
        AlertaDAO daoQuebrado = new AlertaDAO(quebrada);
        Alerta alerta = alerta(NivelAlerta.ATENCAO, 25, 20, 0);

        // Act + Assert
        assertThrows(FonteIndisponivelException.class, () -> daoQuebrado.salvar(alerta));
        assertThrows(FonteIndisponivelException.class, daoQuebrado::listarAlertas);
    }
}

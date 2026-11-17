package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DadosTerritorioTest {

    private static final String CODIGO = "coord2026";
    private DadosTerritorio territorio;

    @BeforeEach
    void preparar() throws DadosInvalidosException {
        territorio = new DadosTerritorio("Córrego da Várzea", "Zona Leste", CODIGO);
    }

    @Test
    void temLimiaresPadraoParaOsTresSensores() throws DadosInvalidosException {
        // Act + Assert
        assertEquals(20, territorio.getLimiares("chuva").getAtencao());
        assertEquals(120, territorio.getLimiares("nivel").getAlerta());
        assertEquals(90, territorio.getLimiares("umidade").getCritico());
    }

    @Test
    void alteraLimiaresComOCodigoCorreto() throws DadosInvalidosException {
        // Act
        territorio.setLimiares(CODIGO, "chuva", 10, 30, 60);

        // Assert
        assertEquals(NivelAlerta.ATENCAO, territorio.classificar("chuva", 10));
        assertEquals(NivelAlerta.CRITICO, territorio.classificar("chuva", 60));
    }

    @Test
    void codigoErradoNaoAlteraOsLimiares() throws DadosInvalidosException {
        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.setLimiares("errado", "chuva", 10, 30, 60));

        // Assert
        assertEquals(true, erro.getMessage().contains("Código de acesso incorreto"));
        assertEquals(20, territorio.getLimiares("chuva").getAtencao());
        assertThrows(DadosInvalidosException.class,
                () -> territorio.setLimiares(null, "chuva", 10, 30, 60));
    }

    @Test
    void limiarInvalidoNaoAlteraOsValoresAnteriores() throws DadosInvalidosException {
        // Act
        assertThrows(DadosInvalidosException.class,
                () -> territorio.setLimiares(CODIGO, "chuva", 50, 40, 70));

        // Assert
        assertEquals(20, territorio.getLimiares("chuva").getAtencao());
    }

    @Test
    void rejeitaTipoDesconhecidoNomeVazioECodigoVazio() {
        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> territorio.getLimiares("vento"));
        assertThrows(DadosInvalidosException.class, () -> territorio.setNome(" "));
        assertThrows(DadosInvalidosException.class,
                () -> new DadosTerritorio(null, "Zona Leste", CODIGO));
        assertThrows(DadosInvalidosException.class,
                () -> new DadosTerritorio("Córrego", "Zona Leste", " "));
    }
}

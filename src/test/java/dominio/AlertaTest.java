package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import excecao.DadosInvalidosException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    private DadosTerritorio territorio;

    @BeforeEach
    void preparar() throws DadosInvalidosException {
        territorio = new DadosTerritorio("Córrego da Várzea", "Zona Leste", "coord2026");
    }

    @Test
    void emiteAlertaDoNivelAtingidoComOLimiteCorreto() throws DadosInvalidosException {
        // Act
        Optional<Alerta> resultado = Alerta.avaliar("PLU-01", "chuva", "mm/h", 45,
                "Média móvel", territorio, AGORA);

        // Assert
        assertTrue(resultado.isPresent());
        Alerta alerta = resultado.get();
        assertEquals(NivelAlerta.ALERTA, alerta.getNivel());
        assertEquals(40, alerta.getLimiteAtingido());
        assertEquals(45, alerta.getValorPrevisto());
    }

    @Test
    void naoEmiteAlertaAbaixoDaAtencao() throws DadosInvalidosException {
        // Act
        Optional<Alerta> resultado = Alerta.avaliar("PLU-01", "chuva", "mm/h", 5,
                "Média móvel", territorio, AGORA);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void motivoExplicaSensorValorLimiteEEstrategia() throws DadosInvalidosException {
        // Arrange
        Alerta alerta = Alerta.avaliar("PLU-01", "chuva", "mm/h", 75,
                "Média móvel", territorio, AGORA).orElseThrow();

        // Act
        String motivo = alerta.getMotivo();

        // Assert
        assertTrue(motivo.contains("PLU-01"));
        assertTrue(motivo.contains("75,0 mm/h"));
        assertTrue(motivo.contains("CRÍTICO"));
        assertTrue(motivo.contains("70,0"));
        assertTrue(motivo.contains("Média móvel"));
    }

    @Test
    void rejeitaAlertaComNivelNormalOuValorAbaixoDoLimite() {
        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> new Alerta("PLU-01", "chuva",
                NivelAlerta.NORMAL, 10, 0, "mm/h", "Média móvel", AGORA));
        assertThrows(DadosInvalidosException.class, () -> new Alerta("PLU-01", "chuva",
                NivelAlerta.ALERTA, 30, 40, "mm/h", "Média móvel", AGORA));
    }

    @Test
    void naoAvaliaValorDeTipoSemLimiares() {
        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> Alerta.avaliar("VEN-01", "vento",
                "km/h", 50, "Média móvel", territorio, AGORA));
    }
}

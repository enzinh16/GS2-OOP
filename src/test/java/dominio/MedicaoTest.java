package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import excecao.DadosInvalidosException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MedicaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Test
    void criaMedicaoValidaGuardandoOsDados() throws DadosInvalidosException {
        // Arrange + Act
        Medicao medicao = new Medicao("PLU-01", 12.5, "mm/h", AGORA);

        // Assert
        assertEquals("PLU-01", medicao.getSensorId());
        assertEquals(12.5, medicao.getValor());
        assertEquals("mm/h", medicao.getUnidade());
        assertEquals(AGORA, medicao.getDataHora());
    }

    @Test
    void rejeitaValorNaN() {
        // Arrange
        double valorInvalido = Double.NaN;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Medicao("PLU-01", valorInvalido, "mm/h", AGORA));

        // Assert
        assertEquals("Valor de medição inválido: NaN", erro.getMessage());
    }

    @Test
    void rejeitaSensorVazioEDataNula() {
        // Arrange + Act + Assert
        assertThrows(DadosInvalidosException.class, () -> new Medicao(" ", 10, "mm/h", AGORA));
        assertThrows(DadosInvalidosException.class, () -> new Medicao("PLU-01", 10, "mm/h", null));
    }
}

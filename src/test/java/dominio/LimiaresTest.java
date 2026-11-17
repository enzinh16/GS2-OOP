package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

class LimiaresTest {

    @Test
    void classificaNosLimitesExatosEAbaixoDeles() throws DadosInvalidosException {
        // Arrange
        Limiares limiares = new Limiares(20, 40, 70);

        // Act + Assert
        assertEquals(NivelAlerta.NORMAL, limiares.classificar(19.9));
        assertEquals(NivelAlerta.ATENCAO, limiares.classificar(20));
        assertEquals(NivelAlerta.ALERTA, limiares.classificar(40));
        assertEquals(NivelAlerta.CRITICO, limiares.classificar(70));
    }

    @Test
    void rejeitaLimiaresForaDeOrdem() {
        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Limiares(50, 40, 70));

        // Assert
        assertEquals(true, erro.getMessage().contains("crescer"));
    }

    @Test
    void rejeitaLimiarNegativoOuNaN() {
        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> new Limiares(-5, 40, 70));
        assertThrows(DadosInvalidosException.class, () -> new Limiares(Double.NaN, 40, 70));
    }

    @Test
    void naoClassificaValorNaN() throws DadosInvalidosException {
        // Arrange
        Limiares limiares = new Limiares(20, 40, 70);

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> limiares.classificar(Double.NaN));
    }
}

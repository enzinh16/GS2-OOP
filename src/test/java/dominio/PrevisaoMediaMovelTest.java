package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import excecao.DadosInvalidosException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrevisaoMediaMovelTest {

    @Test
    void calculaMediaDasUltimasNLeituras() throws DadosInvalidosException {
        // Arrange: 7 leituras, só as 5 últimas (10..50) entram: média 30
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5);
        List<Medicao> historico = Historicos.de(100, 100, 10, 20, 30, 40, 50);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(historico);

        // Assert
        assertTrue(resultado.temValor());
        assertEquals(30.0, resultado.getValor());
        assertEquals(5, resultado.getLeiturasUsadas());
        assertEquals("PLU-01", resultado.getSensorId());
    }

    @Test
    void ordenaPorDataHoraMesmoComHistoricoEmbaralhado() throws DadosInvalidosException {
        // Arrange: a ordem cronológica é 10, 20, 90; a média das 2 últimas é 55
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(2);
        List<Medicao> historico = new ArrayList<>(Historicos.de(10, 20, 90));
        Collections.reverse(historico);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(historico);

        // Assert
        assertEquals(55.0, resultado.getValor());
    }

    @Test
    void semLeiturasSuficientesNaoPrevNada() throws DadosInvalidosException {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(Historicos.de(10, 20, 30, 40));

        // Assert
        assertFalse(resultado.temValor());
        assertTrue(resultado.getInterpretacao().contains("Dados insuficientes"));
        assertThrows(IllegalStateException.class, resultado::getValor);
    }

    @Test
    void historicoVazioTambemEInsuficiente() throws DadosInvalidosException {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(3);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(new ArrayList<>());

        // Assert
        assertFalse(resultado.temValor());
    }

    @Test
    void rejeitaHistoricoNuloOuQueMisturaSensores() throws DadosInvalidosException {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(2);
        List<Medicao> misturado = new ArrayList<>(Historicos.de("PLU-01", 10, 20));
        misturado.addAll(Historicos.de("NIV-01", 30));

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> estrategia.prever(null));
        assertThrows(DadosInvalidosException.class, () -> estrategia.prever(misturado));
    }

    @Test
    void rejeitaJanelaInvalida() {
        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> new PrevisaoMediaMovel(0));
    }

    @Test
    void descreverUsaADescricaoDaSuperclasse() throws DadosInvalidosException {
        // Act
        String descricao = new PrevisaoMediaMovel(5).descrever();

        // Assert
        assertTrue(descricao.contains("Média móvel (mínimo de 5 leituras)"));
        assertTrue(descricao.contains("últimas 5 leituras"));
    }
}

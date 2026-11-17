package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

class PrevisaoComparacaoTest {

    @Test
    void projetaTendenciaDeAlta() throws DadosInvalidosException {
        // Arrange: bloco anterior (10, 20) tem média 15; atual (30, 40) tem média 35.
        // Variação +20, então a projeção é 35 + 20 = 55.
        PrevisaoComparacao estrategia = new PrevisaoComparacao(2);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(Historicos.de(10, 20, 30, 40));

        // Assert
        assertEquals(55.0, resultado.getValor());
        assertTrue(resultado.getInterpretacao().contains("em alta"));
    }

    @Test
    void projetaTendenciaDeQuedaSemFicarNegativa() throws DadosInvalidosException {
        // Arrange: anterior 40, atual 10, variação -30, 10 - 30 = -20, limitado a 0
        PrevisaoComparacao estrategia = new PrevisaoComparacao(2);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(Historicos.de(40, 40, 10, 10));

        // Assert
        assertEquals(0.0, resultado.getValor());
        assertTrue(resultado.getInterpretacao().contains("em queda"));
    }

    @Test
    void historicoEstavelMantemAMedia() throws DadosInvalidosException {
        // Arrange
        PrevisaoComparacao estrategia = new PrevisaoComparacao(2);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(Historicos.de(20, 20, 20, 20));

        // Assert
        assertEquals(20.0, resultado.getValor());
        assertTrue(resultado.getInterpretacao().contains("estável"));
    }

    @Test
    void exigeDoisBlocosCompletosDeLeituras() throws DadosInvalidosException {
        // Arrange: janela 3 exige 6 leituras
        PrevisaoComparacao estrategia = new PrevisaoComparacao(3);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(Historicos.de(10, 20, 30, 40, 50));

        // Assert
        assertEquals(6, estrategia.getMinimoLeituras());
        assertFalse(resultado.temValor());
        assertTrue(resultado.getInterpretacao().contains("exige pelo menos 6"));
    }

    @Test
    void rejeitaHistoricoQueMisturaSensores() throws DadosInvalidosException {
        // Arrange
        PrevisaoComparacao estrategia = new PrevisaoComparacao(1);
        java.util.List<Medicao> misturado = new java.util.ArrayList<>(Historicos.de("PLU-01", 10));
        misturado.addAll(Historicos.de("NIV-01", 30));

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> estrategia.prever(misturado));
    }

    @Test
    void estrategiasDiferentesDaoResultadosDiferentesParaOMesmoHistorico() throws DadosInvalidosException {
        // Arrange: o mesmo histórico, duas estratégias (polimorfismo)
        java.util.List<Medicao> historico = Historicos.de(10, 20, 30, 40);
        EstrategiaPrevisao media = new PrevisaoMediaMovel(2);
        EstrategiaPrevisao comparacao = new PrevisaoComparacao(2);

        // Act
        double deMedia = media.prever(historico).getValor();         // (30 + 40) / 2
        double deComparacao = comparacao.prever(historico).getValor(); // 35 + (35 - 15)

        // Assert
        assertEquals(35.0, deMedia);
        assertEquals(55.0, deComparacao);
    }
}

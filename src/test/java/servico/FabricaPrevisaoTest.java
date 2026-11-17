package servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dominio.EstrategiaPrevisao;
import dominio.Historicos;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;
import java.util.List;
import org.junit.jupiter.api.Test;

class FabricaPrevisaoTest {

    @Test
    void criaAEstrategiaCorrespondenteAoTipo() throws DadosInvalidosException {
        // Arrange
        FabricaPrevisao fabrica = new FabricaPrevisao();

        // Act
        EstrategiaPrevisao media = fabrica.criar(TipoPrevisao.MEDIA_MOVEL);
        EstrategiaPrevisao comparacao = fabrica.criar(TipoPrevisao.COMPARACAO);

        // Assert
        assertTrue(media instanceof PrevisaoMediaMovel);
        assertTrue(comparacao instanceof PrevisaoComparacao);
    }

    @Test
    void usaAsJanelasPadrao() throws DadosInvalidosException {
        // Arrange
        FabricaPrevisao fabrica = new FabricaPrevisao();

        // Act + Assert: média móvel exige 5 leituras; comparação exige 2 x 3 = 6
        assertEquals(5, fabrica.criar(TipoPrevisao.MEDIA_MOVEL).getMinimoLeituras());
        assertEquals(6, fabrica.criar(TipoPrevisao.COMPARACAO).getMinimoLeituras());
    }

    @Test
    void respeitaJanelasPersonalizadas() throws DadosInvalidosException {
        // Arrange
        FabricaPrevisao fabrica = new FabricaPrevisao(4, 2);

        // Act + Assert
        assertEquals(4, fabrica.criar(TipoPrevisao.MEDIA_MOVEL).getMinimoLeituras());
        assertEquals(4, fabrica.criar(TipoPrevisao.COMPARACAO).getMinimoLeituras());
    }

    @Test
    void criarTodasDevolveUmaEstrategiaPorTipoSemPrecisarSaberQualEQual() throws DadosInvalidosException {
        // Arrange
        FabricaPrevisao fabrica = new FabricaPrevisao(2, 1);

        // Act
        List<EstrategiaPrevisao> todas = fabrica.criarTodas();

        // Assert: o código só usa o tipo abstrato e obtém previsões diferentes
        assertEquals(TipoPrevisao.values().length, todas.size());
        double primeira = todas.get(0).prever(Historicos.de(10, 20, 30)).getValor();
        double segunda = todas.get(1).prever(Historicos.de(10, 20, 30)).getValor();
        assertEquals(25.0, primeira); // média de 20 e 30
        assertEquals(40.0, segunda);  // 30 + (30 - 20)
    }

    @Test
    void rejeitaTipoNulo() {
        // Arrange
        FabricaPrevisao fabrica = new FabricaPrevisao();

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> fabrica.criar(null));
    }

    @Test
    void janelaInvalidaFalhaAoCriar() {
        // Arrange
        FabricaPrevisao fabrica = new FabricaPrevisao(0, 3);

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> fabrica.criar(TipoPrevisao.MEDIA_MOVEL));
    }
}

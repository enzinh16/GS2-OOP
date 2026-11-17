package servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.Historicos;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.SensorPluviometro;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistencia.AlertaDAO;
import persistencia.MedicaoDAO;

@ExtendWith(MockitoExtension.class)
class ServicoMonitoramentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 12, 0, 0);

    @Mock
    private MedicaoDAO medicaoDAO;
    @Mock
    private AlertaDAO alertaDAO;

    private SensorPluviometro sensor;
    private ServicoMonitoramento servico;

    @BeforeEach
    void preparar() throws DadosInvalidosException {
        DadosTerritorio territorio = new DadosTerritorio("Córrego da Várzea", "Zona Leste", "coord2026");
        Clock relogioFixo = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        sensor = new SensorPluviometro("PLU-01", "Zona Leste", new Random(1));
        servico = new ServicoMonitoramento(medicaoDAO, alertaDAO, new FabricaPrevisao(), territorio, relogioFixo);
    }

    @Test
    void previsaoCriticaGravaOAlertaCerto() throws Exception {
        // Arrange: média de 80 mm/h, acima do limite crítico (70)
        when(medicaoDAO.buscarUltimas("PLU-01", 5)).thenReturn(Historicos.de(80, 80, 80, 80, 80));

        // Act
        AvaliacaoPrevisao avaliacao = servico.preverEAvaliar(sensor, TipoPrevisao.MEDIA_MOVEL);

        // Assert
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaDAO).salvar(captor.capture());
        Alerta gravado = captor.getValue();
        assertEquals(NivelAlerta.CRITICO, gravado.getNivel());
        assertEquals("PLU-01", gravado.getSensorId());
        assertEquals("chuva", gravado.getTipo());
        assertEquals(80.0, gravado.getValorPrevisto());
        assertEquals(70.0, gravado.getLimiteAtingido());
        assertEquals("Média móvel", gravado.getEstrategia());
        assertEquals(AGORA, gravado.getDataHora());
        assertTrue(avaliacao.getAlerta().isPresent());
        assertTrue(avaliacao.getResultado().temValor());
    }

    @Test
    void previsaoAbaixoDaAtencaoNaoGravaAlerta() throws Exception {
        // Arrange: média de 2 mm/h, abaixo do limite de atenção (20)
        when(medicaoDAO.buscarUltimas("PLU-01", 5)).thenReturn(Historicos.de(1, 2, 3, 2, 2));

        // Act
        AvaliacaoPrevisao avaliacao = servico.preverEAvaliar(sensor, TipoPrevisao.MEDIA_MOVEL);

        // Assert
        verify(alertaDAO, never()).salvar(any());
        assertTrue(avaliacao.getResultado().temValor());
        assertFalse(avaliacao.getAlerta().isPresent());
    }

    @Test
    void historicoInsuficienteNaoGravaNada() throws Exception {
        // Arrange: só 2 leituras, a média móvel exige 5
        when(medicaoDAO.buscarUltimas("PLU-01", 5)).thenReturn(Historicos.de(80, 80));

        // Act
        AvaliacaoPrevisao avaliacao = servico.preverEAvaliar(sensor, TipoPrevisao.MEDIA_MOVEL);

        // Assert: o sistema se cala
        verify(alertaDAO, never()).salvar(any());
        assertFalse(avaliacao.getResultado().temValor());
        assertFalse(avaliacao.getAlerta().isPresent());
    }

    @Test
    void usaOMinimoDeLeiturasDaEstrategiaEscolhidaPelaFabrica() throws Exception {
        // Arrange: a comparação entre períodos exige 6 leituras (2 x 3)
        when(medicaoDAO.buscarUltimas("PLU-01", 6)).thenReturn(Historicos.de(10, 10, 10, 10, 10, 10));

        // Act
        AvaliacaoPrevisao avaliacao = servico.preverEAvaliar(sensor, TipoPrevisao.COMPARACAO);

        // Assert
        verify(medicaoDAO).buscarUltimas("PLU-01", 6);
        assertEquals("Comparação entre períodos", avaliacao.getResultado().getEstrategia());
        assertTrue(avaliacao.getResultado().temValor());
    }

    @Test
    void falhaDoBancoNaConsultaSobeComoFonteIndisponivel() throws Exception {
        // Arrange
        when(medicaoDAO.buscarUltimas("PLU-01", 5))
                .thenThrow(new FonteIndisponivelException("banco de dados", "banco fora do ar"));

        // Act + Assert
        assertThrows(FonteIndisponivelException.class,
                () -> servico.preverEAvaliar(sensor, TipoPrevisao.MEDIA_MOVEL));
        verifyNoInteractions(alertaDAO);
    }

    @Test
    void leituraValidaEhGravadaComAHoraDoRelogio() throws Exception {
        // Arrange
        sensor.forcarProximaLeitura(12.5);

        // Act
        Medicao retornada = servico.registrarLeitura(sensor);

        // Assert
        ArgumentCaptor<Medicao> captor = ArgumentCaptor.forClass(Medicao.class);
        verify(medicaoDAO).salvar(captor.capture());
        Medicao gravada = captor.getValue();
        assertEquals(12.5, gravada.getValor());
        assertEquals("PLU-01", gravada.getSensorId());
        assertEquals("mm/h", gravada.getUnidade());
        assertEquals(AGORA, gravada.getDataHora());
        assertEquals(gravada, retornada);
    }

    @Test
    void leituraInvalidaNaoGravaMedicao() {
        // Arrange: 999 mm/h está fora da faixa física do pluviômetro
        sensor.forcarProximaLeitura(999);

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> servico.registrarLeitura(sensor));
        verifyNoInteractions(medicaoDAO);
    }

    @Test
    void sensorEmFalhaNaoGravaMedicao() {
        // Arrange
        sensor.simularFalha(true);

        // Act + Assert
        assertThrows(FonteIndisponivelException.class, () -> servico.registrarLeitura(sensor));
        verifyNoInteractions(medicaoDAO);
    }

    @Test
    void tipoDePrevisaoNuloLancaDadosInvalidos() {
        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> servico.preverEAvaliar(sensor, null));
        verifyNoInteractions(medicaoDAO, alertaDAO);
    }
}

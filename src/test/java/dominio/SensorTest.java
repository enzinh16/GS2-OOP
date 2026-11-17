package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import java.time.LocalDateTime;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SensorTest {

    @Test
    void leituraSimuladaFicaDentroDaFaixaDoSensor() throws Exception {
        // Arrange
        SensorPluviometro sensor = new SensorPluviometro("PLU-01", "Zona Leste", new Random(42));

        // Act + Assert
        for (int i = 0; i < 200; i++) {
            double valor = sensor.lerValor();
            assertTrue(valor >= sensor.getFaixaMin() && valor <= sensor.getFaixaMax());
        }
    }

    @Test
    void sensorEmFalhaLancaFonteIndisponivelEFicaIndisponivel() {
        // Arrange
        SensorNivelCorrego sensor = new SensorNivelCorrego("NIV-01", "Zona Leste", new Random(1));
        sensor.simularFalha(true);

        // Act
        FonteIndisponivelException erro = assertThrows(FonteIndisponivelException.class,
                sensor::lerValor);

        // Assert
        assertEquals("NIV-01", erro.getFonte());
        assertFalse(sensor.verificarFuncionamento());
        assertEquals(StatusComponente.INDISPONIVEL, sensor.verificarStatus());
    }

    @Test
    void leituraForaDaFaixaLancaDadosInvalidos() {
        // Arrange
        SensorUmidadeSolo sensor = new SensorUmidadeSolo("UMI-01", "Zona Leste", new Random(1));
        sensor.forcarProximaLeitura(150); // umidade só vai de 0 a 100

        // Act + Assert
        assertThrows(DadosInvalidosException.class, sensor::lerValor);
    }

    @Test
    void lerMedicaoDevolveMedicaoDoProprioSensor() throws Exception {
        // Arrange
        SensorPluviometro sensor = new SensorPluviometro("PLU-01", "Zona Leste", new Random(7));
        sensor.forcarProximaLeitura(33.3);
        LocalDateTime agora = LocalDateTime.of(2026, 11, 16, 10, 0);

        // Act
        Medicao medicao = sensor.lerMedicao(agora);

        // Assert
        assertEquals("PLU-01", medicao.getSensorId());
        assertEquals(33.3, medicao.getValor());
        assertEquals("mm/h", medicao.getUnidade());
    }

    @Test
    void sensorVoltaAFuncionarDepoisDaFalha() throws Exception {
        // Arrange
        SensorPluviometro sensor = new SensorPluviometro("PLU-01", "Zona Leste", new Random(3));
        sensor.simularFalha(true);
        sensor.simularFalha(false);

        // Act + Assert
        assertTrue(sensor.verificarFuncionamento());
        assertEquals(StatusComponente.OPERACIONAL, sensor.verificarStatus());
        sensor.lerValor(); // não lança
    }
}

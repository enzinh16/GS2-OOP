package servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dominio.Historicos;
import dominio.Medicao;
import excecao.FonteIndisponivelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportadorCSVTest {

    @TempDir
    Path pasta;

    @Test
    void gravaCabecalhoEUmaLinhaPorMedicao() throws Exception {
        // Arrange
        List<Medicao> medicoes = Historicos.de(10.5, 20);
        Path arquivo = pasta.resolve("historico.csv");

        // Act
        new ExportadorCSV().exportar(medicoes, arquivo);
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);

        // Assert
        assertEquals(3, linhas.size());
        assertEquals(ExportadorCSV.CABECALHO, linhas.get(0));
        assertEquals("0;PLU-01;10.50;mm/h;2026-11-16T08:00", linhas.get(1));
        assertEquals("0;PLU-01;20.00;mm/h;2026-11-16T09:00", linhas.get(2));
    }

    @Test
    void destinoInacessivelLancaFonteIndisponivelException() {
        // Arrange: a pasta de destino não existe
        Path invalido = pasta.resolve("nao-existe").resolve("historico.csv");

        // Act + Assert
        assertThrows(FonteIndisponivelException.class,
                () -> new ExportadorCSV().exportar(List.of(), invalido));
    }
}

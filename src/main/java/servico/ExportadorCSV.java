package servico;

import dominio.Medicao;
import excecao.FonteIndisponivelException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Bônus: exporta uma lista de medições para um arquivo CSV (separador ";", valores com ponto decimal). */
public class ExportadorCSV {
    public static final String CABECALHO = "id;sensor_id;valor;unidade;data_hora";

    public void exportar(List<Medicao> medicoes, Path destino) throws FonteIndisponivelException {
        if (medicoes == null || destino == null) {
            throw new IllegalArgumentException("A lista de medições e o arquivo de destino são obrigatórios.");
        }
        try (BufferedWriter escritor = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            escritor.write(CABECALHO);
            escritor.newLine();
            for (Medicao m : medicoes) {
                escritor.write(String.format(Locale.ROOT, "%d;%s;%.2f;%s;%s",
                        m.getId(), m.getSensorId(), m.getValor(), m.getUnidade(), m.getDataHora().withNano(0)));
                escritor.newLine();
            }
        } catch (IOException e) {
            throw new FonteIndisponivelException("arquivo CSV",
                    "Não foi possível gravar o CSV em " + destino + ": " + e.getMessage(), e);
        }
    }
}

package dominio;

import excecao.DadosInvalidosException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Apoio dos testes: monta históricos de chuva do sensor PLU-01, uma leitura por hora. */
public final class Historicos {
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 16, 8, 0);

    private Historicos() { }

    public static List<Medicao> de(double... valores) throws DadosInvalidosException {
        return de("PLU-01", valores);
    }

    public static List<Medicao> de(String sensorId, double... valores) throws DadosInvalidosException {
        List<Medicao> lista = new ArrayList<>();
        for (int i = 0; i < valores.length; i++) {
            lista.add(new Medicao(sensorId, valores[i], "mm/h", INICIO.plusHours(i)));
        }
        return lista;
    }
}

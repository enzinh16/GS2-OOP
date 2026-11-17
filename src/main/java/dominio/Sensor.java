package dominio;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import java.time.LocalDateTime;

public interface Sensor {
    String getId();

    /** Lê o valor atual, já validado contra a faixa física do sensor. */
    double lerValor() throws FonteIndisponivelException, DadosInvalidosException;

    boolean verificarFuncionamento();

    String getTipo();

    String getUnidade();

    /** Lê o sensor e devolve a leitura como Medicao. */
    default Medicao lerMedicao(LocalDateTime dataHora)
            throws FonteIndisponivelException, DadosInvalidosException {
        return new Medicao(getId(), lerValor(), getUnidade(), dataHora);
    }
}

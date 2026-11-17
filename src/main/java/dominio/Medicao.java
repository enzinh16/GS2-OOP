package dominio;

import excecao.DadosInvalidosException;
import java.time.LocalDateTime;
import java.util.Locale;

public class Medicao {
    private long id; // 0 até o banco gerar o id
    private final String sensorId;
    private final double valor;
    private final String unidade;
    private final LocalDateTime dataHora;

    public Medicao(String sensorId, double valor, String unidade, LocalDateTime dataHora)
            throws DadosInvalidosException {
        if (sensorId == null || sensorId.isBlank()) {
            throw new DadosInvalidosException("O id do sensor é obrigatório.");
        }
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new DadosInvalidosException("Valor de medição inválido: " + valor);
        }
        if (unidade == null || unidade.isBlank()) {
            throw new DadosInvalidosException("A unidade da medição é obrigatória.");
        }
        if (dataHora == null) {
            throw new DadosInvalidosException("A data/hora da medição é obrigatória.");
        }
        this.sensorId = sensorId;
        this.valor = valor;
        this.unidade = unidade;
        this.dataHora = dataHora;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSensorId() { return sensorId; }
    public double getValor() { return valor; }
    public String getUnidade() { return unidade; }
    public LocalDateTime getDataHora() { return dataHora; }

    @Override
    public String toString() {
        return String.format(Locale.forLanguageTag("pt-BR"), "[%s] %s = %.1f %s",
                dataHora.withNano(0), sensorId, valor, unidade);
    }
}

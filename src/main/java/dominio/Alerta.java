package dominio;

import excecao.DadosInvalidosException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

public class Alerta {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private long id; // 0 até o banco gerar o id
    private final String sensorId;
    private final String tipo;
    private final NivelAlerta nivel;
    private final double valorPrevisto;
    private final double limiteAtingido;
    private final String unidade;
    private final String estrategia;
    private final LocalDateTime dataHora;

    public Alerta(String sensorId, String tipo, NivelAlerta nivel, double valorPrevisto,
                  double limiteAtingido, String unidade, String estrategia, LocalDateTime dataHora)
            throws DadosInvalidosException {
        if (sensorId == null || sensorId.isBlank()) {
            throw new DadosInvalidosException("O id do sensor do alerta é obrigatório.");
        }
        if (tipo == null || tipo.isBlank()) {
            throw new DadosInvalidosException("O tipo do sensor do alerta é obrigatório.");
        }
        if (nivel == null || nivel == NivelAlerta.NORMAL) {
            throw new DadosInvalidosException("Um alerta precisa ter nível ATENÇÃO, ALERTA ou CRÍTICO.");
        }
        if (!Double.isFinite(valorPrevisto) || !Double.isFinite(limiteAtingido)) {
            throw new DadosInvalidosException("Valor previsto e limite do alerta devem ser números válidos.");
        }
        if (valorPrevisto < limiteAtingido) {
            throw new DadosInvalidosException(String.format(PT_BR,
                    "Valor previsto (%.1f) abaixo do limite informado (%.1f).", valorPrevisto, limiteAtingido));
        }
        if (unidade == null || unidade.isBlank() || estrategia == null || estrategia.isBlank()) {
            throw new DadosInvalidosException("Unidade e estratégia do alerta são obrigatórias.");
        }
        if (dataHora == null) {
            throw new DadosInvalidosException("A data/hora do alerta é obrigatória.");
        }
        this.sensorId = sensorId;
        this.tipo = tipo;
        this.nivel = nivel;
        this.valorPrevisto = valorPrevisto;
        this.limiteAtingido = limiteAtingido;
        this.unidade = unidade;
        this.estrategia = estrategia;
        this.dataHora = dataHora;
    }

    /**
     * Compara a previsão com os limiares do território.
     * Devolve o alerta do nível atingido, ou vazio se o valor estiver abaixo da ATENÇÃO.
     */
    public static Optional<Alerta> avaliar(String sensorId, String tipo, String unidade,
                                           double valorPrevisto, String estrategia,
                                           DadosTerritorio territorio, LocalDateTime dataHora)
            throws DadosInvalidosException {
        NivelAlerta nivel = territorio.classificar(tipo, valorPrevisto);
        if (nivel == NivelAlerta.NORMAL) {
            return Optional.empty();
        }
        double limite = territorio.getLimiares(tipo).limitePara(nivel);
        return Optional.of(new Alerta(sensorId, tipo, nivel, valorPrevisto, limite,
                unidade, estrategia, dataHora));
    }

    /** Texto que explica por que o alerta foi emitido. */
    public String getMotivo() {
        return String.format(PT_BR,
                "Previsão de %.1f %s para %s (estratégia: %s) atingiu o limite de %s (%.1f %s). %s",
                valorPrevisto, unidade, sensorId, estrategia,
                nivel.getRotulo(), limiteAtingido, unidade, nivel.getOrientacao());
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSensorId() { return sensorId; }
    public String getTipo() { return tipo; }
    public NivelAlerta getNivel() { return nivel; }
    public double getValorPrevisto() { return valorPrevisto; }
    public double getLimiteAtingido() { return limiteAtingido; }
    public String getUnidade() { return unidade; }
    public String getEstrategia() { return estrategia; }
    public LocalDateTime getDataHora() { return dataHora; }

    @Override
    public String toString() {
        return "[" + nivel.getRotulo() + "] " + getMotivo();
    }
}

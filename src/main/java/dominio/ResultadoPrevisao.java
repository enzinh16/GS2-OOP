package dominio;

import java.util.Locale;

/** Resultado de uma previsão: o valor previsto e o que ele significa (ou por que não há valor). */
public final class ResultadoPrevisao {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final String sensorId;
    private final String estrategia;
    private final Double valor; // null quando não há dados suficientes
    private final int leiturasUsadas;
    private final String interpretacao;

    private ResultadoPrevisao(String sensorId, String estrategia, Double valor,
                              int leiturasUsadas, String interpretacao) {
        this.sensorId = sensorId;
        this.estrategia = estrategia;
        this.valor = valor;
        this.leiturasUsadas = leiturasUsadas;
        this.interpretacao = interpretacao;
    }

    public static ResultadoPrevisao comValor(String sensorId, String estrategia, double valor,
                                             int leiturasUsadas, String interpretacao) {
        return new ResultadoPrevisao(sensorId, estrategia, valor, leiturasUsadas, interpretacao);
    }

    /** Previsão não feita: o sistema "se cala" em vez de chutar um valor. */
    public static ResultadoPrevisao semDados(String sensorId, String estrategia,
                                             int leiturasDisponiveis, int minimoExigido) {
        String texto = String.format(PT_BR,
                "Dados insuficientes: %d leitura(s) disponível(is) e a estratégia %s exige pelo menos %d. "
                        + "Nenhuma previsão nem alerta foi emitido.",
                leiturasDisponiveis, estrategia, minimoExigido);
        return new ResultadoPrevisao(sensorId, estrategia, null, leiturasDisponiveis, texto);
    }

    public boolean temValor() { return valor != null; }

    public double getValor() {
        if (valor == null) {
            throw new IllegalStateException("A previsão não tem valor: " + interpretacao);
        }
        return valor;
    }

    public String getSensorId() { return sensorId; }
    public String getEstrategia() { return estrategia; }
    public int getLeiturasUsadas() { return leiturasUsadas; }
    public String getInterpretacao() { return interpretacao; }

    @Override
    public String toString() {
        return interpretacao;
    }
}

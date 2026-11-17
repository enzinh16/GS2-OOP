package dominio;

import excecao.DadosInvalidosException;
import java.util.List;
import java.util.Locale;

/**
 * Compara a média das últimas N leituras com a das N anteriores e projeta a mesma
 * variação para o próximo período. Reage rápido a tendências, mas exagera picos isolados.
 */
public class PrevisaoComparacao extends EstrategiaPrevisao {
    private final int janela;

    public PrevisaoComparacao(int janela) throws DadosInvalidosException {
        super("Comparação entre períodos", 2 * janela);
        this.janela = janela;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) throws DadosInvalidosException {
        List<Medicao> ordenado = prepararHistorico(historico);
        String sensorId = ordenado.isEmpty() ? null : ordenado.get(0).getSensorId();
        if (ordenado.size() < getMinimoLeituras()) {
            return ResultadoPrevisao.semDados(sensorId, getNome(), ordenado.size(), getMinimoLeituras());
        }
        int total = ordenado.size();
        double mediaAtual = media(ordenado.subList(total - janela, total));
        double mediaAnterior = media(ordenado.subList(total - 2 * janela, total - janela));
        double variacao = mediaAtual - mediaAnterior;
        double previsto = Math.max(0, mediaAtual + variacao); // grandezas físicas não são negativas
        String unidade = ordenado.get(total - 1).getUnidade();

        String tendencia = variacao > 0 ? "em alta" : variacao < 0 ? "em queda" : "estável";
        String texto = String.format(Locale.forLanguageTag("pt-BR"),
                "Médias de %s: anterior %.1f, atual %.1f %s (tendência %s). Projeção para o próximo período: %.1f %s.",
                sensorId, mediaAnterior, mediaAtual, unidade, tendencia, previsto, unidade);
        return ResultadoPrevisao.comValor(sensorId, getNome(), previsto, 2 * janela, texto);
    }

    @Override
    public String descrever() {
        return super.descrever() + " - dois blocos de " + janela + " leituras";
    }

    public int getJanela() { return janela; }
}

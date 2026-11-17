package dominio;

import excecao.DadosInvalidosException;
import java.util.List;
import java.util.Locale;

/** Previsão = média das últimas N leituras. Estável, mas reage devagar a picos. */
public class PrevisaoMediaMovel extends EstrategiaPrevisao {
    private final int janela;

    public PrevisaoMediaMovel(int janela) throws DadosInvalidosException {
        super("Média móvel", janela);
        this.janela = janela;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) throws DadosInvalidosException {
        List<Medicao> ordenado = prepararHistorico(historico);
        String sensorId = ordenado.isEmpty() ? null : ordenado.get(0).getSensorId();
        if (ordenado.size() < getMinimoLeituras()) {
            return ResultadoPrevisao.semDados(sensorId, getNome(), ordenado.size(), getMinimoLeituras());
        }
        List<Medicao> recentes = ordenado.subList(ordenado.size() - janela, ordenado.size());
        double previsto = media(recentes);
        String unidade = recentes.get(recentes.size() - 1).getUnidade();
        String texto = String.format(Locale.forLanguageTag("pt-BR"),
                "Média das últimas %d leituras de %s: %.1f %s. Indica o nível recente, sem projetar tendência.",
                janela, sensorId, previsto, unidade);
        return ResultadoPrevisao.comValor(sensorId, getNome(), previsto, janela, texto);
    }

    @Override
    public String descrever() {
        return super.descrever() + " - média das últimas " + janela + " leituras";
    }

    public int getJanela() { return janela; }
}

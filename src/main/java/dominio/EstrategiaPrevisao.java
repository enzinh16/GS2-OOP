package dominio;

import excecao.DadosInvalidosException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Contrato das estratégias de previsão. Cada filha sobrescreve prever(). */
public abstract class EstrategiaPrevisao {
    private final String nome;
    private final int minimoLeituras;

    protected EstrategiaPrevisao(String nome, int minimoLeituras) throws DadosInvalidosException {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("A estratégia de previsão precisa de um nome.");
        }
        if (minimoLeituras < 1) {
            throw new DadosInvalidosException(
                    "O mínimo de leituras deve ser pelo menos 1; valor recebido: " + minimoLeituras);
        }
        this.nome = nome;
        this.minimoLeituras = minimoLeituras;
    }

    /** Prevê o próximo valor a partir do histórico (de um único sensor, em qualquer ordem). */
    public abstract ResultadoPrevisao prever(List<Medicao> historico) throws DadosInvalidosException;

    public String descrever() {
        return nome + " (mínimo de " + minimoLeituras + " leituras)";
    }

    /**
     * Valida o histórico e devolve uma cópia em ordem cronológica.
     * Histórico nulo, com elemento nulo ou misturando sensores é erro de quem chamou.
     */
    protected List<Medicao> prepararHistorico(List<Medicao> historico) throws DadosInvalidosException {
        if (historico == null) {
            throw new DadosInvalidosException("O histórico de medições não pode ser nulo.");
        }
        List<Medicao> ordenado = new ArrayList<>(historico);
        String sensorId = null;
        for (Medicao medicao : ordenado) {
            if (medicao == null) {
                throw new DadosInvalidosException("O histórico contém uma medição nula.");
            }
            if (sensorId == null) {
                sensorId = medicao.getSensorId();
            } else if (!sensorId.equals(medicao.getSensorId())) {
                throw new DadosInvalidosException(
                        "O histórico mistura sensores: " + sensorId + " e " + medicao.getSensorId());
            }
        }
        ordenado.sort(Comparator.comparing(Medicao::getDataHora).thenComparingLong(Medicao::getId));
        return ordenado;
    }

    protected static double media(List<Medicao> medicoes) {
        double soma = 0;
        for (Medicao medicao : medicoes) {
            soma += medicao.getValor();
        }
        return soma / medicoes.size();
    }

    public String getNome() { return nome; }
    public int getMinimoLeituras() { return minimoLeituras; }
}

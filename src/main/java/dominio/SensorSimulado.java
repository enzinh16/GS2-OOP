package dominio;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import java.util.Random;

/** Base dos sensores simulados: cada filha só define como gerar o valor. */
public abstract class SensorSimulado extends ComponenteTerritorial implements Sensor {
    private final double faixaMin;
    private final double faixaMax;
    protected final Random random;
    private boolean emFalha = false;
    private Double leituraForcada = null;

    protected SensorSimulado(String id, String nome, String regiao,
                             double faixaMin, double faixaMax, Random random) {
        super(id, nome, regiao);
        this.faixaMin = faixaMin;
        this.faixaMax = faixaMax;
        this.random = random;
    }

    /** Gera o valor simulado, sem validar. */
    protected abstract double gerarValor();

    @Override
    public double lerValor() throws FonteIndisponivelException, DadosInvalidosException {
        if (emFalha) {
            throw new FonteIndisponivelException(getId(),
                    "O sensor " + getId() + " não está respondendo.");
        }
        double valor;
        if (leituraForcada != null) {
            valor = leituraForcada;
            leituraForcada = null;
        } else {
            valor = gerarValor();
        }
        if (Double.isNaN(valor) || valor < faixaMin || valor > faixaMax) {
            throw new DadosInvalidosException(String.format(
                    "Leitura %.1f %s fora da faixa válida (%.0f a %.0f) no sensor %s.",
                    valor, getUnidade(), faixaMin, faixaMax, getId()));
        }
        return valor;
    }

    @Override
    public boolean verificarFuncionamento() {
        return !emFalha;
    }

    @Override
    public StatusComponente verificarStatus() {
        return emFalha ? StatusComponente.INDISPONIVEL : StatusComponente.OPERACIONAL;
    }

    /** Liga ou desliga a falha simulada do sensor. */
    public void simularFalha(boolean falha) {
        this.emFalha = falha;
    }

    /** A próxima leitura devolve exatamente este valor (útil em testes e demonstrações). */
    public void forcarProximaLeitura(double valor) {
        this.leituraForcada = valor;
    }

    public double getFaixaMin() { return faixaMin; }
    public double getFaixaMax() { return faixaMax; }
}

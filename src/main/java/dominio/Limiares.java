package dominio;

import excecao.DadosInvalidosException;
import java.util.Locale;

/** Limites de ATENÇÃO, ALERTA e CRÍTICO de um tipo de sensor. Imutável e sempre válido. */
public final class Limiares {
    private final double atencao;
    private final double alerta;
    private final double critico;

    public Limiares(double atencao, double alerta, double critico) throws DadosInvalidosException {
        if (!Double.isFinite(atencao) || !Double.isFinite(alerta) || !Double.isFinite(critico)) {
            throw new DadosInvalidosException("Os limiares devem ser números válidos.");
        }
        if (atencao <= 0) {
            throw new DadosInvalidosException(String.format(Locale.forLanguageTag("pt-BR"),
                    "O limiar de ATENÇÃO deve ser positivo; valor recebido: %.1f", atencao));
        }
        if (!(atencao < alerta && alerta < critico)) {
            throw new DadosInvalidosException(String.format(Locale.forLanguageTag("pt-BR"),
                    "Os limiares devem crescer (atenção < alerta < crítico); recebido: %.1f, %.1f, %.1f",
                    atencao, alerta, critico));
        }
        this.atencao = atencao;
        this.alerta = alerta;
        this.critico = critico;
    }

    public NivelAlerta classificar(double valor) throws DadosInvalidosException {
        if (Double.isNaN(valor)) {
            throw new DadosInvalidosException("Não é possível classificar um valor inválido (NaN).");
        }
        if (valor >= critico) return NivelAlerta.CRITICO;
        if (valor >= alerta) return NivelAlerta.ALERTA;
        if (valor >= atencao) return NivelAlerta.ATENCAO;
        return NivelAlerta.NORMAL;
    }

    /** Valor a partir do qual o nível é atingido (0 para NORMAL). */
    public double limitePara(NivelAlerta nivel) {
        return switch (nivel) {
            case CRITICO -> critico;
            case ALERTA -> alerta;
            case ATENCAO -> atencao;
            case NORMAL -> 0.0;
        };
    }

    public double getAtencao() { return atencao; }
    public double getAlerta() { return alerta; }
    public double getCritico() { return critico; }
}

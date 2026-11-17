package servico;

import dominio.Alerta;
import dominio.ResultadoPrevisao;
import java.util.Optional;

/** O que o menu mostra após uma previsão: o resultado e, se houve, o alerta emitido. */
public final class AvaliacaoPrevisao {
    private final ResultadoPrevisao resultado;
    private final Alerta alerta; // null quando não há alerta

    public AvaliacaoPrevisao(ResultadoPrevisao resultado, Alerta alerta) {
        if (resultado == null) {
            throw new IllegalArgumentException("O resultado da previsão é obrigatório.");
        }
        this.resultado = resultado;
        this.alerta = alerta;
    }

    public ResultadoPrevisao getResultado() { return resultado; }

    public Optional<Alerta> getAlerta() { return Optional.ofNullable(alerta); }
}

package servico;

import dominio.EstrategiaPrevisao;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory das estratégias de previsão. É a única classe que conhece as estratégias
 * concretas: quem pede uma estratégia trabalha só com EstrategiaPrevisao, sem if/instanceof.
 */
public class FabricaPrevisao {
    public static final int JANELA_MEDIA_MOVEL_PADRAO = 5;
    public static final int JANELA_COMPARACAO_PADRAO = 3;

    @FunctionalInterface
    private interface Criador {
        EstrategiaPrevisao criar() throws DadosInvalidosException;
    }

    private final Map<TipoPrevisao, Criador> criadores = new EnumMap<>(TipoPrevisao.class);

    public FabricaPrevisao() {
        this(JANELA_MEDIA_MOVEL_PADRAO, JANELA_COMPARACAO_PADRAO);
    }

    public FabricaPrevisao(int janelaMediaMovel, int janelaComparacao) {
        criadores.put(TipoPrevisao.MEDIA_MOVEL, () -> new PrevisaoMediaMovel(janelaMediaMovel));
        criadores.put(TipoPrevisao.COMPARACAO, () -> new PrevisaoComparacao(janelaComparacao));
    }

    public EstrategiaPrevisao criar(TipoPrevisao tipo) throws DadosInvalidosException {
        if (tipo == null) {
            throw new DadosInvalidosException("O tipo de previsão é obrigatório.");
        }
        Criador criador = criadores.get(tipo);
        if (criador == null) {
            throw new DadosInvalidosException("Tipo de previsão sem estratégia registrada: " + tipo);
        }
        return criador.criar();
    }

    /** Uma instância de cada estratégia registrada, na ordem do enum. */
    public List<EstrategiaPrevisao> criarTodas() throws DadosInvalidosException {
        List<EstrategiaPrevisao> todas = new ArrayList<>();
        for (TipoPrevisao tipo : TipoPrevisao.values()) {
            todas.add(criar(tipo));
        }
        return todas;
    }
}

package servico;

public enum TipoPrevisao {
    MEDIA_MOVEL("Média móvel"),
    COMPARACAO("Comparação entre períodos");

    private final String rotulo;

    TipoPrevisao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() { return rotulo; }
}

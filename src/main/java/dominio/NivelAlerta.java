package dominio;

public enum NivelAlerta {
    NORMAL("NORMAL", "Nenhuma ação necessária."),
    ATENCAO("ATENÇÃO", "Sugestão: acompanhar a evolução das leituras."),
    ALERTA("ALERTA", "Sugestão: reforçar a vigilância e preparar o aviso à população."),
    CRITICO("CRÍTICO", "Sugestão: avisar quem está em área de risco e avaliar o plano de contingência.");

    private final String rotulo;
    private final String orientacao;

    NivelAlerta(String rotulo, String orientacao) {
        this.rotulo = rotulo;
        this.orientacao = orientacao;
    }

    public String getRotulo() { return rotulo; }
    public String getOrientacao() { return orientacao; }
}

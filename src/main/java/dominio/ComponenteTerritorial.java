package dominio;

/** Qualquer elemento físico monitorado no território (sensor, ponto, estação). */
public abstract class ComponenteTerritorial {
    private final String id;
    private final String nome;
    private final String regiao;

    protected ComponenteTerritorial(String id, String nome, String regiao) {
        this.id = id;
        this.nome = nome;
        this.regiao = regiao;
    }

    public abstract StatusComponente verificarStatus();

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getRegiao() { return regiao; }
}

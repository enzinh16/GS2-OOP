package excecao;

/** Lançada quando uma fonte de dados (sensor, gerador, banco) não responde. */
public class FonteIndisponivelException extends Exception {
    private final String fonte;

    public FonteIndisponivelException(String fonte, String mensagem) {
        super(mensagem);
        this.fonte = fonte;
    }

    public FonteIndisponivelException(String fonte, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.fonte = fonte;
    }

    public String getFonte() {
        return fonte;
    }
}

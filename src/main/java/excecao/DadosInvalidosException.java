package excecao;

/** Lançada quando um dado, valor ou limiar não respeita as regras do sistema. */
public class DadosInvalidosException extends Exception {
    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}

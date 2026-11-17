package dominio;

import excecao.DadosInvalidosException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Dados do território monitorado e limiares de alerta de cada tipo de sensor. */
public class DadosTerritorio {
    private String nome;
    private String regiao;
    private final String codigoAcesso;
    private final Map<String, Limiares> limiares = new LinkedHashMap<>();

    public DadosTerritorio(String nome, String regiao, String codigoAcesso)
            throws DadosInvalidosException {
        setNome(nome);
        setRegiao(regiao);
        if (codigoAcesso == null || codigoAcesso.isBlank()) {
            throw new DadosInvalidosException("O código de acesso do território é obrigatório.");
        }
        this.codigoAcesso = codigoAcesso;
        // Limiares iniciais (parâmetros de simulação, ajustáveis pelo menu).
        limiares.put(SensorPluviometro.TIPO, new Limiares(20, 40, 70));   // mm/h
        limiares.put(SensorNivelCorrego.TIPO, new Limiares(90, 120, 150)); // cm
        limiares.put(SensorUmidadeSolo.TIPO, new Limiares(70, 80, 90));   // %
    }

    public String getNome() { return nome; }
    public String getRegiao() { return regiao; }

    public void setNome(String nome) throws DadosInvalidosException {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("O nome do território é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public void setRegiao(String regiao) throws DadosInvalidosException {
        if (regiao == null || regiao.isBlank()) {
            throw new DadosInvalidosException("A região do território é obrigatória.");
        }
        this.regiao = regiao.trim();
    }

    public Set<String> getTiposMonitorados() {
        return Collections.unmodifiableSet(limiares.keySet());
    }

    public Limiares getLimiares(String tipo) throws DadosInvalidosException {
        Limiares encontrado = limiares.get(tipo);
        if (encontrado == null) {
            throw new DadosInvalidosException("Tipo de sensor sem limiares configurados: " + tipo);
        }
        return encontrado;
    }

    /** Altera os limiares de um tipo. Exige o código de acesso; se algo for inválido, nada muda. */
    public void setLimiares(String codigo, String tipo, double atencao, double alerta, double critico)
            throws DadosInvalidosException {
        if (!codigoConfere(codigo)) {
            throw new DadosInvalidosException("Código de acesso incorreto: limiares não alterados.");
        }
        getLimiares(tipo); // confirma que o tipo existe
        limiares.put(tipo, new Limiares(atencao, alerta, critico));
    }

    private boolean codigoConfere(String codigo) {
        if (codigo == null) {
            return false;
        }
        return MessageDigest.isEqual(codigo.getBytes(StandardCharsets.UTF_8),
                codigoAcesso.getBytes(StandardCharsets.UTF_8));
    }

    public NivelAlerta classificar(String tipo, double valor) throws DadosInvalidosException {
        return getLimiares(tipo).classificar(valor);
    }
}

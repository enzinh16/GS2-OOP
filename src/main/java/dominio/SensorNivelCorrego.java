package dominio;

import java.util.Random;

public class SensorNivelCorrego extends SensorSimulado {
    public static final String TIPO = "nivel";

    public SensorNivelCorrego(String id, String regiao, Random random) {
        super(id, "Nível do córrego", regiao, 0, 400, random);
    }

    @Override
    protected double gerarValor() {
        return 40 + random.nextDouble() * 120; // 40 a 160 cm
    }

    @Override public String getTipo() { return TIPO; }
    @Override public String getUnidade() { return "cm"; }
}

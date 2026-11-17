package dominio;

import java.util.Random;

public class SensorUmidadeSolo extends SensorSimulado {
    public static final String TIPO = "umidade";

    public SensorUmidadeSolo(String id, String regiao, Random random) {
        super(id, "Umidade do solo", regiao, 0, 100, random);
    }

    @Override
    protected double gerarValor() {
        return 30 + random.nextDouble() * 65; // 30 a 95 %
    }

    @Override public String getTipo() { return TIPO; }
    @Override public String getUnidade() { return "%"; }
}

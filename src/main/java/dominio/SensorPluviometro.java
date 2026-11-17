package dominio;

import java.util.Random;

public class SensorPluviometro extends SensorSimulado {
    public static final String TIPO = "chuva";

    public SensorPluviometro(String id, String regiao, Random random) {
        super(id, "Pluviômetro", regiao, 0, 300, random);
    }

    @Override
    protected double gerarValor() {
        // Metade das leituras com chuva fraca (0 a 5 mm/h), metade até 90 mm/h.
        return random.nextDouble() < 0.5 ? random.nextDouble() * 5 : random.nextDouble() * 90;
    }

    @Override public String getTipo() { return TIPO; }
    @Override public String getUnidade() { return "mm/h"; }
}

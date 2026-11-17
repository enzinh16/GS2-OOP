import dominio.DadosTerritorio;
import dominio.Limiares;
import dominio.Medicao;
import dominio.Sensor;
import dominio.SensorNivelCorrego;
import dominio.SensorPluviometro;
import dominio.SensorUmidadeSolo;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class SistemaMonitoramento {

    public static void main(String[] args) {
        try {
            DadosTerritorio territorio = new DadosTerritorio("Córrego da Várzea", "Zona Leste", "coord2026");
            Random random = new Random();
            List<Sensor> sensores = List.of(
                    new SensorPluviometro("PLU-01", territorio.getRegiao(), random),
                    new SensorNivelCorrego("NIV-01", territorio.getRegiao(), random),
                    new SensorUmidadeSolo("UMI-01", territorio.getRegiao(), random));
            executarMenu(territorio, sensores);
        } catch (DadosInvalidosException e) {
            System.out.println("Erro ao iniciar o sistema: " + e.getMessage());
        }
    }

    private static void executarMenu(DadosTerritorio territorio, List<Sensor> sensores) {
        Scanner scanner = new Scanner(System.in);
        int opcao;
        do {
            System.out.println("\n=== Territory Predictive Insights ===");
            System.out.println("Território: " + territorio.getNome() + " (" + territorio.getRegiao() + ")");
            System.out.println("1 - Ler sensores");
            System.out.println("2 - Ver limiares de alerta");
            System.out.println("0 - Sair");
            System.out.print("Opção: ");
            opcao = lerInteiro(scanner);

            switch (opcao) {
                case 1 -> lerSensores(sensores);
                case 2 -> mostrarLimiares(territorio);
                case 0 -> System.out.println("Até logo!");
                default -> System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    private static void lerSensores(List<Sensor> sensores) {
        for (Sensor sensor : sensores) {
            try {
                Medicao medicao = sensor.lerMedicao(LocalDateTime.now());
                System.out.println(medicao);
            } catch (FonteIndisponivelException e) {
                System.out.println("Fonte indisponível: " + e.getMessage());
            } catch (DadosInvalidosException e) {
                System.out.println("Dado inválido: " + e.getMessage());
            }
        }
    }

    private static void mostrarLimiares(DadosTerritorio territorio) {
        for (String tipo : territorio.getTiposMonitorados()) {
            try {
                Limiares l = territorio.getLimiares(tipo);
                System.out.printf("%-8s atenção >= %.1f | alerta >= %.1f | crítico >= %.1f%n",
                        tipo, l.getAtencao(), l.getAlerta(), l.getCritico());
            } catch (DadosInvalidosException e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private static int lerInteiro(Scanner scanner) {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

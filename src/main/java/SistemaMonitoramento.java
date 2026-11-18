import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.Limiares;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.SensorNivelCorrego;
import dominio.SensorPluviometro;
import dominio.SensorSimulado;
import dominio.SensorUmidadeSolo;
import dominio.StatusComponente;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import persistencia.AlertaDAO;
import persistencia.FabricaConexao;
import persistencia.MedicaoDAO;
import servico.AvaliacaoPrevisao;
import servico.ExportadorCSV;
import servico.FabricaPrevisao;
import servico.ServicoMonitoramento;
import servico.TipoPrevisao;

/** Menu principal (console). Só conversa com o usuário: a lógica fica no ServicoMonitoramento. */
public class SistemaMonitoramento {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int LIMITE_LISTAGEM = 30;
    private static final int MAX_RODADAS = 100;
    private static final int LIMITE_DETALHE = 10; // acima disso, a opção 1 mostra só o resumo
    private static final String CODIGO_ACESSO_PADRAO = "coord2026";

    private final Scanner scanner;
    private final DadosTerritorio territorio;
    private final List<SensorSimulado> sensores;
    private final MedicaoDAO medicaoDAO;
    private final AlertaDAO alertaDAO;
    private final ServicoMonitoramento servico;
    private final ExportadorCSV exportador = new ExportadorCSV();

    SistemaMonitoramento(Scanner scanner, DadosTerritorio territorio, List<SensorSimulado> sensores,
                         MedicaoDAO medicaoDAO, AlertaDAO alertaDAO, ServicoMonitoramento servico) {
        this.scanner = scanner;
        this.territorio = territorio;
        this.sensores = sensores;
        this.medicaoDAO = medicaoDAO;
        this.alertaDAO = alertaDAO;
        this.servico = servico;
    }

    public static void main(String[] args) {
        try {
            DadosTerritorio territorio = new DadosTerritorio("Córrego da Várzea", "Zona Leste", CODIGO_ACESSO_PADRAO);
            Random random = new Random();
            List<SensorSimulado> sensores = List.of(
                    new SensorPluviometro("PLU-01", territorio.getRegiao(), random),
                    new SensorNivelCorrego("NIV-01", territorio.getRegiao(), random),
                    new SensorUmidadeSolo("UMI-01", territorio.getRegiao(), random));

            FabricaConexao conexao = new FabricaConexao();
            MedicaoDAO medicaoDAO = new MedicaoDAO(conexao);
            AlertaDAO alertaDAO = new AlertaDAO(conexao);
            ServicoMonitoramento servico = new ServicoMonitoramento(medicaoDAO, alertaDAO,
                    new FabricaPrevisao(), territorio, Clock.systemDefaultZone());

            new SistemaMonitoramento(new Scanner(System.in), territorio, sensores,
                    medicaoDAO, alertaDAO, servico).executar();
        } catch (DadosInvalidosException e) {
            System.out.println("Erro ao iniciar o sistema: " + e.getMessage());
        } catch (FonteIndisponivelException e) {
            System.out.println("Não foi possível abrir o banco de dados (" + e.getFonte() + "): " + e.getMessage());
            System.out.println("Verifique se outra cópia do programa já está aberta e tente novamente.");
        }
    }

    void executar() {
        int opcao;
        do {
            exibirMenu();
            opcao = lerInteiro("Opção: ");
            switch (opcao) {
                case 1 -> verificarSensoresVariasVezes();
                case 2 -> rodarPrevisoes();
                case 3 -> verHistorico();
                case 4 -> verAlertas();
                case 5 -> gerenciarTerritorio();
                case 6 -> statusCompleto();
                case 7 -> simulacoes();
                case 8 -> exportarHistorico();
                case 0 -> System.out.println("Até logo! Os dados ficaram gravados no banco.");
                default -> System.out.println("Opção inválida. Escolha um número do menu.");
            }
        } while (opcao != 0);
    }

    private void exibirMenu() {
        System.out.println("\n=== Territory Predictive Insights ===");
        System.out.println("Território: " + territorio.getNome() + " (" + territorio.getRegiao() + ")");
        System.out.println("1 - Verificar sensores (ler, validar e gravar)");
        System.out.println("2 - Rodar previsões");
        System.out.println("3 - Ver histórico de um sensor");
        System.out.println("4 - Ver alertas emitidos");
        System.out.println("5 - Gerenciar dados do território");
        System.out.println("6 - Status completo");
        System.out.println("7 - Simulações (falha de sensor e leitura inválida)");
        System.out.println("8 - Exportar histórico para CSV");
        System.out.println("0 - Sair");
    }

    // ---------------------------------------------------------------- 1. verificar sensores

    /** Pergunta quantas leituras de cada sensor fazer e repete a rodada esse número de vezes. */
    private void verificarSensoresVariasVezes() {
        int rodadas = lerInteiro("Quantas leituras de cada sensor? (1 a " + MAX_RODADAS + "): ");
        if (rodadas < 1 || rodadas > MAX_RODADAS) {
            System.out.println("Quantidade inválida. Informe um número de 1 a " + MAX_RODADAS + ".");
            return;
        }
        boolean detalhar = rodadas <= LIMITE_DETALHE;
        int total = 0;
        for (int i = 1; i <= rodadas; i++) {
            if (detalhar && rodadas > 1) {
                System.out.println("\n-- Leitura " + i + " de " + rodadas + " --");
            }
            total += verificarSensores(detalhar);
        }
        System.out.println("\n" + total + " medição(ões) gravada(s) em " + rodadas + " rodada(s).");
        if (!detalhar) {
            System.out.println("(Com mais de " + LIMITE_DETALHE + " leituras, o detalhe de cada uma não é exibido. "
                    + "Use a opção 3 para ver o histórico.)");
        }
    }

    /** Lê e grava uma rodada de leituras. Devolve quantas foram gravadas; erros de um sensor não param os outros. */
    private int verificarSensores(boolean detalhar) {
        int gravadas = 0;
        for (SensorSimulado sensor : sensores) {
            try {
                Medicao medicao = servico.registrarLeitura(sensor);
                gravadas++;
                if (detalhar) {
                    NivelAlerta nivel = territorio.classificar(sensor.getTipo(), medicao.getValor());
                    System.out.println("Gravada: " + medicao + " | nível atual: " + nivel.getRotulo());
                }
            } catch (FonteIndisponivelException e) {
                System.out.println("Fonte indisponível (" + e.getFonte() + "): " + e.getMessage()
                        + " Nada foi gravado para " + sensor.getId() + ".");
            } catch (DadosInvalidosException e) {
                System.out.println("Leitura descartada, dado inválido: " + e.getMessage()
                        + " Nada foi gravado para " + sensor.getId() + ".");
            }
        }
        return gravadas;
    }

    // ---------------------------------------------------------------- 2. previsões

    private void rodarPrevisoes() {
        for (SensorSimulado sensor : sensores) {
            for (TipoPrevisao tipo : TipoPrevisao.values()) {
                System.out.println("\n[" + sensor.getId() + " | " + tipo.getRotulo() + "]");
                try {
                    AvaliacaoPrevisao avaliacao = servico.preverEAvaliar(sensor, tipo);
                    System.out.println(avaliacao.getResultado().getInterpretacao());
                    if (avaliacao.getAlerta().isPresent()) {
                        System.out.println(">> " + avaliacao.getAlerta().get() + " (alerta gravado)");
                    } else if (avaliacao.getResultado().temValor()) {
                        System.out.println("Sem alerta: a previsão está abaixo do limite de atenção.");
                    }
                } catch (FonteIndisponivelException e) {
                    System.out.println("Fonte indisponível (" + e.getFonte() + "): " + e.getMessage());
                } catch (DadosInvalidosException e) {
                    System.out.println("Dado inválido: " + e.getMessage());
                }
            }
        }
    }

    // ---------------------------------------------------------------- 3. histórico e 4. alertas

    private void verHistorico() {
        SensorSimulado sensor = escolherSensor();
        if (sensor == null) {
            return;
        }
        try {
            List<Medicao> historico = medicaoDAO.buscarHistoricoPorSensor(sensor.getId());
            if (historico.isEmpty()) {
                System.out.println("Nenhuma medição gravada para " + sensor.getId() + " ainda.");
                return;
            }
            int inicio = Math.max(0, historico.size() - LIMITE_LISTAGEM);
            System.out.println("Histórico de " + sensor.getId() + " (" + historico.size() + " medições"
                    + (inicio > 0 ? "; mostrando as " + LIMITE_LISTAGEM + " mais recentes" : "") + "):");
            for (Medicao medicao : historico.subList(inicio, historico.size())) {
                System.out.println("  " + medicao);
            }
        } catch (FonteIndisponivelException e) {
            System.out.println("Fonte indisponível (" + e.getFonte() + "): " + e.getMessage());
        }
    }

    private void verAlertas() {
        try {
            List<Alerta> alertas = alertaDAO.listarAlertas();
            if (alertas.isEmpty()) {
                System.out.println("Nenhum alerta emitido até agora.");
                return;
            }
            System.out.println("Alertas emitidos (" + alertas.size() + "), do mais recente ao mais antigo:");
            for (Alerta alerta : alertas) {
                System.out.println("  " + alerta.getDataHora().format(FORMATO_DATA) + " " + alerta);
            }
        } catch (FonteIndisponivelException e) {
            System.out.println("Fonte indisponível (" + e.getFonte() + "): " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 5. território

    private void gerenciarTerritorio() {
        int opcao;
        do {
            System.out.println("\n--- Dados do território ---");
            System.out.println("1 - Ver dados e limiares");
            System.out.println("2 - Alterar nome");
            System.out.println("3 - Alterar região");
            System.out.println("4 - Alterar limiares (exige código de acesso)");
            System.out.println("0 - Voltar");
            opcao = lerInteiro("Opção: ");
            switch (opcao) {
                case 1 -> mostrarTerritorio();
                case 2 -> alterarNome();
                case 3 -> alterarRegiao();
                case 4 -> alterarLimiares();
                case 0 -> { }
                default -> System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    private void mostrarTerritorio() {
        System.out.println("Nome: " + territorio.getNome());
        System.out.println("Região: " + territorio.getRegiao());
        for (String tipo : territorio.getTiposMonitorados()) {
            try {
                System.out.println("  " + descreverLimiares(tipo, territorio.getLimiares(tipo)));
            } catch (DadosInvalidosException e) {
                System.out.println("  Erro ao ler os limiares de " + tipo + ": " + e.getMessage());
            }
        }
    }

    private String descreverLimiares(String tipo, Limiares l) {
        return String.format(PT_BR, "%-8s atenção >= %.1f | alerta >= %.1f | crítico >= %.1f",
                tipo, l.getAtencao(), l.getAlerta(), l.getCritico());
    }

    private void alterarNome() {
        try {
            territorio.setNome(lerTexto("Novo nome do território: "));
            System.out.println("Nome atualizado para: " + territorio.getNome());
        } catch (DadosInvalidosException e) {
            System.out.println("Nome não alterado: " + e.getMessage());
        }
    }

    private void alterarRegiao() {
        try {
            territorio.setRegiao(lerTexto("Nova região: "));
            System.out.println("Região atualizada para: " + territorio.getRegiao());
        } catch (DadosInvalidosException e) {
            System.out.println("Região não alterada: " + e.getMessage());
        }
    }

    private void alterarLimiares() {
        List<String> tipos = new ArrayList<>(territorio.getTiposMonitorados());
        System.out.println("Tipo de sensor:");
        for (int i = 0; i < tipos.size(); i++) {
            System.out.println("  " + (i + 1) + " - " + tipos.get(i));
        }
        int escolha = lerInteiro("Escolha: ");
        if (escolha < 1 || escolha > tipos.size()) {
            System.out.println("Tipo inválido. Nada foi alterado.");
            return;
        }
        String tipo = tipos.get(escolha - 1);
        String codigo = lerTexto("Código de acesso: ");
        Double atencao = lerNumero("Novo limiar de ATENÇÃO: ");
        Double alerta = lerNumero("Novo limiar de ALERTA: ");
        Double critico = lerNumero("Novo limiar CRÍTICO: ");
        if (atencao == null || alerta == null || critico == null) {
            System.out.println("Número inválido. Limiares não alterados.");
            return;
        }
        try {
            territorio.setLimiares(codigo, tipo, atencao, alerta, critico);
            System.out.println("Limiares atualizados: " + descreverLimiares(tipo, territorio.getLimiares(tipo)));
        } catch (DadosInvalidosException e) {
            System.out.println("Limiares não alterados: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 6. status completo

    private void statusCompleto() {
        System.out.println("\n=== Status completo ===");
        System.out.println("Território: " + territorio.getNome() + " (" + territorio.getRegiao() + ")");
        for (SensorSimulado sensor : sensores) {
            System.out.println("\nSensor " + sensor.getId() + " - " + sensor.getNome()
                    + " [" + sensor.getTipo() + ", " + sensor.getUnidade() + "]");
            System.out.println("  Status: " + sensor.verificarStatus()
                    + " | funcionamento: " + (sensor.verificarFuncionamento() ? "OK" : "em falha"));
            try {
                List<Medicao> historico = medicaoDAO.buscarHistoricoPorSensor(sensor.getId());
                System.out.println("  Leituras gravadas: " + historico.size());
                if (!historico.isEmpty()) {
                    Medicao ultima = historico.get(historico.size() - 1);
                    NivelAlerta nivel = territorio.classificar(sensor.getTipo(), ultima.getValor());
                    System.out.println("  Última leitura: " + ultima + " | nível: " + nivel.getRotulo());
                }
                System.out.println("  Limiares: " + descreverLimiares(sensor.getTipo(),
                        territorio.getLimiares(sensor.getTipo())));
            } catch (FonteIndisponivelException e) {
                System.out.println("  Fonte indisponível (" + e.getFonte() + "): " + e.getMessage());
            } catch (DadosInvalidosException e) {
                System.out.println("  Dado inválido: " + e.getMessage());
            }
        }
        resumirAlertas();
    }

    private void resumirAlertas() {
        try {
            List<Alerta> alertas = alertaDAO.listarAlertas();
            Map<NivelAlerta, Integer> porNivel = new EnumMap<>(NivelAlerta.class);
            for (Alerta alerta : alertas) {
                porNivel.merge(alerta.getNivel(), 1, Integer::sum);
            }
            StringBuilder detalhe = new StringBuilder();
            for (NivelAlerta nivel : NivelAlerta.values()) {
                if (porNivel.containsKey(nivel)) {
                    detalhe.append(detalhe.length() == 0 ? "" : ", ")
                            .append(nivel.getRotulo()).append(": ").append(porNivel.get(nivel));
                }
            }
            System.out.println("\nAlertas emitidos: " + alertas.size()
                    + (detalhe.length() > 0 ? " (" + detalhe + ")" : ""));
        } catch (FonteIndisponivelException e) {
            System.out.println("\nAlertas: fonte indisponível (" + e.getFonte() + "): " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 7. exportar CSV (bônus)

    private void exportarHistorico() {
        SensorSimulado sensor = escolherSensor();
        if (sensor == null) {
            return;
        }
        try {
            List<Medicao> historico = medicaoDAO.buscarHistoricoPorSensor(sensor.getId());
            if (historico.isEmpty()) {
                System.out.println("Nenhuma medição para exportar de " + sensor.getId() + ".");
                return;
            }
            Path destino = Path.of("dados", "historico-" + sensor.getId() + ".csv");
            Files.createDirectories(destino.toAbsolutePath().getParent());
            exportador.exportar(historico, destino);
            System.out.println(historico.size() + " medições exportadas para " + destino.toAbsolutePath());
        } catch (FonteIndisponivelException e) {
            System.out.println("Fonte indisponível (" + e.getFonte() + "): " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Não foi possível criar a pasta de exportação: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 8. simulações (demonstração)

    private void simulacoes() {
        System.out.println("\n--- Simulações ---");
        System.out.println("1 - Ligar/desligar falha de um sensor");
        System.out.println("2 - Forçar a próxima leitura de um sensor (ex.: 999 para leitura inválida)");
        System.out.println("0 - Voltar");
        int opcao = lerInteiro("Opção: ");
        switch (opcao) {
            case 1 -> alternarFalha();
            case 2 -> forcarLeitura();
            case 0 -> { }
            default -> System.out.println("Opção inválida.");
        }
    }

    private void alternarFalha() {
        SensorSimulado sensor = escolherSensor(true);
        if (sensor == null) {
            return;
        }
        boolean novaFalha = sensor.verificarFuncionamento();
        sensor.simularFalha(novaFalha);
        System.out.println("Sensor " + sensor.getId() + (novaFalha ? " agora está em falha." : " voltou a funcionar."));
    }

    private void forcarLeitura() {
        SensorSimulado sensor = escolherSensor();
        if (sensor == null) {
            return;
        }
        Double valor = lerNumero("Valor da próxima leitura (" + sensor.getUnidade() + "): ");
        if (valor == null) {
            System.out.println("Número inválido. Nada foi alterado.");
            return;
        }
        sensor.forcarProximaLeitura(valor);
        System.out.println("A próxima leitura de " + sensor.getId() + " será " + valor
                + ". Use a opção 1 do menu principal para lê-la.");
    }

    // ---------------------------------------------------------------- entrada de dados

    private SensorSimulado escolherSensor() {
        return escolherSensor(false);
    }

    /** Lista os sensores para escolha; com mostrarStatus, indica se cada um está operacional ou indisponível. */
    private SensorSimulado escolherSensor(boolean mostrarStatus) {
        System.out.println("Sensor:");
        for (int i = 0; i < sensores.size(); i++) {
            SensorSimulado s = sensores.get(i);
            String linha = "  " + (i + 1) + " - " + s.getId() + " (" + s.getNome() + ")";
            if (mostrarStatus) {
                linha += " [" + (s.verificarStatus() == StatusComponente.OPERACIONAL
                        ? "OPERACIONAL" : "INDISPONÍVEL") + "]";
            }
            System.out.println(linha);
        }
        int escolha = lerInteiro("Escolha: ");
        if (escolha < 1 || escolha > sensores.size()) {
            System.out.println("Sensor inválido.");
            return null;
        }
        return sensores.get(escolha - 1);
    }

    private String lerTexto(String pergunta) {
        System.out.print(pergunta);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    /** Lê um inteiro. Entrada inválida vira -1 (opção inválida); fim da entrada vira 0 (sair). */
    private int lerInteiro(String pergunta) {
        System.out.print(pergunta);
        if (!scanner.hasNextLine()) {
            return 0;
        }
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Lê um número (aceita vírgula ou ponto). Devolve null se o texto não for um número. */
    private Double lerNumero(String pergunta) {
        String texto = lerTexto(pergunta).replace(',', '.');
        try {
            return Double.valueOf(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

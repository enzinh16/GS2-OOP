package servico;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.EstrategiaPrevisao;
import dominio.Medicao;
import dominio.ResultadoPrevisao;
import dominio.Sensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import persistencia.AlertaDAO;
import persistencia.MedicaoDAO;

/**
 * Fica entre o menu e o banco: lê, valida e grava medições; prevê a partir do histórico
 * e grava os alertas. Tudo entra pelo construtor, o que permite testar com DAOs mockados.
 */
public class ServicoMonitoramento {
    private final MedicaoDAO medicaoDAO;
    private final AlertaDAO alertaDAO;
    private final FabricaPrevisao fabrica;
    private final DadosTerritorio territorio;
    private final Clock relogio;

    public ServicoMonitoramento(MedicaoDAO medicaoDAO, AlertaDAO alertaDAO,
                                FabricaPrevisao fabrica, DadosTerritorio territorio, Clock relogio) {
        this.medicaoDAO = Objects.requireNonNull(medicaoDAO, "medicaoDAO");
        this.alertaDAO = Objects.requireNonNull(alertaDAO, "alertaDAO");
        this.fabrica = Objects.requireNonNull(fabrica, "fabrica");
        this.territorio = Objects.requireNonNull(territorio, "territorio");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /**
     * Lê o sensor, valida e grava a medição. Se o sensor falhar ou o valor for inválido,
     * a exceção sobe e nada é gravado.
     */
    public Medicao registrarLeitura(Sensor sensor)
            throws FonteIndisponivelException, DadosInvalidosException {
        exigirSensor(sensor);
        Medicao medicao = sensor.lerMedicao(agora());
        medicaoDAO.salvar(medicao);
        return medicao;
    }

    /**
     * Busca o histórico, prevê com a estratégia pedida e avalia os limiares.
     * Sem leituras suficientes, o sistema se cala: não há previsão nem alerta gravado.
     */
    public AvaliacaoPrevisao preverEAvaliar(Sensor sensor, TipoPrevisao tipo)
            throws FonteIndisponivelException, DadosInvalidosException {
        exigirSensor(sensor);
        EstrategiaPrevisao estrategia = fabrica.criar(tipo);
        List<Medicao> historico = medicaoDAO.buscarUltimas(sensor.getId(), estrategia.getMinimoLeituras());
        ResultadoPrevisao resultado = estrategia.prever(historico);
        if (!resultado.temValor()) {
            return new AvaliacaoPrevisao(resultado, null);
        }
        Optional<Alerta> alerta = Alerta.avaliar(sensor.getId(), sensor.getTipo(), sensor.getUnidade(),
                resultado.getValor(), resultado.getEstrategia(), territorio, agora());
        if (alerta.isPresent()) {
            alertaDAO.salvar(alerta.get());
        }
        return new AvaliacaoPrevisao(resultado, alerta.orElse(null));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private static void exigirSensor(Sensor sensor) throws DadosInvalidosException {
        if (sensor == null) {
            throw new DadosInvalidosException("O sensor é obrigatório.");
        }
    }
}

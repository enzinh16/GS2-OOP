# Global Solution 2º/2026 - POO: Territory Predictive Insights

Sistema de monitoramento territorial em console, feito em Java. Ele lê sensores simulados, valida cada leitura, grava no banco, **prevê** o próximo valor a partir do histórico e emite alertas **ATENÇÃO / ALERTA / CRÍTICO** explicando o motivo.


## Recorte e público beneficiado

**Recorte escolhido: chuva intensa e risco de alagamento em pontos vulneráveis.**

Alagamento se repete todo ano em muitos bairros e quase sempre dá sinais antes: a chuva aperta, o córrego começa a subir e o solo já não absorve mais água. Esses sinais estão em medidas diferentes, e é isso que o sistema acompanha.

**Quem sofre com o problema:** as famílias que vivem nas partes baixas e nas margens de córregos urbanos. Quando a água sai da margem, elas perdem móveis, ficam sem conseguir sair de casa e, nas encostas, correm risco de deslizamento.

**Quem usaria o sistema:** a coordenação de uma associação de bairro ou a Defesa Civil local. A pergunta que ele ajuda a responder é: *"o córrego vai transbordar nas próximas horas e o que dá para fazer hoje?"*. Com o alerta em mãos, a coordenação pode reforçar a vigilância, avisar quem mora nas áreas baixas, deixar um abrigo de prontidão ou acionar o plano de contingência. **O sistema não decide nada disso sozinho:** ele mostra os números e o motivo do alerta para quem vai decidir.

**O território** é fictício e descrito só por região e equipamento, nunca por pessoas: *Córrego da Várzea, Zona Leste*. O sistema não guarda dados pessoais.

**Por que estes três sensores:** cada um mostra uma parte do mesmo problema.

| Sensor | Tipo | O que mede | Por que importa para alagamento |
|---|---|---|---|
| `PLU-01` pluviômetro | `chuva` | intensidade da chuva (mm/h) | é a **causa**: o primeiro sinal de risco |
| `NIV-01` nível do córrego | `nivel` | altura da água (cm) | é o **efeito**: sobe com atraso e mostra quando a água vai sair da margem |
| `UMI-01` umidade do solo | `umidade` | saturação do solo (%) | é a **condição**: solo encharcado não absorve mais chuva e a água escorre para o córrego |

## Como compilar, testar e rodar

**Requisitos:** JDK 17 ou superior e Maven 3.9 ou superior. Nada mais precisa ser instalado: o banco H2 vem como dependência do Maven.

Na pasta que contém o `pom.xml`:

```bash
mvn clean test           # compila e roda os testes (JUnit 5 + Mockito)
mvn compile exec:java    # abre o menu do sistema
```

O banco é um **H2 em arquivo**, criado automaticamente em `./dados/territorio.mv.db` na primeira execução. As tabelas vêm de `src/main/resources/schema.sql`. Por isso rode sempre a partir da pasta do projeto, para usar o mesmo banco. O histórico sobrevive ao reiniciar o programa. Para começar do zero, apague a pasta `dados/`.

**Código de acesso** para alterar limiares (menu 5): `coord2026`.

### Roteiro rápido de demonstração

1. Opção **1**, informando **6** leituras: gera o histórico de que as previsões precisam.
2. Opção **2**: roda as duas estratégias de previsão. Com poucas leituras, o sistema avisa que os dados são insuficientes e não emite alerta.
3. As leituras são aleatórias e nem sempre chegam aos limiares. Para ver um alerta, abra a opção **5**, altere os limiares da chuva para valores baixos (código `coord2026`) e rode a opção **2** de novo.
4. Opções **3** e **4**: histórico por sensor e alertas emitidos.
5. Feche o programa (**0**), abra de novo e repita as opções **3** e **4**: os dados continuam lá.
6. Opção **7** → **1** (ligar a falha de um sensor) ou **7** → **2** (forçar a leitura 999) e depois a opção **1** com 1 leitura: mostra as exceções tratadas no menu.
7. Opção **8**: exporta o histórico de um sensor para um CSV em `dados/`.

### Menu

| Opção | O que faz |
|---|---|
| 1 | Verificar sensores: pergunta quantas leituras fazer (1 a 100) e, para cada rodada, lê, valida e grava uma medição de cada sensor |
| 2 | Rodar previsões: média móvel e comparação entre períodos, com alerta quando há |
| 3 | Ver histórico de um sensor |
| 4 | Ver alertas emitidos |
| 5 | Gerenciar dados do território (nome, região e limiares, que exigem código) |
| 6 | Status completo |
| 7 | Simulações para demonstração: ligar/desligar a falha de um sensor (mostrando se está operacional ou indisponível) e forçar uma leitura inválida |
| 8 | Exportar histórico para CSV (bônus) |
| 0 | Sair |

## Estrutura

```text
src/main/java/
├── dominio/       ComponenteTerritorial, StatusComponente, Sensor, SensorSimulado,
│                  SensorPluviometro, SensorNivelCorrego, SensorUmidadeSolo,
│                  Medicao, Alerta, NivelAlerta, Limiares, DadosTerritorio,
│                  EstrategiaPrevisao, PrevisaoMediaMovel, PrevisaoComparacao, ResultadoPrevisao
├── excecao/       DadosInvalidosException, FonteIndisponivelException
├── persistencia/  FabricaConexao, MedicaoDAO, AlertaDAO
├── servico/       FabricaPrevisao, TipoPrevisao, ServicoMonitoramento,
│                  AvaliacaoPrevisao, ExportadorCSV
└── SistemaMonitoramento.java   (menu principal)
src/main/resources/schema.sql   (tabelas medicao e alerta)
src/test/java/                  (testes JUnit 5 e Mockito, mesma divisão em pacotes)
```

Os pacotes seguem a estrutura sugerida no enunciado.

## Decisões de projeto

### Domínio e polimorfismo

- **`ComponenteTerritorial`** (abstrata): id, nome e região, com `verificarStatus()` abstrato. A filha concreta é **`SensorSimulado`** (também abstrata, implementa `Sensor`), da qual herdam `SensorPluviometro`, `SensorNivelCorrego` e `SensorUmidadeSolo`. Um sensor é um componente físico do território, então herda id, nome e região. A base cuida de falha simulada, validação da faixa física e status. Cada filha só sobrescreve `gerarValor()`.
- **`Sensor`** (interface): `lerValor()`, `verificarFuncionamento()`, `getTipo()`, `getUnidade()`, `getId()` e o método padrão `lerMedicao()`, que já devolve uma `Medicao` validada.
- **`Medicao`** e **`Alerta`**: o construtor valida os campos (sensor vazio, `NaN`, data nula, alerta com nível `NORMAL` ou valor abaixo do limite lançam `DadosInvalidosException`). Assim não existe objeto inválido em memória.
- **`Limiares`** (imutável): os limites de ATENÇÃO, ALERTA e CRÍTICO de um tipo de sensor. Só é criado se forem positivos e crescentes.
- **`DadosTerritorio`**: atributos privados, **código de acesso** para alterar limiares e limiares **por tipo de sensor**. Com código errado ou limiar inválido, nada muda (o novo `Limiares` é criado e validado antes de substituir o antigo).
- **`NivelAlerta`** (enum): NORMAL, ATENÇÃO, ALERTA e CRÍTICO, cada um com uma orientação escrita como **sugestão** ("avaliar", "considerar"), não como ordem.

Limiares iniciais (**parâmetros de simulação** escolhidos pela equipe, não valores oficiais):

| Sensor | Faixa física válida | ATENÇÃO | ALERTA | CRÍTICO |
|---|---|---|---|---|
| Chuva (mm/h) | 0 a 300 | 20 | 40 | 70 |
| Nível do córrego (cm) | 0 a 400 | 90 | 120 | 150 |
| Umidade do solo (%) | 0 a 100 | 70 | 80 | 90 |

### Exceções customizadas

| Exceção | Quando é lançada |
|---|---|
| `DadosInvalidosException` | leitura fora da faixa física ou `NaN`, campo obrigatório ausente, limiar negativo ou fora de ordem, código de acesso errado, tipo de sensor desconhecido, histórico que mistura sensores, tipo de previsão nulo |
| `FonteIndisponivelException` | sensor em falha, banco de dados inacessível, falha ao gravar o CSV (guarda a fonte em `getFonte()`) |

As duas são **verificadas** (`extends Exception`): o compilador obriga quem lê um sensor ou acessa o banco a tratá-las. As mensagens dizem o problema e o valor recebido, por exemplo `Leitura 999,0 mm/h fora da faixa válida (0 a 300) no sensor PLU-01.` O menu captura as duas e mostra a mensagem ao usuário. Nenhum `catch` é vazio. Um registro corrompido no banco também vira `FonteIndisponivelException`, indicando o id da linha.

### Previsões (polimorfismo)

`EstrategiaPrevisao` (abstrata) define o contrato (`prever()` abstrato), valida o histórico e o ordena por data. As filhas chamam `super(nome, mínimo)` no construtor, sobrescrevem `prever()` e estendem `descrever()` com `super.descrever()`.

| Estratégia | Cálculo | Mínimo de leituras |
|---|---|---|
| `PrevisaoMediaMovel` (N = 5) | média das últimas 5 leituras | 5 |
| `PrevisaoComparacao` (N = 3) | `mediaAtual + (mediaAtual − mediaAnterior)` entre os dois últimos blocos de 3 leituras; nunca devolve valor negativo | 6 |

O resultado é um `ResultadoPrevisao`: o valor previsto e um texto que explica o que ele significa (por exemplo, "tendência em alta"). Com **menos leituras que o mínimo**, o resultado vem **sem valor**, explica quantas leituras faltam e **nenhum alerta é emitido**. Isso não é erro: o sistema prefere se calar a chutar. Histórico nulo ou que mistura sensores é erro de quem chamou e lança `DadosInvalidosException`.

### Alertas

`Alerta.avaliar(...)` compara o valor previsto com os limiares do tipo do sensor e cria o alerta do nível mais grave atingido. `getMotivo()` gera o texto que explica **qual sensor, qual valor previsto, qual limite foi atingido e qual estratégia** gerou o alerta, por exemplo:

> Previsão de 80,0 mm/h para PLU-01 (estratégia: Média móvel) atingiu o limite de CRÍTICO (70,0 mm/h). Sugestão: avisar quem está em área de risco e avaliar o plano de contingência.

### Persistência (JDBC + H2)

Usamos JDBC puro com H2 em arquivo (`jdbc:h2:./dados/territorio`). Como o banco fica gravado em disco, as medições e os alertas sobrevivem ao reiniciar o programa; um H2 em memória perderia tudo ao fechar. O arquivo do banco fica na pasta `dados/`, que não vai para o GitHub. As tabelas `medicao` e `alerta` são criadas pelo `src/main/resources/schema.sql`, que a `FabricaConexao` executa ao iniciar (`CREATE TABLE IF NOT EXISTS`, então dados existentes não são apagados). Por isso o projeto roda do zero na correção, sem configurar nada. Os DAOs usam `PreparedStatement` em todas as consultas (nunca concatenam String no SQL) e `try-with-resources` para fechar `Connection`, `PreparedStatement` e `ResultSet`. Todo `SQLException` vira `FonteIndisponivelException`, com a causa original preservada.

**Por que JDBC e não JPA:** os dados são só duas tabelas simples, e queríamos controlar o SQL e a validação no domínio, sem entidades duplicadas e sem a configuração do Spring. Medições e alertas só são inseridos e consultados, nunca alterados ou apagados, porque formam o histórico que justifica cada previsão.

### Serviço

O `ServicoMonitoramento` fica entre o menu e o banco e segue o fluxo **ler → validar → gravar → prever → alertar**. Em `registrarLeitura`, o sensor lê e valida a medição antes de gravar: leitura inválida ou sensor em falha não grava nada. Em `preverEAvaliar`, o serviço pede a estratégia à `FabricaPrevisao`, busca no banco só as últimas N leituras que ela exige, prevê e compara com os limiares; com leituras insuficientes ele se cala (sem previsão e sem alerta) e, se houver alerta, grava no banco. Os DAOs, a fábrica, o território e o relógio entram pelo construtor para podermos testar a lógica de alerta com Mockito, sem banco e com horário fixo.

### Bônus: exportação em CSV

A opção 8 do menu exporta o histórico de um sensor para `dados/historico-<sensor>.csv` (separador `;`, com cabeçalho). Escolhemos só este bônus.

## Padrão criacional: Factory (`FabricaPrevisao`)

**O que faz:** guarda um `EnumMap<TipoPrevisao, criador>` e é a **única** classe que conhece `PrevisaoMediaMovel` e `PrevisaoComparacao` e as janelas de cada uma. O serviço e o menu pedem `fabrica.criar(tipo)` ou `fabrica.criarTodas()` e trabalham só com `EstrategiaPrevisao`. A estratégia é escolhida **sem `if tipo ==` e sem `instanceof`**, como o enunciado exige.

**Por que aqui e não em outro lugar:** a escolha da estratégia é o único ponto do sistema em que um `if` por tipo apareceria. Sem a fábrica, esse `if` se repetiria no serviço e no menu, e cada nova estratégia exigiria mexer nos dois. Com ela, o resto do código só chama `prever()` e cada estratégia responde do seu jeito (polimorfismo).

**Por que não Singleton nem Builder:** os limiares já vivem em `DadosTerritorio`, protegidos por código de acesso, e um Singleton de configuração criaria um segundo caminho para alterá-los, além de estado global que complica os testes. `Medicao` e `Alerta` têm poucos campos, todos obrigatórios, e já se validam no construtor, então um Builder só acrescentaria código.

**Trade-off:** é mais uma classe e um nível de indireção. Para incluir uma estratégia nova, é preciso registrá-la na fábrica e no enum `TipoPrevisao`, mas o restante do código não muda.

## Testes

São **66 testes JUnit 5 em 12 classes**, no padrão **AAA** (Arrange, Act, Assert), com vários `assertThrows` das duas exceções customizadas. Os valores esperados são calculados à mão nos próprios testes (por exemplo, a média de 10..50 é 30; a comparação 15 → 35 projeta 55), então se um cálculo errar, o teste fica vermelho.

- **Mockito:** `ServicoMonitoramentoTest` mocka `MedicaoDAO` e `AlertaDAO` e verifica a lógica de alerta sem banco: previsão crítica grava o alerta certo (`ArgumentCaptor`); previsão abaixo da atenção ou histórico insuficiente **não** grava alerta (`never()`); leitura inválida ou sensor em falha não grava medição (`verifyNoInteractions`).
- **DAOs:** `MedicaoDAOTest` e `AlertaDAOTest` usam um H2 de verdade em pasta temporária (`@TempDir`): id gerado, ordem do histórico, últimas N leituras, dados que sobrevivem ao reabrir o banco e banco inacessível.
- **Domínio:** validações de `Medicao`, `Limiares`, `DadosTerritorio` (incluindo código de acesso), `Alerta` e sensores; as duas estratégias, a fábrica e o `ExportadorCSV`.

## Reflexão crítica

**Quando o sistema deve se calar?** Quando o histórico do sensor tem menos leituras que o mínimo da estratégia: menos de 5 para a média móvel e menos de 6 para a comparação entre períodos. Com poucas leituras, uma média ou uma tendência não é confiável: uma única leitura alta poderia virar um CRÍTICO. Nesses casos o código não devolve uma previsão "chutada": o `ResultadoPrevisao` vem sem valor, o texto diz quantas leituras faltam e nenhum alerta é emitido. O sistema também se cala quando o sensor está em falha: nenhuma medição é inventada para cobrir o buraco. Mesmo com dados suficientes, a comparação entre períodos exagera picos isolados, e a média móvel é estável, mas reage devagar. Por isso o alerta informa qual estratégia o gerou, e a escolha entre elas cabe a quem coordena.

**O que os dados não representam?** Um sensor descreve um ponto, não a região inteira: o pluviômetro pode registrar 0 mm/h enquanto chove forte a dois quilômetros, e uma única régua não mostra se a água sai da margem em outro trecho do córrego. As leituras são **simuladas** com sorteios que nós escolhemos, não medidas reais, e os limiares (por exemplo, 20, 40 e 70 mm/h para chuva) são parâmetros de exemplo, **não normas da Defesa Civil**. Eles precisariam ser calibrados com séries históricas e revisados por especialistas antes de qualquer uso real. O sistema também não sabe quem está exposto ao risco, nem considera obras de drenagem, eventos no bairro ou a confiabilidade de cada sensor. Além disso, os limiares alterados no menu valem só para a sessão atual e voltam ao padrão ao reiniciar; só medições e alertas ficam gravados no banco.

**Por que o alerta apoia a decisão da pessoa, em vez de decidir por ela?** O alerta mostra a evidência (sensor, valor previsto, limite atingido e estratégia) e uma orientação genérica, escrita como sugestão. A decisão de avisar uma comunidade depende de contexto que o sistema não tem. Por isso os limiares são ajustáveis pela coordenação, com código de acesso, e quem responde pela ação continua sendo uma pessoa. Um alarme falso automático custaria a confiança da comunidade, e uma ausência de alerta não garante que está tudo bem.

## Divisão de tarefas

| Integrante | Responsabilidade |
|---|---|
| Enzo Yokokura Araujo | Domínio e exceções (componente territorial, sensores, medição, alerta, limiares e dados do território com código de acesso), estratégias de previsão e Factory, testes dessas partes, menu completo, configuração do Git e README |
| Marco Antonio Ferreira Fonseca | Persistência (banco H2, criação das tabelas e acesso a medições e alertas), serviço de monitoramento, testes do acesso ao banco e do serviço (com Mockito) e exportação CSV (bônus) |

Revisão do código e integração na `main` foram feitas pelos dois.

## Uso de IA

Usamos o Claude como apoio, em conjunto, e declaramos abaixo onde e como revisamos.

- **Onde usamos:** para entender o enunciado e planejar a divisão de tarefas; para configurar o ambiente; para gerar a primeira versão do domínio e das exceções; para tirar dúvidas de Git; e na criação deste README.
- **Como revisamos:** lemos as classes uma a uma, rodamos `mvn clean test` e o menu, alteramos o que não entendemos e testamos fechando e abrindo o programa para ver os dados persistirem.
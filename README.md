
# Global Solution 2º/2026 - POO: Territory Predictive Insights

Trabalho da Global Solution 2026/2 de POO. É um sistema de monitoramento territorial feito em Java, que funciona pelo console. Ele recebe dados de sensores simulados, verifica se as informações são válidas, armazena tudo em um banco de dados e faz previsões. Quando identifica algum risco, gera um alerta de ATENÇÃO, ALERTA ou CRÍTICO, informando o sensor, o valor registrado e o limite atingido.

## Problema e público beneficiado

**Problema escolhido: chuvas intensas e risco de alagamentos.**

O sistema monitora sinais como volume de chuva, nível dos córregos e saturação do solo para identificar possíveis alagamentos.

**Quem é afetado:** moradores de regiões baixas e próximas a córregos, que podem sofrer perdas materiais, dificuldades de locomoção e riscos de deslizamento.

**Quem utiliza o sistema:** associações de bairro e Defesa Civil, que podem usar os alertas para orientar moradores, reforçar a vigilância e preparar abrigos ou planos de emergência.

**O território** é fictício e descrito só por região e equipamento, nunca por pessoas: _Córrego da Várzea, Zona Leste_. O sistema não guarda dados pessoais.

## Como compilar e rodar

**Requisitos:** JDK 17 ou superior e Maven 3.9 ou superior.

Na pasta que contém o `pom.xml`:

```bash
mvn compile exec:java    # compila e abre o menu do sistema
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
├── SistemaMonitoramento.java
├── dominio/
│   ├── Alerta.java
│   ├── ComponenteTerritorial.java
│   ├── DadosTerritorio.java
│   ├── EstrategiaPrevisao.java
│   ├── Limiares.java
│   ├── Medicao.java
│   ├── NivelAlerta.java
│   ├── PrevisaoComparacao.java
│   ├── PrevisaoMediaMovel.java
│   ├── ResultadoPrevisao.java
│   ├── Sensor.java
│   ├── SensorNivelCorrego.java
│   ├── SensorPluviometro.java
│   ├── SensorSimulado.java
│   ├── SensorUmidadeSolo.java
│   └── StatusComponente.java
├── excecao/
│   ├── DadosInvalidosException.java
│   └── FonteIndisponivelException.java
├── persistencia/
│   ├── AlertaDAO.java
│   ├── FabricaConexao.java
│   └── MedicaoDAO.java
└── servico/
    ├── AvaliacaoPrevisao.java
    ├── ExportadorCSV.java
    ├── FabricaPrevisao.java
    ├── ServicoMonitoramento.java
    └── TipoPrevisao.java

src/main/resources/
└── schema.sql

src/test/java/
├── dominio/
│   ├── AlertaTest.java
│   ├── DadosTerritorioTest.java
│   ├── Historicos.java
│   ├── LimiaresTest.java
│   ├── MedicaoTest.java
│   ├── PrevisaoComparacaoTest.java
│   ├── PrevisaoMediaMovelTest.java
│   └── SensorTest.java
├── persistencia/
│   ├── AlertaDAOTest.java
│   └── MedicaoDAOTest.java
└── servico/
    ├── ExportadorCSVTest.java
    ├── FabricaPrevisaoTest.java
    └── ServicoMonitoramentoTest.java
```

## Sensores e limiares

**Sensores.** Cada um mostra uma parte do mesmo problema.

| Sensor | Tipo | O que mede | Por que importa para alagamento |
|---|---|---|---|
| `PLU-01` pluviômetro | `chuva` | intensidade da chuva (mm/h) | é a **causa**: o primeiro sinal de risco |
| `NIV-01` nível do córrego | `nivel` | altura da água (cm) | é o **efeito**: sobe com atraso e mostra quando a água vai sair da margem |
| `UMI-01` umidade do solo | `umidade` | saturação do solo (%) | é a **condição**: solo encharcado não absorve mais chuva e a água escorre para o córrego |

**Limiares.** Cada tipo de sensor tem três limites crescentes: ao atingir o primeiro, o sistema emite ATENÇÃO; o segundo, ALERTA; o terceiro, CRÍTICO. Os valores iniciais são **parâmetros de simulação escolhidos pela equipe, não valores oficiais**. Uma leitura fora da faixa física é recusada como inválida e não vai para o banco.

| Sensor | Faixa física válida | ATENÇÃO | ALERTA | CRÍTICO |
|---|---|---|---|---|
| Chuva (mm/h) | 0 a 300 | 20 | 40 | 70 |
| Nível do córrego (cm) | 0 a 400 | 90 | 120 | 150 |
| Umidade do solo (%) | 0 a 100 | 70 | 80 | 90 |

A coordenação pode alterar os limiares pelo menu (opção 5), com o código de acesso. Os valores alterados valem só até fechar o programa.

## Decisões de projeto

### Domínio e polimorfismo

- **`ComponenteTerritorial`** (abstrata): id, nome e região, com `verificarStatus()` abstrato. A filha concreta é **`SensorSimulado`** (também abstrata, implementa `Sensor`), da qual herdam `SensorPluviometro`, `SensorNivelCorrego` e `SensorUmidadeSolo`. Um sensor é um componente físico do território, então herda id, nome e região. A base cuida de falha simulada, validação da faixa física e status. Cada filha só sobrescreve `gerarValor()`.
- **`Sensor`** (interface): `lerValor()`, `verificarFuncionamento()`, `getTipo()`, `getUnidade()`, `getId()` e o método padrão `lerMedicao()`, que já devolve uma `Medicao` validada.
- **`Medicao`** e **`Alerta`**: o construtor valida os campos (sensor vazio, `NaN`, data nula, alerta com nível `NORMAL` ou valor abaixo do limite lançam `DadosInvalidosException`). Assim não existe objeto inválido em memória.
- **`Limiares`** (imutável): os limites de ATENÇÃO, ALERTA e CRÍTICO de um tipo de sensor. Só é criado se forem positivos e crescentes.
- **`DadosTerritorio`**: atributos privados, **código de acesso** para alterar limiares e limiares **por tipo de sensor**. Com código errado ou limiar inválido, nada muda (o novo `Limiares` é criado e validado antes de substituir o antigo).
- **`NivelAlerta`** (enum): NORMAL, ATENÇÃO, ALERTA e CRÍTICO, cada um com uma orientação escrita como **sugestão** ("avaliar", "considerar"), não como ordem.


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

Utilizamos JDBC com H2 em arquivo para armazenar medições e alertas de forma permanente, mantendo os dados mesmo após fechar o programa. O banco fica na pasta `dados/` e suas tabelas são criadas automaticamente na inicialização, sem apagar informações existentes. As consultas utilizam `PreparedStatement` e os recursos são fechados corretamente. Erros de banco são tratados por `FonteIndisponivelException`.

**Por que JDBC e não JPA:** como o projeto possui apenas duas tabelas simples, o JDBC oferece maior controle sobre as consultas e validações, sem a necessidade de configurações adicionais. Além disso, medições e alertas são apenas registrados e consultados, preservando o histórico para justificar cada previsão.

### Serviço

O `ServicoMonitoramento` fica entre o menu e o banco e segue o fluxo **ler → validar → gravar → prever → alertar**. Em `registrarLeitura`, o sensor lê e valida a medição antes de gravar: leitura inválida ou sensor em falha não grava nada. Em `preverEAvaliar`, o serviço pede a estratégia à `FabricaPrevisao`, busca no banco só as últimas N leituras que ela exige, prevê e compara com os limiares; com leituras insuficientes ele se cala (sem previsão e sem alerta) e, se houver alerta, grava no banco. Os DAOs, a fábrica, o território e o relógio entram pelo construtor para podermos testar a lógica de alerta com Mockito, sem banco e com horário fixo.

### Bônus: exportação em CSV

A opção 8 do menu exporta o histórico de um sensor para `dados/historico-<sensor>.csv` (separador `;`, com cabeçalho).

## Padrão criacional: Factory (`FabricaPrevisao`)

**O que faz:** guarda um `EnumMap<TipoPrevisao, criador>` e é a **única** classe que conhece `PrevisaoMediaMovel` e `PrevisaoComparacao` e as janelas de cada uma. O serviço e o menu pedem `fabrica.criar(tipo)` ou `fabrica.criarTodas()` e trabalham só com `EstrategiaPrevisao`. A estratégia é escolhida **sem `if tipo ==` e sem `instanceof`**, como o enunciado exige.

**Por que aqui e não em outro lugar:** a escolha da estratégia é o único ponto do sistema em que um `if` por tipo apareceria. Sem a fábrica, esse `if` se repetiria no serviço e no menu, e cada nova estratégia exigiria mexer nos dois. Com ela, o resto do código só chama `prever()` e cada estratégia responde do seu jeito (polimorfismo).

**Por que não Singleton nem Builder:** os limiares já vivem em `DadosTerritorio`, protegidos por código de acesso, e um Singleton de configuração criaria um segundo caminho para alterá-los, além de estado global que complica os testes. `Medicao` e `Alerta` têm poucos campos, todos obrigatórios, e já se validam no construtor, então um Builder só acrescentaria código.

**Trade-off:** é mais uma classe e um nível de indireção. Para incluir uma estratégia nova, é preciso registrá-la na fábrica e no enum `TipoPrevisao`, mas o restante do código não muda.

## Testes

**Como rodar.** Na pasta que contém o `pom.xml`:

```bash
mvn clean test
```

O comando compila o projeto e executa todos os testes. No final, o Maven deve mostrar `BUILD SUCCESS` e `Tests run: 66, Failures: 0, Errors: 0`. Para rodar só uma classe:

```bash
mvn test -Dtest=ServicoMonitoramentoTest
```

Os relatórios detalhados ficam em `target/surefire-reports/`. Os testes **não usam** o banco da pasta `dados/`: os de DAO criam um H2 próprio numa pasta temporária, e os do serviço trocam os DAOs por mocks. Por isso dá para rodá-los sem apagar ou alterar o histórico do programa.

**O que é testado.** São 66 testes JUnit 5 em 12 classes, no padrão **AAA** (Arrange, Act, Assert), com vários `assertThrows` das duas exceções customizadas. [...resto da seção como já está...]

## Reflexão crítica

Nosso sistema não gera previsões quando faltam dados suficientes: são necessárias cinco leituras para a média móvel e seis para a comparação entre períodos. O mesmo ocorre em caso de falha do sensor ou leitura fora da faixa permitida. Mesmo com dados suficientes, as estratégias possuem limitações: a média móvel é mais estável, mas reage lentamente, enquanto a comparação entre períodos responde rápido, mas pode exagerar picos isolados. Por isso, cada alerta informa a estratégia utilizada.

Os sensores representam apenas um ponto, não toda a região. Além disso, as leituras são **simuladas por sorteios dentro de faixas plausíveis**, e os limiares (20, 40 e 70 mm/h para chuva) são valores de demonstração, não normas da Defesa Civil. Para uso real, precisariam ser calibrados com dados históricos e avaliados por especialistas. O sistema também não considera fatores como drenagem local e pessoas expostas ao risco. Os limiares alterados no menu valem apenas durante a execução, enquanto medições e alertas ficam salvos no banco.

O alerta apoia a decisão humana ao apresentar os dados e uma sugestão de ação, sem emitir ordens. Como o sistema não conhece todo o contexto da região, a decisão final continua sendo de uma pessoa. Afinal, um alerta falso pode comprometer a confiança da comunidade, e a ausência de um alerta não garante que não exista perigo.

## Divisão de tarefas

| Integrante | Responsabilidade |
|---|---|
| Enzo Yokokura Araujo | Domínio e exceções (componente territorial, sensores, medição, alerta, limiares e dados do território com código de acesso), estratégias de previsão e Factory, testes dessas partes, menu completo, configuração do Git e README |
| Marco Antonio Ferreira Fonseca | Persistência (banco H2, criação das tabelas e acesso a medições e alertas), serviço de monitoramento, testes do acesso ao banco e do serviço (com Mockito) e exportação CSV (bônus) |

A revisão do código e integração na `main` foram feitas pelos dois.

## Uso de IA

Usamos o Claude como apoio, em conjunto, e declaramos abaixo onde e como revisamos.

- **Onde usamos:** para entender o enunciado e planejar a divisão de tarefas; para configurar o ambiente; para gerar o esqueleto da primeira versão do domínio e das exceções; para tirar dúvidas de Git; e na criação deste README.
- **Como revisamos:** lemos as classes uma a uma, rodamos `mvn clean test` e o menu, alteramos o que não entendemos e testamos fechando e abrindo o programa para ver os dados persistirem.

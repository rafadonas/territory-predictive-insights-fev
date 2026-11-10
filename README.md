# ☔ VigiaChuva: Territory Predictive Insights (POO)

Global Solution 2026.2, FIAP, Ciência da Computação (Turma de Fevereiro), Programação Orientada a Objetos.
Tema: **CitizenTech**. Recorte escolhido: **🌊 clima local, chuva intensa e risco de alagamento**.

| Integrante | RM | Responsabilidade principal |
|---|---|---|
| Rafael do Nascimento Silva | 566263 | Domínio, exceções e menu |
| Guilherme Morais de Assis | 564198 | Persistência (JDBC + H2), previsões, alertas e testes |

---

## 1. Público beneficiado e decisão apoiada
**Quem sofre o problema:** moradores de áreas baixas, perto de córregos, e de encostas de um bairro urbano. Quando chove forte, ruas alagam, o córrego transborda e o solo encharcado pode deslizar.

**Quem usa o sistema:** a coordenação da Defesa Civil local e as lideranças comunitárias (associação de moradores).

**Que decisão ele apoia:** *"Devemos avisar a comunidade, deixar equipes de prontidão ou avaliar a retirada preventiva de moradores, e em qual região?"* O sistema lê os sensores, guarda o histórico, prevê os próximos valores e explica por que cada alerta foi emitido. **Quem decide é a pessoa responsável:** o alerta só sugere uma ação.

> O território "Várzea Leste" e seus equipamentos são **fictícios** e todos os dados são **simulados**. Não há dados pessoais: o território é descrito por região e equipamento, nunca por indivíduos.

## 2. Como executar
Pré-requisito: **Java 17 ou superior** (testado no JDK 25). O Maven **não** precisa estar instalado: o projeto traz o Maven Wrapper (`mvnw`).

```bash
git clone https://github.com/rafadonas/territory-predictive-insights-fev.git
cd territory-predictive-insights-fev
```

```bash
# Windows (PowerShell ou CMD)
mvnw.cmd clean package
java -jar target/vigiachuva-poo.jar

# Linux / macOS
./mvnw clean package
java -jar target/vigiachuva-poo.jar
```
- `package` compila, **roda os 33 testes** e gera o JAR executável (já com o H2).
- Alternativa sem gerar o JAR: `mvnw.cmd -q compile exec:java`.
- Acentos no console do Windows: se aparecerem caracteres estranhos, rode `chcp 65001` antes.
- **Código de acesso** para alterar limiares: `GS2026` (pode ser trocado pela variável de ambiente `VIGIACHUVA_CODIGO`).

**Roteiro rápido de demonstração:** opção **1** com 12 ciclos (gera histórico) → opção **2** (previsões e alertas) → feche e abra o programa de novo → opção **5** (o histórico continua lá) → opção **3** (histórico por sensor e exportação para CSV).

## 3. Configuração do banco (para a correção)
- **H2 embutido em ARQUIVO**, criado automaticamente em `./dados/vigiachuva.mv.db` na primeira execução. Não há nada para instalar ou configurar.
- As tabelas são criadas pelo `src/main/resources/schema.sql` (`CREATE TABLE IF NOT EXISTS`), que roda a cada inicialização sem apagar dados.
- Escolhemos arquivo, e não memória, porque o enunciado pede que **os dados sobrevivam ao reiniciar o programa**. H2 em memória perderia tudo ao fechar.
- Para começar do zero, apague a pasta `dados/`. Para usar outro banco H2: `java -Dvigiachuva.db.url=jdbc:h2:./outro -jar target/vigiachuva-poo.jar`.
- Os testes usam H2 **em memória**, cada um com um banco próprio, e não tocam no arquivo.

## 4. Estrutura do projeto
```
src/main/java/
├── dominio/       ComponenteTerritorial (abstrata), PontoMonitoramento, Piscinao, StatusComponente,
│                  Sensor (interface), SensorSimulado, Pluviometro, ReguaNivelCorrego, SensorUmidadeSolo, TipoSensor,
│                  DadosTerritorio, Limiares, EstrategiaPrevisao (abstrata), PrevisaoMediaMovel,
│                  PrevisaoComparacao, ResultadoPrevisao, Medicao, Alerta, NivelAlerta
├── excecao/       DadosInvalidosException, FonteIndisponivelException, AcessoNegadoException, PersistenciaException
├── persistencia/  ConexaoBanco, MedicaoDAO, AlertaDAO   (JDBC + PreparedStatement + try-with-resources)
├── servico/       ServicoMonitoramento, FabricaPrevisao, ConfiguracaoSistema, RelatorioBuilder,
│                  ExportadorCsv, CenarioPiloto, ResultadoAnalise
└── SistemaMonitoramento.java   menu principal (Scanner)
src/main/resources/schema.sql
src/test/java/     MedicaoTest, PrevisaoMediaMovelTest, PrevisaoComparacaoTest, DadosTerritorioTest,
                   ComponentesTest, ServicoMonitoramentoTest (Mockito), FabricaPrevisaoTest, PersistenciaH2Test
```

## 5. Onde está cada requisito
| Critério | Onde está no código |
|---|---|
| **Domínio e polimorfismo** | `ComponenteTerritorial` abstrata (id, nome, região, status) com `verificarStatus()` e `detalhar()` abstratos e **2 filhas**: `PontoMonitoramento`, cujo status depende de quantos sensores respondem, e `Piscinao`, cujo status depende da ocupação. Interface `Sensor` (`lerValor`, `estaFuncionando`, `getTipo`) com **3 implementações**: `Pluviometro`, `ReguaNivelCorrego` e `SensorUmidadeSolo`. `EstrategiaPrevisao` abstrata com `PrevisaoMediaMovel` e `PrevisaoComparacao`, que sobrescrevem `prever()`, chamam `super.prever()` (checagem de histórico mínimo) e `super(...)` no construtor. **Nenhum `if tipo ==` ou `instanceof`**: a estratégia sai da `FabricaPrevisao` (mapa) e o serviço só chama `prever()`. |
| **Exceções e validação** | `DadosInvalidosException`: medição fora da faixa física (no construtor de `Medicao`), população ou área negativa (setters de `DadosTerritorio`), limiar fora de ordem ou acima da faixa (`Limiares`, `alterarLimiares`), volume do piscinão, estratégia desconhecida. `FonteIndisponivelException`: sensor que não responde (`SensorSimulado.lerValor`). Também `AcessoNegadoException` (código errado) e `PersistenciaException` (falha de banco com mensagem clara). Todas são capturadas no menu com mensagem útil, e **nenhum catch é vazio**. |
| **Persistência** | `MedicaoDAO` e `AlertaDAO` com **PreparedStatement** (`?`, nunca concatenação) e **try-with-resources** em toda conexão, comando e ResultSet. Cada medição válida e cada alerta é gravado; o menu lista o histórico por sensor (opção 3.1) e os alertas emitidos (opção 3.2). |
| **Padrão criacional** | Três padrões, justificados na seção 6: Factory, Singleton e Builder. |
| **Testes unitários** | **33 testes** JUnit 5 no padrão AAA, com vários `assertThrows` das exceções customizadas e **Mockito** mockando `MedicaoDAO` e `AlertaDAO` em `ServicoMonitoramentoTest`. |
| **Bônus (+0,5)** | **Exportar o histórico consultado em CSV** (opção 3.1, depois responda `s`): arquivo em `./exportacoes/` com `;` e vírgula decimal, que abre direto no Excel. |

## 6. Padrões criacionais: por que ali e não em outro lugar
- **Factory (`FabricaPrevisao`).** É o único lugar onde se decide *qual* estratégia criar e *com quais parâmetros* (janela de 5 para a média móvel, 3+3 para a comparação, ambas vindas da configuração). Usamos um mapa de código para construtor em vez de `if/else`; para incluir uma terceira estratégia basta registrar uma linha no catálogo. Não faria sentido usar Factory para `Medicao` ou `Alerta`: existe um tipo só de cada, e o construtor já valida tudo.
- **Singleton (`ConfiguracaoSistema`).** Limiares e janelas são regras que precisam ser **as mesmas** para o serviço que gera alertas, para o menu que os mostra e para quem os altera. Com duas instâncias, o menu poderia exibir 30 mm/h enquanto o serviço usa 50 mm/h. O construtor é privado e o acesso é só por `getInstancia()`. Não usamos Singleton nos DAOs nem no serviço porque eles recebem dependências pelo construtor, o que permite trocá-los por mocks nos testes.
- **Builder (`RelatorioBuilder`).** O "status completo" tem várias partes opcionais (território, componentes, contagens, limiares, alertas). O menu encadeia só as partes que quer (a opção 4 usa território e limiares; a opção 5 usa tudo) e chama `construir()`. Sem o Builder, seria um método gigante com parâmetros booleanos.

## 7. Como as previsões e os alertas funcionam
1. A opção 1 lê os sensores. Cada valor vira uma `Medicao`, que valida a faixa física e é gravada. Sensor que não responde gera `FonteIndisponivelException`; valor impossível (ex.: chuva de -1 mm/h) gera `DadosInvalidosException` e é **descartado**.
2. A opção 2 busca no banco as últimas 24 medições de cada sensor e roda as duas estratégias:
   - **Média móvel:** média das últimas 5 leituras. É estável, mas demora a perceber subidas rápidas.
   - **Comparação com o período anterior:** compara a média das 3 últimas com a das 3 anteriores e projeta a mesma variação, limitada à faixa física. Percebe tendências cedo, mas exagera se a variação for ruído.
3. Cada previsão **confiável** é comparada com os limiares do `ConfiguracaoSistema`. Se alguma atingir um limite, é gravado **um** alerta (ATENÇÃO, ALERTA ou CRÍTICO, sempre o mais grave) com a justificativa: qual sensor, valor previsto, limite estourado, qual estratégia, a outra estimativa e a ação sugerida.

Limiares padrão (valores de referência adotados pela equipe para a simulação, ajustáveis no menu com código):

| Sensor | ATENÇÃO | ALERTA | CRÍTICO |
|---|---|---|---|
| Chuva (mm/h) | 10 | 30 | 50 |
| Nível do córrego (m) | 2,5 | 3,2 | 3,8 |
| Umidade do solo (%) | 80 | 90 | 95 |

## 7.1 Validação das previsões (PB-16)
O teste `ValidacaoPrevisoesTest` gera 600 leituras válidas por sensor (simulação com semente fixa, então o resultado é reprodutível). Para cada instante, ele prevê a leitura seguinte usando só o histórico até ali e compara com o valor real. Rode `mvnw.cmd test -Dtest=ValidacaoPrevisoesTest` para reproduzir.

| Sensor | Estratégia | Previsões | MAE | Acerto do nível | Falsos alarmes | Alertas perdidos |
|---|---|---|---|---|---|---|
| chuva | Média móvel (5 leituras) | 595 | 7,42 mm/h | 65,5% | 69 | 28 |
| chuva | Comparação com o período anterior (3 + 3) | 594 | 9,55 mm/h | 56,9% | 20 | 89 |
| nível do córrego | Média móvel (5 leituras) | 595 | 0,45 m | 74,5% | 43 | 74 |
| nível do córrego | Comparação com o período anterior (3 + 3) | 594 | 0,55 m | 73,2% | 69 | 39 |
| umidade do solo | Média móvel (5 leituras) | 595 | 6,76 % | 73,4% | 52 | 52 |
| umidade do solo | Comparação com o período anterior (3 + 3) | 594 | 7,95 % | 69,7% | 65 | 41 |

*MAE = erro médio absoluto. Acerto do nível = o nível previsto (nenhum, ATENÇÃO, ALERTA ou CRÍTICO) é igual ao real. Falso alarme = previu alerta e o real ficou abaixo de ATENÇÃO. Alerta perdido = não previu e o real atingiu ATENÇÃO ou mais.*

**O que os números dizem:** a média móvel erra menos em todos os sensores, mas na chuva perde menos alertas e dá mais alarmes falsos. A comparação reage mais rápido a mudanças e, por isso, exagera quando a variação é ruído. Nenhuma das duas acerta o nível em mais de 75% das vezes. É por isso que o sistema emite um único alerta com o nível mais grave **e** mostra as duas estimativas: quem decide vê a divergência. Também é por isso que os limiares são tratados como hipótese (ver Reflexão crítica). Os dados são simulados; com dados reais, esses números precisariam ser recalculados.

## 8. Decisões da equipe
- **JDBC puro em vez de Spring Boot:** o enunciado dá nota cheia aos dois. Com JDBC cada consulta fica explícita (PreparedStatement e try-with-resources), o que facilita explicar na conversa de correção, e o projeto roda com um único JAR.
- **H2 em arquivo:** o histórico precisa sobreviver ao reinício, e a correção não exige instalar nada.
- **Exceções checadas** para problemas de dado e de fonte: o compilador obriga quem chama a tratar. `PersistenciaException` é não checada porque nenhuma camada intermediária consegue se recuperar de banco fora do ar; quem informa o usuário é o menu.
- **`SensorSimulado` como classe-mãe dos sensores:** evita repetir a simulação de falha e de leitura espúria em cada sensor; cada filho implementa só a sua "física" (`proximoValor`).
- **Mínimo de 5 leituras** para prever (6 na comparação). Abaixo disso, a estratégia responde "sem previsão" em vez de chutar.
- **Um alerta por sensor por rodada**, com o nível mais grave entre as estratégias, para não duplicar avisos sobre o mesmo fato.
- **Injeção de dependência no `ServicoMonitoramento`:** DAOs, estratégias e relógio entram pelo construtor, o que permite testar a lógica de alerta com Mockito, sem banco.

## 9. Reflexão crítica
**Quando o sistema deve se calar?** Quando não há histórico suficiente. Com menos de 5 leituras, uma média é dominada por um único valor atípico, e a comparação entre períodos precisa de duas janelas completas (6 leituras). Por isso as estratégias devolvem "sem previsão" e nenhum alerta é emitido. Também deveria se calar, ou ao menos avisar que a confiança é baixa, quando um sensor passa muito tempo sem responder ou quando muitas leituras são descartadas: um buraco no histórico faz a média parecer mais calma do que a realidade. Uma melhoria futura seria exigir que as leituras usadas sejam recentes (por exemplo, da última hora), e não apenas as últimas gravadas.

**O que os dados não representam?** Um pluviômetro mede a chuva em um ponto, mas chuvas convectivas de verão variam muito em poucos quarteirões. Um sensor por região diz pouco sobre uma rua específica. A régua de nível mostra o córrego naquele ponto, não a capacidade das galerias entupidas rio acima. Os dados também não mostram quem é mais vulnerável (idosos, pessoas com deficiência, moradias precárias), nem fatores como lixo acumulado e obras. Além disso, neste protótipo os valores são **simulados** e os limiares são **de referência**, não calibrados com o histórico real do território. Antes de qualquer uso real, seria preciso validá-los com dados oficiais e com a Defesa Civil.

**Por que o alerta apoia a decisão, em vez de decidir?** Porque as consequências são assimétricas e dependem de contexto que o sistema não tem. Um falso alarme que manda retirar moradores custa confiança: na próxima vez, menos gente atende. Um alerta perdido pode custar vidas. Quem está no território sabe se a rua já foi limpa, se há uma escola funcionando ou se a ponte está interditada. Por isso cada alerta mostra o porquê (sensor, valor previsto, limite, estratégia) e termina com "a decisão final é da equipe responsável": a pessoa pode concordar, discordar ou pedir mais informação, e continua responsável pela escolha.

## 10. Limitações conhecidas
- O volume do piscinão e as alterações de dados do território e de limiares ficam em memória: voltam ao padrão ao reiniciar. O histórico de medições e alertas é persistido.
- As medições usam o relógio real: várias leituras seguidas (vários ciclos) ficam com horários muito próximos. Na simulação, cada ciclo representa uma nova leitura, não uma hora.
- O banco H2 em arquivo aceita um processo por vez: não abra duas janelas do programa ao mesmo tempo.

## 11. Uso de IA
Usamos IA generativa como parceira de desenvolvimento: Claude Code (Anthropic) e Gemini (Google, pelo Antigravity CLI). Aqui declaramos onde ela entrou e o que ficou com a equipe.

**O que a IA fez:** escreveu a maior parte do código a partir das nossas instruções (domínio, exceções, persistência JDBC, serviço e menu), os testes JUnit/Mockito e a primeira versão deste README. Também atuou como revisora independente, comparando o projeto com o enunciado item a item.

**O que a equipe fez:**
- **Definiu o objetivo e as regras de trabalho:** seguir o enunciado à risca, não inventar dados nem fontes, não publicar nada sem nossa autorização e só aceitar o resultado com nota 10 na revisão.
- **Acompanhou as etapas e validou as decisões:** recorte de chuva intensa e alagamento, caminho JDBC + H2, os três padrões criacionais e um território fictício descrito por região, sem dados pessoais.
- **Revisou executando o sistema, não só lendo:** `mvnw clean package` com 33 testes passando, uso de todas as opções do menu, reinício do programa para confirmar que o histórico continua no banco e exportação do CSV.
- **Pediu uma revisão final de cada requisito do enunciado.** Ela encontrou um texto de explicação errado na previsão por comparação ("estável em 0,0" quando a variação era zero), que foi corrigido.

**Responsabilidade por parte do código:** cada integrante responde pela parte indicada na seção 12. Antes da entrega, cada um estuda a sua parte para conseguir explicá-la na conversa de correção.

## 12. Divisão de tarefas
| Integrante | Tarefas |
|---|---|
| Rafael do Nascimento Silva | `dominio/` (componentes, sensores, `DadosTerritorio`, `Medicao`), `excecao/`, menu (`SistemaMonitoramento`) com captura de exceções, exportação CSV |
| Guilherme Morais de Assis | `persistencia/` (`ConexaoBanco`, `MedicaoDAO`, `AlertaDAO`, `schema.sql`), estratégias de previsão, `FabricaPrevisao`, `ConfiguracaoSistema`, alertas, `RelatorioBuilder` e testes JUnit/Mockito |

# Pesquisa de conteúdo para uma versão local

Pesquisa realizada em 18/10/2026. Objetivo: encontrar referências públicas para
recriar currículo, formatos de exercícios e materiais com fidelidade ao
Duolingo, sem depender dos seus servidores durante o uso.

## Resultado principal

Há documentação oficial suficiente para orientar a estrutura pedagógica e os
formatos de exercícios. Também existem corpora de pesquisa com conteúdo real
publicados pelo Duolingo. **Não foi localizado nesta pesquisa um pacote público
que reúna todos os cursos atuais, exercícios, respostas, áudios e regras de
personalização.** A busca foi limitada às fontes abaixo; isso não prova que
tal material não exista em nenhum lugar.

O curso-alvo ainda precisa ser escolhido: idioma estudado e idioma de origem.
Inglês para falantes de português foi examinado como exemplo, sem assumir que
seja o curso desejado pelo usuário. As fontes atuais também podem divergir do
conteúdo entregue pelo servidor ao APK 6.101.2 e à conta do usuário.

**Atualização:** o usuário escolheu inglês para falantes de português. O
aprofundamento e o estado de acesso ao corpus estão em
`docs/INGLES-PARA-FALANTES-PORTUGUES.md`. Os dados do Dataverse foram
catalogados pela API; o download exige um formulário de visitantes e ainda
não foi realizado.

**Atualização posterior:** o formulário foi preenchido e enviado com
autorização do usuário. O download e a importação local de inglês–português
foram concluídos. A seção de acesso e a entrega descritas no levantamento
inicial abaixo registram a situação anterior; o estado atual está em
`reports/staple-import.json` e `content/en-pt/README.md`.

## Fontes mais úteis

| Fonte | O que foi verificado | Utilidade e limite |
|---|---|---|
| [Guia oficial do aplicativo](https://blog.duolingo.com/duolingo-101-how-to-learn-a-language-on-duolingo/) | Unidades com objetivos de comunicação; combinação de vocabulário, gramática e quatro habilidades; prática personalizada | Referência geral do fluxo, sem inventário completo de exercícios |
| [Mudanças no currículo](https://blog.duolingo.com/duolingo-updates/) | Uso de metas CEFR e exemplos de habilidades por nível | Define progressão de dificuldade; não estabelece o alcance exato de cada curso |
| [Caminho e guias de unidade](https://blog.duolingo.com/new-duolingo-home-screen-design/?region=central-and-eastern-europe) | Integração de revisão, histórias e explicações ao caminho | Fonte histórica de desenho do produto; conferir detalhes na versão instalada |
| [Miniunidades intermediárias](https://blog.duolingo.com/intermediate-mini-units/) | Organização mais curta, combinando conteúdo novo e atividades de aplicação | Mostra que a estrutura continua mudando |
| [Duolingo Research](https://research.duolingo.com/) | Índice de pesquisas, dados e ferramentas | Ponto de entrada oficial para dados reais; não é exportação dos cursos |
| [STAPLE 2020](https://sharedtask.duolingo.com/) | Frases e múltiplas traduções aceitas, originárias de cinco cursos | Fonte concreta de conteúdo histórico, sem currículo completo ou áudios |
| [SLAM 2018](https://sharedtask.duolingo.com/2018.html) | Dados históricos de exercícios e erros em três combinações de idiomas | Útil para estudar formatos e aprendizagem, sem equivalência ao curso atual |
| [Half-life regression](https://github.com/duolingo/halflife-regression) | Código de pesquisa para modelar memória e revisão | Referência histórica; não prova qual algoritmo o aplicativo atual usa |
| [Catálogo comunitário](https://duolingodata.com/) | Listagens de cursos e estruturas de unidades | Fonte não oficial; deve ser conferida contra o app/conta-alvo |

## Conteúdo real: STAPLE

A publicação oficial de 2020 descreve frases em inglês com traduções para
português, húngaro, japonês, coreano e vietnamita. Para o par inglês–português,
a tabela informa **4.000 enunciados de treino e 526.466 traduções aceitas**.
Há ainda conjuntos de desenvolvimento e teste com 500 enunciados cada.
Essas traduções são variantes das respostas, **não centenas de milhares de
lições diferentes**. A amostragem vem de cursos daquela época.
[Fonte oficial e tabela](https://sharedtask.duolingo.com/#data).

O [código auxiliar oficial](https://github.com/duolingo/duolingo-sharedtask-2020/)
tem leitores e avaliação. O link oficial dos dados leva ao
[Harvard Dataverse](https://dataverse.harvard.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/38OJR6).
A página do Dataverse exigiu verificação de navegador nesta pesquisa, portanto
os arquivos e seus termos específicos não foram examinados nem importados.
Não se deve atribuir aos dados a licença de um repositório de código diferente.

**Aplicação proposta:** avaliar esse corpus como base de frases e variantes de
correção, quando o par de idiomas for compatível. A classificação por unidade,
nível, vocabulário e gramática precisará de trabalho adicional. Não foi
implementado importador nem demonstrada cobertura dos temas desejados.

## Exemplo de mapa de curso

O [mapa comunitário de inglês para falantes de português](https://duolingodata.com/dat/enfpt1005v2.html)
se identifica como uma fotografia de setembro de 2026, com oito seções e
progressão introdutória até B2. Ele apresenta temas e contagens de atividades.
No início aparecem, por exemplo, situações de alimentação, apresentações e
viagem, além de gramática básica. Isso ajuda a ordenar objetivos, mas não
fornece todas as perguntas, respostas e gravações. O retrato não foi validado
contra a conta do usuário e não deve ser tratado como currículo oficial fixo.

## Formatos que a versão local precisaria oferecer

| Capacidade observada nas fontes | Dados necessários na recriação | Referência |
|---|---|---|
| Associar palavras e preencher lacunas | Pares, alternativas, respostas e contexto | [Leitura](https://blog.duolingo.com/covering-all-the-bases-duolingos-approach-to-reading-skills/) |
| Tradução digitada e produção escrita | Enunciado, traduções aceitas, pistas e explicações | [Escrita](https://blog.duolingo.com/covering-all-the-bases-duolingos-approach-to-writing-skills/) |
| Escuta e compreensão progressiva | Áudio local, transcrição e perguntas | [Escuta](https://blog.duolingo.com/covering-all-the-bases-duolingos-approach-to-listening-skills/) |
| Repetição falada e diálogos | Frases-alvo, áudio e mecanismo local de avaliação | [Fala](https://blog.duolingo.com/covering-all-the-bases-duolingos-approach-to-speaking-skills/) |
| Histórias com compreensão | Roteiro, falas, áudio e questões | [Leitura e Stories](https://blog.duolingo.com/covering-all-the-bases-duolingos-approach-to-reading-skills/) |
| Episódios de rádio com verificações | Roteiro, gravações e exercícios intercalados | [DuoRadio](https://blog.duolingo.com/duoradio-listening-practice/) |
| Situações interativas do cotidiano | Cenários, escolhas, diálogos e consequências | [Adventures](https://blog.duolingo.com/adventures/) |
| Revisão de palavras e erros | Histórico local e seleção de atividades | [Prática](https://blog.duolingo.com/guide-to-duolingo-practice-hub/) |
| Revisão espaçada | Registro de desempenho e agenda de revisão | [Repetição espaçada](https://blog.duolingo.com/spaced-repetition-for-learning/) |

A coluna de dados necessários é uma proposta de implementação, não uma
afirmação sobre o formato interno dos servidores. A documentação pública
descreve as experiências; ela não disponibiliza todos os respectivos assets.
A presença de uma função no blog não comprova disponibilidade em todo curso,
plataforma ou conta. Fala e conversação totalmente locais precisarão de
validação específica de modelos, idiomas, tamanho e desempenho no aparelho.

## Outros assuntos além de idiomas

- **Matemática:** a publicação oficial descreve operações, frações, decimais,
  medidas e geometria. Pode orientar um banco local de problemas gerados por
  regras, sem representar todo o currículo atual.
  [Fonte](https://blog.duolingo.com/duolingo-launches-math-app/).
- **Música:** fundamentos de ritmo, notas, teclado, leitura musical e percepção
  sonora. A recriação exige exercícios e áudio locais próprios ou utilizáveis.
  [Fonte](https://blog.duolingo.com/music-course/).
- **Xadrez:** progressão desde movimentos das peças até estratégias, por meio
  de exercícios interativos. Posições, regras e dificuldade exigem implementação.
  [Fonte](https://blog.duolingo.com/chess-course/).

Esses artigos têm datas e alcances distintos; não foram usados para afirmar
quantidades atuais de unidades ou disponibilidade universal.

## Como buscar fidelidade com rastreabilidade

1. Escolher o curso e uma referência de conteúdo/versionamento. Registrar o
   que foi confirmado no aplicativo e o que é somente descrição pública.
2. Montar um mapa de objetivos e pré-requisitos, associando cada tema à fonte.
3. Separar conteúdo oficial disponível, amostras históricas e conteúdo novo
   produzido para preencher lacunas. Não rotular conteúdo novo como original.
4. Para cada exercício, guardar enunciado, respostas aceitas, explicação,
   habilidade, dificuldade, mídia local e origem.
5. Validar uma unidade completa offline, com correção, revisão e progresso.
   Depois ampliar o acervo com revisão linguística e teste de dificuldade.
6. Comparar cobertura de temas e variedade de exercícios. Sem acesso ao banco
   completo, não prometer equivalência de todas as frases ou personalização.

## O que esta etapa entregou

Pesquisa e referências para currículo e experiências, identificação de um
corpus oficial especialmente relevante para inglês–português, e separação dos
materiais disponíveis das lacunas. Não foram baixados cursos completos,
áudios, dados de conta, corpora de exercícios ou material protegido por login.
Nenhum novo aplicativo offline foi implementado nesta etapa.

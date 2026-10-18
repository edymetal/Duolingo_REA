# Conteúdo local e independência do servidor

Inspeção estática complementar em 18/10/2026, sobre a extração local do APK
6.101.2. Esta análise usa arquivos decodificados por Apktool e buscas locais;
não é uma nova evidência emitida pelo REA.

## Observações verificadas

- `assets/courses_and_languages.json` contém uma lista `courses`. O primeiro
  registro tem as chaves `id`, `learning_language_id`, `from_language_id` e
  `status`; esse registro é um catálogo, não uma lição.
- `assets/hardcoded_sessions/` contém 18 JSONs: 12 em `math`, 4 em `chess` e 2
  em `landscape` (arquivos de música). Não há uma pasta de sessões de idiomas
  nesse conjunto específico. Isso não prova ausência de conteúdo em todos os
  demais formatos ou locais do aplicativo.
- `smali/com/duolingo/core/offline/SessionMetadata.smali` inclui os campos
  textuais `downloadedAppVersionString` e `downloadedTimestamp` em sua
  representação de metadados.
- `smali/com/duolingo/core/offline/OfflineManifest.smali` contém referência a
  `sessionMetadata` e identificadores de sessão offline.
- Existe o modelo desmontado
  `smali_classes3/com/duolingo/data/session/SessionRouteParams$Lesson$Request.smali`.
  Sua existência, isoladamente, não demonstra um endpoint HTTP específico.

## Conclusão e limites

O material examinado não fornece um acervo completo dos cursos de idiomas.
Ele contém interface, lógica, recursos, catálogo de cursos e algum conteúdo
de exercícios local. Os metadados de download e estruturas offline são
compatíveis com conteúdo obtido e armazenado posteriormente. A dependência
de conteúdo remoto é uma inferência; o fluxo completo de rede das lições
ainda não foi rastreado nem observado nesta investigação.

Não é correto dizer que toda lição necessariamente exige conexão a cada
execução: pode haver conteúdo previamente baixado. Também não é correto
tratar a instalação completa dos APKs como uma cópia de todos os cursos.
Nenhum cache ou dado privado do aplicativo no celular foi copiado.

Para uma versão independente dos servidores do fornecedor, será necessário
definir um conjunto local de lições e implementar ou adaptar os serviços e o
armazenamento de progresso. Desativar a internet, por si só, não estabelece
essa independência. Continua em aberto se o destino desejado é totalmente
offline no Android ou com um backend local no computador.

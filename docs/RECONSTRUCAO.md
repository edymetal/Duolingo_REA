# Base editável do APK

O material fornecido permite desmontar e reconstruir o APK **base** do
Duolingo 6.101.2 (2476), pacote `com.duolingo`. O projeto local contém recursos
XML, assets e dez conjuntos de classes Smali em `decoded/`. Isso permite
alterações no aplicativo empacotado. Não constitui uma reimplementação
independente completa nem recupera o projeto Kotlin/Java original.

## Limitação do arquivo fornecido

O manifesto declara `android:requiredSplitTypes="base__abi,base__density"`
e `com.android.vending.splits.required=true`. Os APKs adicionais de arquitetura
e densidade não estavam na pasta APK. Não há bibliotecas `lib/` no APK base.
O Apktool também relatou referências de recursos não resolvidas na decodificação.
Não se removeu a exigência de splits: removê-la não recuperaria os arquivos.

Uma instalação completa depende dos splits da **mesma versão**. APKs
reassinados também precisam de uma assinatura consistente entre base e splits.
Os serviços remotos, contas, cursos e verificações do fornecedor não são
reconstruídos a partir do APK. O funcionamento deles com outra assinatura não
foi estabelecido.

## Reproduzir no Windows

Use PowerShell, um JDK 17+ e Android SDK Build Tools. O script encontra o JBR
do Android Studio e o SDK desta máquina; em outras máquinas informe
`-JavaHome` e `-AndroidSdk` ou configure `JAVA_HOME` e `ANDROID_HOME`.

```powershell
.\scripts\apk.ps1 Setup
.\scripts\apk.ps1 Decode
.\scripts\apk.ps1 Build
.\scripts\apk.ps1 Verify
python .\scripts\inspect_apk.py
python .\scripts\verify_rebuild.py
```

Coloque o arquivo original em `APK/Duolingo.apk`. O SHA-256 é validado antes da
decodificação. O script se recusa a sobrescrever uma pasta `decoded` existente.
Nesta máquina a decodificação já foi feita; comece pelas edições ou por Build.

O Apktool 3.0.3 é baixado da publicação oficial e verificado por SHA-256:
`dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423`.
Fonte: https://github.com/iBotPeaches/Apktool/releases/tag/v3.0.3

O resultado é `dist/Duolingo-base-editable.apk`. A assinatura de desenvolvimento
é gerada localmente em `.tools/development.keystore`, com senha de teste
`android`. Ela não é a assinatura do fornecedor e não deve ser usada para
distribuição. Preserve essa chave local para manter a mesma assinatura nas
próximas reconstruções. O script não instala nem desinstala apps.

## Onde alterar

| Material | Local |
|---|---|
| Textos e traduções | `decoded/res/values*/strings.xml`, `plurals.xml` |
| Cores e estilos | `decoded/res/values/colors.xml`, `styles.xml` |
| Imagens e layouts incluídos | `decoded/res/` |
| Animações, áudio e dados empacotados | `decoded/assets/` |
| Permissões, componentes e aliases | `decoded/AndroidManifest.xml` |
| Bytecode desmontado | `decoded/smali*/` |
| Versão e configuração de reconstrução | `decoded/apktool.yml` |

Edite os arquivos em `decoded/` e copie os arquivos alterados para `overrides/`,
preservando os caminhos relativos. Build aplica esses arquivos à cópia local.
O Git versiona as substituições, scripts e relatórios; APKs, bytecode e assets
extraídos, ferramentas e chaves ficam locais e podem ser regenerados.

Não copie `decoded/original/META-INF` sobre o APK reconstruído: as assinaturas
originais deixam de valer após alterações. O pacote continua `com.duolingo`;
uma assinatura diferente impede atualizar uma instalação oficial existente.
Não desinstale uma instalação com dados que precise preservar.

## Evidências e cobertura

`reports/rea-inspection.json` contém o resultado REA retido resumido. O bundle
completo de aproximadamente 40 MB está localmente em `.tools/rea-evidence.json`.
O REA encontrou 18.039 ocorrências no grafo; o inventário ZIP por Python lista
18.038 entradas. A contagem de ocorrências do grafo não deve ser tratada como
uma contagem de arquivos ZIP.

Evidência da inspeção: `ev_43202f48bdb950d28f76438273cd5b41d94185058f4202e17848c948a339e8af`.
Inventário: `ev_233038ae4578d1f25dd95f6ba0fd7820fa94063494c97bd5a1e0670f3b899171`.
SHA-256 original: `806c41968c9748f91afb77d6c08093c8ab6b0e2eefdbbb50c9b29c1bc7cd4baa`.

A ferramenta Android do REA retornou `capability_unavailable`, pois
`REA_JADX_MCP_JAR` não estava configurado. A decodificação foi feita com Apktool,
fora do REA, sem alterar o registro MCP. O inventário complementar identifica
explicitamente essa origem em `reports/apk-inventory.json`.

O manifesto aponta a aplicação para `com.duolingo.core.DuoApp`; o alias
`com.duolingo.app.LoginActivity` aponta para `com.duolingo.splash.LaunchActivity`.
Os namespaces desmontados incluem home, session, onboarding, streak, leagues,
stories, music, chess e math. Esses nomes orientam buscas, mas não demonstram
que todas as funções estão disponíveis ou foram executadas.

| Objetivo | Estado |
|---|---|
| Identificar e inventariar o arquivo via REA | Concluído |
| Disponibilizar recursos e bytecode editáveis do APK fornecido | Concluído localmente |
| Reconstruir, alinhar e assinar APK base | Concluído; assinatura v3 e alinhamento verificados |
| Comparar classes e assets da referência | 91.712 descritores de classe e 610 assets preservados |
| Instalar original no emulador Android 16 | Tentativa recusada: INSTALL_FAILED_MISSING_SPLIT |
| Recuperar código-fonte Kotlin/Java original | Não recuperado |
| Obter splits de arquitetura e densidade | Pendente |
| Recriar backend e conteúdo remoto | Não disponível no APK |
| Clone independente completo com paridade funcional | Não concluído |

Consulte os relatórios de validação para o resultado da reconstrução e da
tentativa de instalação. Assinatura e alinhamento válidos não comprovam
paridade funcional.

A tentativa real usou o AVD `Medium_Phone_API_36.1`, Android 16, ABI x86_64,
serial `emulator-5554`. O sistema concluiu o boot, mas recusou o APK original
com `INSTALL_FAILED_MISSING_SPLIT: Missing split for com.duolingo`. Nenhuma
execução do Duolingo foi observada. Os detalhes estão em
`reports/emulator-installation.json`.

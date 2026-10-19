# Atividades do Lingo Edy

As sete telas enviadas pelo usuário foram usadas como referência para a prática
**Cores e roupas**. Abra pelo ícone de halteres na barra inferior ou pela quarta
estrela da trilha. As três lições anteriores de bebidas continuam disponíveis:
agora são quatro lições e 25 exercícios locais.

| Referência | Implementação |
|---|---|
| Escolha a tradução | Personagem, balão, três alternativas e botão Continuar |
| Complete o espaço vazio | Frase com lacuna, personagem e três alternativas |
| Repita a frase | Áudio local, personagem animado, microfone e transcrição offline |
| Complete a frase com imagem | Ilustração, lacuna preenchida ao tocar e palavras para escolher |
| Selecione a imagem | Palavra com áudio e quatro cartões ilustrados |
| Traduza com palavras | Personagem, frase com áudio e banco com distratores; toque em uma palavra da resposta para devolvê-la |
| Combine os pares | Duas colunas embaralhadas, pares desativados ao acertar e erro recuperável |

O cabeçalho usa progresso arredondado, a fonte original e o fundo escuro. Os
botões têm estados desativado, selecionado, correto e incorreto, com relevo.
As animações de Óscar, Lily e Zari são os arquivos Rive extraídos do APK local.
O runtime Rive Canvas e seu WebAssembly estão no próprio APK; a WebView permite
somente os recursos locais interceptados, com acesso de rede bloqueado e sem
permissão Android de internet. As ilustrações de chapéu, bolsa, tintas e bandeira
foram recriadas como desenhos vetoriais no código. Não são arquivos recuperados
do servidor. Ainda pode haver diferenças de animação, pose e proporções.

## Fala sem servidor

Vosk Android 0.3.47, JNA 5.13.0 e o modelo inglês
`vosk-model-small-en-us-0.15` acompanham o APK. O modelo é copiado para a área
privada do aplicativo no primeiro uso do microfone; não há download no telefone.
A preparação pode levar alguns segundos. O áudio do microfone e a transcrição
não são enviados a um servidor e não são gravados em arquivos pelo aplicativo.

A permissão de microfone é solicitada somente ao tocar para gravar. É possível
negar e continuar. **Não posso falar agora** troca o exercício por compreensão
de áudio com alternativas; não concede um acerto automático. O microfone para
ao sair da tela ou colocar o aplicativo em segundo plano.

A correção compara o texto reconhecido com a frase esperada, desconsiderando
maiúsculas, espaços e pontuação. Não replica o avaliador fonético do Duolingo
nem comprova a precisão para todos os sotaques. O teste automatizado utiliza
voz sintética conhecida; a experiência com voz humana ainda merece avaliação
no aparelho do usuário.

O APK ficou maior porque inclui o modelo e bibliotecas para ARM64, ARMv7, x86
e x86_64. As versões e hashes das dependências estão em
`scripts/activity-dependencies.json`. O primeiro preparo no computador baixa os
arquivos desses endereços e verifica os hashes; compilações seguintes usam o
cache em `.tools/activity-deps/`. O reconhecimento no telefone continua offline.
As licenças dos componentes estão no APK e no menu Sobre → Licenças.

## Preparar e compilar

É necessário manter a extração anterior em `decoded/res/` e os dados e áudios
das três lições originais. Para gerar a prática nova:

```powershell
python scripts/prepare_visual_assets.py
python scripts/prepare_activity_assets.py
.\scripts\generate_unit_audio.ps1 -UnitRoot 'D:/Projetos/Web/Desenvolvimento/Pessoal/Duolingo_REA/content/en-pt/generated/reference-audio' -ReportName reference-activities-audio.json
.\scripts\build_offline_app.ps1 -Install
```

Edite `content/en-pt/reference-activities.json` para mudar as perguntas,
alternativas, pares e palavras. Depois prepare os assets, regenere os áudios e
compile. A prática é uma curadoria local separada do corpus STAPLE; as frases
originais desse corpus permanecem intactas.

## Verificação

```powershell
python -m unittest discover -s tests -v
python scripts/test_reference_activities.py
python scripts/test_source_activity_refresh.py
.\scripts\test_speech_android.ps1
```

O teste de interface percorre os sete formatos em modo avião, testa respostas
erradas, correção, desfazer palavras, pares incorretos e persistência. A etapa
de fala verifica a inicialização do modelo e do microfone e usa a alternativa
de escuta para concluir o percurso. O teste de instrumentação separado executa
o motor nativo Android com o WAV conhecido, um áudio diferente e silêncio.
Ele exige que o modelo já tenha sido instalado pelo uso do microfone.

Relatórios: `reports/reference-activities-validation.json`,
`reports/speech-android-validation.json`,
`reports/source-activity-refresh-validation.json` e `reports/activity-assets.json`.
O teste das atividades anteriores percorre os seis exercícios da primeira lição
de bebidas, cobrindo tradução, escuta e montagem de frases com a nova interface,
e confere a pontuação salva após reiniciar. Ele não repete as outras duas lições.
As capturas `reports/activity-*.png` mostram a interface no emulador.
Os testes não apagam dados; podem acrescentar progresso e erros de demonstração.

## Capturas no emulador

[Início](../reports/activity-home.png) ·
[Tradução](../reports/activity-choice.png) ·
[Lacuna](../reports/activity-cloze.png) ·
[Fala](../reports/activity-speaking.png) ·
[Frase com imagem](../reports/activity-image-cloze.png) ·
[Seleção de imagem](../reports/activity-images.png) ·
[Banco de palavras](../reports/activity-bank.png) ·
[Pares](../reports/activity-pairs.png)

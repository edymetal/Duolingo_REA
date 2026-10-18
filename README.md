# REA — instalação local

REA (Reverse Engineer Anything) instalado em **18/10/2026** a partir de
[morluto/rea](https://github.com/morluto/rea).

- Diretório: `D:\Program Files\REA-GitHub`
- Versão: `6.7.0`
- Commit do código-fonte: `23f984b1a` (`fix(tests): restore Android prerequisite verification`)
- Runtime utilizado: Node.js `22.23.3`, npm `10.9.9`.
- Dependências instaladas com `npm ci`; compilação com `npm run build:cached`.
- Componente nativo Windows x64 restaurado do pacote oficial `rea-agents@6.7.0`,
  conforme `native/windows/README.md`, com versão e SHA-256 verificados.
- Comando local `rea.cmd` criado e diretório adicionado ao PATH do usuário.
- MCP e skill configurados para o Codex pelo setup oficial.

## Uso

Reinicie o Codex para carregar o servidor MCP e a skill. Abra um novo terminal
para obter o PATH atualizado. Se necessário, execute o comando pelo caminho completo:

```powershell
& 'D:\Program Files\REA-GitHub\rea.cmd' --version
& 'D:\Program Files\REA-GitHub\rea.cmd' --help
& 'D:\Program Files\REA-GitHub\rea.cmd' mcp doctor --json
& 'D:\Program Files\REA-GitHub\rea.cmd' analyze-javascript-application 'D:\caminho\do\aplicativo' --json
```

O registro MCP aponta diretamente para `scripts\rea.mjs` no clone local, usando
`D:\Program Files\nodejs\node.exe`. O setup preservou as outras configurações e
criou o backup `C:\Users\Edney\.codex\config.toml.rea.backup`.
A skill foi instalada em `C:\Users\Edney\.agents\skills\reverse-engineer-anything`.

## Validação executada

- Compilação: três tarefas concluídas com sucesso.
- Verificação do componente nativo: `ok: true`.
- Setup do Codex: `ready`; registro e skill alinhados.
- MCP via stdio: `healthy: true`, 207 ferramentas, seis prompts e chamada
  `binary_session` concluída com sucesso.
- Análise estática de uma aplicação JavaScript mínima de teste: concluída,
  com geração de evidência e grafo de módulos.

Nenhum provedor de análise profunda está configurado. Recursos que dependem de
Ghidra, IDA, JADX ou outros programas exigem suas próprias instalações e
configurações. A análise JavaScript validada funciona sem esses provedores.

## Manutenção

Este é um clone do código-fonte; atualize-o pelo Git, confira os requisitos da nova
versão e reconstrua o runtime. Ao mudar a versão no Windows, restaure também o
componente nativo da versão correspondente, seguindo o guia oficial.
Depois atualize a integração e reinicie o Codex:

```powershell
Set-Location 'D:\Program Files\REA-GitHub'
git pull --ff-only
npm.cmd ci
# Restaurar o componente nativo da versão exata antes de compilar.
npm.cmd run build:cached
node scripts/rea.mjs setup --client codex --dry-run
node scripts/rea.mjs setup --client codex --yes
node scripts/rea.mjs mcp doctor --json
```

Nesta máquina, o npm inicialmente retornou `UNABLE_TO_VERIFY_LEAF_SIGNATURE`.
A instalação funcionou usando os certificados confiáveis do Windows, com
validação TLS mantida. Se o erro reaparecer, defina na sessão de instalação:

```powershell
$env:NODE_OPTIONS = '--use-system-ca'
```

Código compilado, dependências, componente nativo e configurações pessoais ficam
fora deste repositório. Este documento registra a instalação usada pelo projeto.

# Material local: inglês para falantes de português

O arquivo `generated/staple.en-pt.jsonl` contém o recorte inglês–português
importado do STAPLE 2020: 5.000 enunciados e 654.625 variantes de tradução.
Ele foi gerado localmente após o download autorizado pelo fluxo do Harvard
Dataverse. Os arquivos gerados e o arquivo original ficam fora do Git.

Cada linha JSON contém enunciado, traduções aceitas e respectivos pesos,
identificador original, divisão de pesquisa e origem. Unidade, nível CEFR e
áudio permanecem nulos: não foram inventados nem atribuídos por aproximação.
Isso ainda é um banco de referência, não um curso completo executável.

Para regenerar a partir do arquivo baixado:

```powershell
python scripts/import_staple.py
```

O script valida o MD5 publicado, seleciona somente os três arquivos `gold`
do par `en_pt`, rejeita registros inválidos e identificadores repetidos, e
confere a quantidade de enunciados por conjunto. Nenhum arquivo do TAR é
extraído para um caminho fornecido pelo arquivo. Baselines de tradução
automática e os outros idiomas não são importados.

[Relatório da importação](../../reports/staple-import.json).

## Origem e atribuição

Duolingo, 2020, *Data for the 2020 Duolingo Shared Task on Simultaneous
Translation And Paraphrase for Language Education (STAPLE)*, Harvard
Dataverse, V6. [doi:10.7910/DVN/38OJR6](https://doi.org/10.7910/DVN/38OJR6).

Licença declarada pelo conjunto: [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/).
Transformação realizada: recorte `en_pt` e conversão dos blocos de texto em
JSONL, preservando texto, pesos e divisão de pesquisa. O diretório gerado
também contém um arquivo de atribuição junto dos dados.

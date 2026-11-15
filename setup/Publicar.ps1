# Publicação completa do Smart Finance (ver docs/ARQUITETURA.md > Publicação).
# Uso: powershell -File setup/Publicar.ps1 -Versao 3.4.4 [-Resumo "texto das notas"]
# Requisitos: worktree da main com árvore limpa; .NET SDK; Inno Setup 6;
# credencial GitHub no Git Credential Manager.

param(
    [Parameter(Mandatory = $true)]
    [string]$Versao,
    [string]$Resumo = "Correções e melhorias de interface."
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
Set-Location $raiz

function Passo($mensagem) { Write-Host "`n=== $mensagem ===" -ForegroundColor Cyan }

if ($Versao -notmatch '^\d+\.\d+\.\d+$') { throw "Versão inválida: '$Versao' (esperado X.Y.Z)" }

Passo "Estado do repositório"
$branch = git rev-parse --abbrev-ref HEAD
if ($branch -ne 'main') { throw "Publicar a partir da branch main (atual: $branch)." }
$pendencias = git status --porcelain
if ($pendencias) { throw "Árvore com mudanças não commitadas:`n$pendencias" }
git pull --ff-only origin main | Write-Host

Passo "Bump de versão para $Versao"
$csproj = "$raiz\src\SmartFinance\SmartFinance.csproj"
$iss = "$raiz\setup\SmartFinance.iss"
(Get-Content $csproj -Raw) -replace '<Version>[^<]+</Version>', "<Version>$Versao</Version>" |
    Set-Content $csproj -NoNewline
(Get-Content $iss -Raw) -replace '#define VersaoApp "[^"]+"', "#define VersaoApp `"$Versao`"" |
    Set-Content $iss -NoNewline
$manifest = "$raiz\src\SmartFinancepp.manifest"
$versaoManifest = 'version="' + $Versao + '.0"'
(Get-Content $manifest -Raw) -replace 'version="[0-9.]+\.0"', $versaoManifest |
    Set-Content $manifest -NoNewline
git add $csproj $iss $manifest
if (git status --porcelain -- $csproj $iss $manifest) {
    git commit -m "Bump version to $Versao"
} else {
    Write-Host "versão já está em $Versao (nada a commitar)"
}

Passo "Build Release e testes unitários"
dotnet build SmartFinance.slnx -c Release
if ($LASTEXITCODE -ne 0) { throw "build falhou" }
dotnet test tests/SmartFinance.Tests -c Release --no-build
if ($LASTEXITCODE -ne 0) { throw "testes unitários falharam" }

Passo "Publicando executável e gerando instalador"
dotnet publish src/SmartFinance -c Release -r win-x64 --self-contained true `
    -p:PublishSingleFile=true -p:PublishReadyToRun=true `
    -p:IncludeNativeLibrariesForSelfExtract=true -o dist
if ($LASTEXITCODE -ne 0) { throw "publish falhou" }

$iscc = "$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe"
if (-not (Test-Path $iscc)) { throw "ISCC não encontrado: $iscc" }
& $iscc "$raiz\setup\SmartFinance.iss"
if ($LASTEXITCODE -ne 0) { throw "Inno Setup falhou" }

$caminhoSetup = "$raiz\setup\Output\SmartFinance-Setup-$Versao.exe"
if (-not (Test-Path $caminhoSetup)) { throw "instalador não gerado: $caminhoSetup" }
$hashExe = (Get-FileHash "$raiz\dist\SmartFinance.exe" -Algorithm SHA256).Hash.ToLower()
$hashSetup = (Get-FileHash $caminhoSetup -Algorithm SHA256).Hash.ToLower()
Write-Host "SHA-256 do instalador: $hashSetup"

Passo "Push do main"
git push origin main
if ($LASTEXITCODE -ne 0) { throw "push falhou" }

Passo "Criando release $Versao no GitHub"
$entrada = "protocol=https`nhost=github.com`n`n"
$cr = $entrada | git credential fill 2>$null
$token = ($cr | Where-Object { $_ -like 'password=*' }) -replace '^password=', ''
if (-not $token) { throw "credencial GitHub não encontrada no Git Credential Manager" }

$crlf = [char]96  # backtick: as notas precisam do formato `arquivo`: `hash` que o app parseia
$notas = @"
# Smart Finance $Versao

$Resumo

## Atualização da sua cópia antiga

O aplicativo verifica atualização 1x por dia e se atualiza sozinho. Se preferir, baixe e execute este instalador por cima — seus dados em %APPDATA%\SmartFinance são preservados.

## Verificação de integridade

SHA-256 do ${crlf}SmartFinance.exe${crlf}: ${crlf}$hashExe${crlf}
SHA-256 do ${crlf}SmartFinance-Setup-$Versao.exe${crlf}: ${crlf}$hashSetup${crlf}
"@

$corpo = @{
    tag_name         = $Versao
    target_commitish = 'main'
    name             = "Smart Finance $Versao"
    body             = $notas
} | ConvertTo-Json

$arquivoCorpo = Join-Path $env:TEMP "sf-release-$Versao.json"
[System.IO.File]::WriteAllText($arquivoCorpo, $corpo)

$resposta = curl.exe -s --max-time 30 -X POST `
    -H "Authorization: token $token" -H "Accept: application/vnd.github+json" `
    --data-binary "@$arquivoCorpo" `
    "https://api.github.com/repos/AlessandroCoranFilho123/smart-finance/releases"
$release = $resposta | ConvertFrom-Json
if (-not $release.id) { throw "falha ao criar release: $resposta" }
Remove-Item $arquivoCorpo -ErrorAction SilentlyContinue
Write-Host "release criada: id $($release.id), tag $($release.tag_name)"

Passo "Enviando instalador como asset"
$urlUpload = $release.upload_url -replace '\{\?.*$', ''
$envio = curl.exe -s --max-time 600 -X POST `
    -H "Authorization: token $token" -H "Content-Type: application/octet-stream" `
    --data-binary "@$caminhoSetup" "$urlUpload`?name=SmartFinance-Setup-$Versao.exe"
$asset = $envio | ConvertFrom-Json
if ($asset.state -ne 'uploaded') { throw "falha no upload do asset: $envio" }
Write-Host "asset enviado: $($asset.name) ($($asset.size) bytes)"

Passo "Verificação ponta a ponta"
$latest = curl.exe -s --max-time 30 `
    "https://api.github.com/repos/AlessandroCoranFilho123/smart-finance/releases/latest" | ConvertFrom-Json
if ($latest.tag_name -ne $Versao) { throw "releases/latest aponta para $($latest.tag_name), não $Versao" }
if (-not ($latest.assets | Where-Object { $_.name -eq "SmartFinance-Setup-$Versao.exe" })) {
    throw "asset não encontrado em releases/latest"
}
if ($latest.body -notmatch 'SmartFinance-Setup[^`]*`: `([0-9a-f]{64})' -or $Matches[1] -ne $hashSetup) {
    throw "hash das notas não confere com o publicado"
}

$baixado = Join-Path $env:TEMP "SmartFinance-Setup-$Versao-verificacao.exe"
curl.exe -sL --max-time 600 -o $baixado `
    "https://github.com/AlessandroCoranFilho123/smart-finance/releases/download/$Versao/SmartFinance-Setup-$Versao.exe"
$hashBaixado = (Get-FileHash $baixado -Algorithm SHA256).Hash.ToLower()
Remove-Item $baixado -ErrorAction SilentlyContinue
if ($hashBaixado -ne $hashSetup) { throw "SHA-256 do asset baixado difere do publicado" }

Write-Host "`n>>> Release $Versao publicada e verificada: $($latest.html_url)" -ForegroundColor Green
Write-Host ">>> O aplicativo entrega a atualização automaticamente na próxima checagem diária." -ForegroundColor Green

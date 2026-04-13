@echo off
set VERSION=2.1.0
:: Garante que o bat roda sempre na pasta onde ele esta
cd /d "%~dp0"
echo Diretorio atual: %CD%
echo === Smart Finance - Gerando instalador .exe ===
echo.
:: Passo 1: Compilar e copiar dependencias
echo [1/3] Compilando projeto...
call mvn clean package
if errorlevel 1 (
    echo ERRO: Falha na compilacao. Rode "mvn clean package" para ver detalhes.
    pause
    exit /b 1
)
:: Passo 2: Copiar o jar principal para a pasta de dependencias
echo [2/3] Copiando jar principal...
echo Verificando se o jar existe:
dir target\*.jar
copy /Y target\app-%VERSION%.jar target\dependency\app-%VERSION%.jar
if errorlevel 1 (
    echo ERRO: Nao foi possivel copiar o jar.
    pause
    exit /b 1
)
:: Passo 3: Gerar o instalador (saida em "installer\" fora do target)
echo [3/3] Gerando instalador .exe...
if not exist installer mkdir installer
jpackage ^
  --type exe ^
  --name SmartFinance ^
  --app-version %VERSION% ^
  --vendor "Alessandro" ^
  --icon "src\main\resources\app\icons\logo.ico" ^
  --input target\dependency ^
  --dest installer ^
  --main-jar app-%VERSION%.jar ^
  --main-class app.Main ^
  --module-path C:\javafx-jmods-21.0.9 ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.sql,java.naming,java.logging,java.prefs ^
  --java-options "-Dprism.order=sw" ^
  --java-options "--add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED" ^
  --java-options "--add-opens=javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED" ^
  --win-menu ^
  --win-shortcut
if errorlevel 1 (
    echo ERRO: jpackage falhou.
    pause
    exit /b 1
)
echo.
echo === Instalador gerado com sucesso! ===
echo Arquivo: installer\SmartFinance-%VERSION%.exe
echo.
pause

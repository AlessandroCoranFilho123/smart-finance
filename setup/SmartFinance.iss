; Inno Setup 6 — instalador do Smart Finance
; Requisito: Inno Setup 6 (ISCC). Build: ISCC setup\SmartFinance.iss (a partir da raiz)

#define NomeApp "Smart Finance"
#define VersaoApp "3.4.1"

[Setup]
AppId={{7E1B6C2A-9F4E-4C3B-8F2A-SMARTFINANCE31}
AppName={#NomeApp}
AppVersion={#VersaoApp}
AppPublisher=Alessandro Coran Filho
DefaultDirName={autopf}\Smart Finance
DefaultGroupName={#NomeApp}
DisableProgramGroupPage=yes
OutputDir=Output
OutputBaseFilename=SmartFinance-Setup-{#VersaoApp}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayIcon={app}\SmartFinance.exe
; Encerra o app em execução durante a atualização
CloseApplications=yes

[Languages]
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Files]
Source: "..\dist\SmartFinance.exe"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\{#NomeApp}"; Filename: "{app}\SmartFinance.exe"
Name: "{autodesktop}\{#NomeApp}"; Filename: "{app}\SmartFinance.exe"; Tasks: atalho_desktop

[Tasks]
Name: "atalho_desktop"; Description: "Criar atalho na área de trabalho"; GroupDescription: "Atalhos:"

[Run]
; Sem flags postinstall: roda também nas instalações silenciosas de atualização
; (F6), reabrindo o aplicativo após o instalador substituir os arquivos.
Filename: "{app}\SmartFinance.exe"; Description: "Executar o {#NomeApp}"; Flags: nowait runasoriginaluser

[UninstallDelete]
; Dados do usuário (%APPDATA%\SmartFinance) são PRESERVADOS na desinstalação.

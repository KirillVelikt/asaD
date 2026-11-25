; Inno Setup: установщик TerminalControl
[Setup]
AppName=Пульт Юмани
AppVersion=2.0
DefaultDirName={autopf}\TerminalControl
DefaultGroupName=Пульт Юмани
OutputDir=Output
OutputBaseFilename=TerminalControlSetup
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin

[Files]
Source: "..\dist\TerminalControl.exe"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Пульт Юмани"; Filename: "{app}\TerminalControl.exe"
Name: "{commondesktop}\Пульт Юмани"; Filename: "{app}\TerminalControl.exe"

[Run]
Filename: "{app}\TerminalControl.exe"; Description: "Запустить пульт"; Flags: postinstall nowait skipifsilent

# SF-057 — Correção aplicada: peer de automação no SeletorData

A correção do SF-057 é uma linha: `OnCreateAutomationPeer` no `SeletorData.xaml.cs` retornando `FrameworkElementAutomationPeer`. Aplicada abaixo junto com a simplificação do teste.

```csharp
protected override System.Windows.Automation.Peers.AutomationPeer OnCreateAutomationPeer()
    => new System.Windows.Automation.Peers.FrameworkElementAutomationPeer(this);
```

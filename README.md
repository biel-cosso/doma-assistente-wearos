# Doma Assistente (Wear OS)

Trabalho prático da disciplina **DGT2816 - Interação com sensores de smartphones e wearables** (Estácio).
Aluno: Gabriel Cosso

App para relógio Wear OS pensado para a empresa Doma (caso do enunciado), que quer ajudar funcionários com
necessidades especiais, principalmente com deficiência visual, usando áudio.

O que o app faz:

- lê em voz alta as mensagens, lembretes e dicas da lista (é só tocar no aviso);
- nos alertas de segurança toca um bipe de alarme, vibra e lê o aviso usando o volume de alarme;
- aceita comandos de voz: "ler tudo", "ler mensagens", "ler alertas", "emergência", "conectar fone", "parar" e "ajuda";
- mostra se o relógio tem alto-falante ou fone Bluetooth e avisa quando o fone conecta ou desconecta;
- se não tiver saída de som, oferece abrir a tela de Bluetooth em vez de dar erro.

A documentação com os prints está em [docs/Documentacao-DGT2816.pdf](docs/Documentacao-DGT2816.pdf).

## Onde está cada parte do roteiro

- Passo 2 (verificar saídas de áudio com `getDevices`): `AudioHelper.audioOutputAvailable()`
- Passo 3 (`registerAudioDeviceCallback`): `AudioHelper.observarFones()`, registrado no `onStart()` da `MainActivity`
- Passo 4 (`ACTION_BLUETOOTH_SETTINGS`): `AudioHelper.abrirConfiguracoesBluetooth()`
- Passos 5 e 6 (tocar áudio): `Falador.kt` (TextToSpeech) e `tocarAlarme()` na `MainActivity`
- Microatividade 3 (ListView + Button, permissões e intent-filter): `activity_main.xml` e `AndroidManifest.xml`

## Como rodar

Precisa do Android Studio com uma imagem Wear OS instalada (usei Wear OS 3, API 30, no emulador
"Wear OS Small Round").

1. Abrir a pasta no Android Studio (File > Open) e esperar o Gradle sincronizar.
2. Selecionar o emulador Wear OS e clicar em Run.

## Observações

- Os avisos da lista são exemplos que eu criei, a Doma é uma empresa fictícia.
- No emulador o microfone não funciona, então a tela de voz abre mas não reconhece nada. Por isso, quando não vem
  nenhuma frase (ou com toque longo no botão), aparece a lista de comandos para escolher.
- O comando "emergência" só simula o alerta (alarme + instrução falada), ele não chama ninguém.
- A microatividade 5 (print pelo app complementar) precisa de um relógio de verdade pareado com o celular, que eu não
  tenho. Os prints foram tirados do emulador.

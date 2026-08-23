# Trilha Android

Jogo de **Trilha (Nine Men's Morris)** para Android, desenvolvido como app nativo e jogável offline.

## Funcionalidades

- Regras completas: colocação, movimentação, moinho, captura e voo com 3 peças.
- Vitória quando o adversário fica com menos de 3 peças ou sem movimentos legais.
- 3 níveis de dificuldade:
  - **Fácil**: decisões aleatórias.
  - **Médio**: cria moinhos, bloqueia ameaças e prioriza posições estratégicas.
  - **Difícil**: acrescenta avaliação de resposta adversária e análise heurística.
- Interface touch otimizada para celular em modo retrato.
- Jogo totalmente offline e sem anúncios.
- APK gerado automaticamente pelo GitHub Actions.

## Como jogar

Você controla as peças verdes e começa a partida. Toque em um ponto vazio para colocar uma peça. Depois da fase de colocação, toque em uma peça sua e depois no destino conectado. Ao formar um moinho (3 peças alinhadas), toque em uma peça adversária válida para removê-la.

Quando um jogador fica com apenas 3 peças, passa a poder mover uma peça para qualquer ponto vazio.

## APK

A automação `.github/workflows/android-apk.yml` compila o app e publica `Trilha-Jocimar.apk` na Release `trilha-latest` a cada atualização da branch do jogo.

## Build local

Requisitos: JDK 17, Android SDK 35 e Gradle 8.10.2.

```bash
gradle :app:assembleDebug
```

O APK será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

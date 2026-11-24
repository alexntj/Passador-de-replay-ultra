# Cap cut — implementação de texturas

## Alterações

- `applicationId` e `namespace`: `com.Privvideo.app`.
- Mantida a validação REIMOD em `https://ghostcheats.de.deplexo.com/api/validate`.
- Após a Key autorizada, a Activity exibe as opções `PASSADOR DE REPLAY` e `TEXTURAS`.
- O fluxo original do passador de replay continua usando as telas e métodos existentes.
- A tela de texturas lista os 12 pacotes empacotados em `app/src/main/assets/texture_packs`.
- A textura selecionada é extraída para `/storage/emulated/0/PrivBypass/texture_packs`; o mesmo executor shell usado pelo replay roda `cp -rf` via Shizuku para a pasta `com.dts.freefireth` já existente em `/storage/emulated/0/Android/data/`, sobrescrevendo os correspondentes sem remover a pasta.
- O AIDL foi normalizado para a sintaxe suportada pelo compilador Android.

## Build

No Android Studio, abra a pasta do projeto e execute `assembleDebug`.

Pelo terminal:

```bash
./gradlew assembleDebug
```

O APK será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

O projeto mantém a assinatura debug padrão da variante `debug`; para distribuição, configure uma keystore de release no `app/build.gradle`.

## Observação do ambiente de entrega

O sandbox usado para editar o projeto é ARM64, enquanto os binários `aidl` e `aapt2` instalados no SDK local são x86_64. Por isso, o APK não foi produzido neste ambiente: o Gradle interrompe em `compileDebugAidl` com `Exec format error`. Isso é uma limitação do ambiente de build, não do layout ou dos assets; em um host Android SDK x86_64 o comando acima é o build esperado.

# Cap Cut — implementação de texturas

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

O `versionCode` atual é `4` e o `versionName` é `@ghostprivbypass`. O rótulo do app no launcher é `Cap Cut`; a variante `debug` mantém `android:debuggable="false"`. O `appVersion` enviado à API permanece `2.0`.

O módulo `libintegrity` fixa o SHA-256 do certificado autorizado e a `IntegrityApplication` valida a assinatura antes da inicialização dos providers. O APK intermediário produzido pela CI usa a chave debug do runner; antes de instalar, assine-o com a keystore privada entregue separadamente. Consulte `SIGNING.md` para os comandos. Nunca versione a keystore nem a senha.

O APK final entregue foi assinado com a keystore correspondente ao fingerprint fixado no módulo de integridade. Builds próprios devem usar a mesma chave; se ela for rotacionada, atualize o fingerprint e publique uma atualização assinada pela chave anterior.

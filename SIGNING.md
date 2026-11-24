# Assinatura do APK

O projeto valida o certificado atual do APK instalado usando o módulo `libintegrity`. O fingerprint público autorizado é `bd68b288664443f4482ac295bf67dc774339d8f334697b51087e49d8383458a8`. A chave privada (`.jks`) e sua senha são entregues separadamente e não devem ser adicionadas ao repositório.

No Windows PowerShell, gere primeiro o APK pelo Gradle (`.\gradlew.bat assembleDebug`). Em seguida, use `apksigner` do Android SDK Build Tools para assinar o APK com a keystore fornecida:

```powershell
apksigner sign --ks .\ghostprivbypads-release.jks --ks-key-alias ghostprivbypads --out .\PassadorReplay-signed.apk .\app\build\outputs\apk\debug\app-debug.apk
apksigner verify --verbose --print-certs .\PassadorReplay-signed.apk
```

O `apksigner` solicitará a senha da keystore; a senha da chave usa o mesmo valor. O app falha fechado se o pacote instalado não tiver exatamente um certificado assinante e o fingerprint SHA-256 não corresponder. O certificado é público; a keystore e a senha devem ser guardadas em backup privado.

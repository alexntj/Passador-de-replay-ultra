package com.Privvideo.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.RemoteException
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.passador.auth.ApiGate
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import rikka.shizuku.Shizuku

import android.os.Process
import android.os.Debug
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    private companion object {
        const val APP_ID = "com.Privvideo.app"
        const val APP_VERSION_CODE = 2
        const val APP_SLUG = "stirlax-modz-m1nx-oficial"
        const val APP_VERSION = "2.0"
        const val OFFICIAL_GROUP_URL = "https://whatsapp.com/channel/0029VbCowiZGJP8MKaHW0k3k"
    }

    private val PKG_NORMAL = "com.dts.freefireth"
    private val PKG_MAX = "com.dts.freefiremax"
    private val PATH_NORMAL = "/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays/"
    private val PATH_MAX = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays/"
    private val REQ_NOTIFICATION = 1001
    private val REQ_STORAGE = 1002

    private lateinit var screenConnect: View
    private lateinit var screenLogin: View
    private lateinit var screenMode: View
    private lateinit var screenDirection: View
    private lateinit var screenTransfer: View
    private lateinit var screenTextures: View
    private lateinit var screenBypassSs: View
    private lateinit var screenHologram: View
    private lateinit var menuBackdrop: View
    private var galaxyBackground: GalaxyParticleView? = null
    private lateinit var logBox: TextView
    private lateinit var ssLogBox: TextView
    private lateinit var btnPassar: Button
    private lateinit var btnBypass: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvVersionNormal: TextView
    private lateinit var tvVersionMax: TextView
    private lateinit var etKey: EditText
    private lateinit var btnValidateKey: Button
    private lateinit var tvKeyStatus: TextView
    private lateinit var textureList: LinearLayout
    private lateinit var btnInjectTexture: Button
    private lateinit var tvTextureStatus: TextView
    private lateinit var btnInjectMaxHologram: Button
    private lateinit var btnClearPrivBypassFolder: Button
    private lateinit var tvMaxHologramStatus: TextView
    private lateinit var hologramSafeIndicator: View
    private lateinit var hologramLogBox: TextView
    private lateinit var hologramLogScroll: ScrollView
    private var hologramIndicatorAnimator: ObjectAnimator? = null
    private var hologramInjectionInProgress = false
    private var privBypassCleanupInProgress = false
    private lateinit var tvAndroidVersion: TextView
    private lateinit var tvDeviceModel: TextView
    private lateinit var tvShizukuState: TextView
    private lateinit var btnSsCleanup: Button
    private val adbManager by lazy { AdbManager(this) }
    private val SHIZUKU_REQUEST_CODE = 2001
    private var privilegedService: IPrivilegedService? = null
    private var pendingCommand: String? = null
    private var pendingCallback: ((String?) -> Unit)? = null

    private val shizukuArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(APP_ID, PrivilegedService::class.java.name)
        ).daemon(false)
            .processNameSuffix("privileged")
            .debuggable(true)
            .version(APP_VERSION_CODE)
    }

    private val shizukuConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            privilegedService = IPrivilegedService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            privilegedService = null
        }
    }

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                val message = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    "Shizuku autorizado para este aplicativo."
                } else {
                    "Permissão do Shizuku recusada."
                }
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }

    private var directionMaxToNormal = true
    private var currentKey = ""
    private var currentSessionToken = ""
    private val preferences by lazy { getSharedPreferences("priv_bypass_preferences", MODE_PRIVATE) }
    private data class TexturePack(val displayName: String, val folderName: String, val thumbnail: Int)
    private val texturePacks = arrayOf(
        TexturePack("BAKI", "2 BAKI", R.drawable.thumb_baki),
        TexturePack("BAKUGO", "1 BAKUGO", R.drawable.thumb_bakugo),
        TexturePack("GOKU BLACK", "3 GOKU BLACK", R.drawable.thumb_goku_black),
        TexturePack("SUPER SAYAJIN 3", "4 GOKU SUPER SAYAJIN 3", R.drawable.thumb_ssj3),
        TexturePack("LUFFY", "5 LUFFY", R.drawable.thumb_luffy),
        TexturePack("LUFFY GEAR 5", "6 LUFFY GEAR SIX", R.drawable.thumb_luffy_gear6),
        TexturePack("PANTERA COR DE ROSA", "7 PANTERA COR DE ROSA", R.drawable.thumb_pantera),
        TexturePack("GOJO", "8 GOJO", R.drawable.thumb_gojo),
        TexturePack("CHAMA", "10 CHAMA", R.drawable.thumb_chama),
        TexturePack("FINN", "11 FINN", R.drawable.thumb_finn),
        TexturePack("SONIC", "9 SONIC", R.drawable.thumb_sonic),
        TexturePack("CJ", "12 CJ", R.drawable.thumb_cj)
    )
    private var selectedTextureIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ativarBlindagemAtiva()
        setContentView(R.layout.activity_main)
        galaxyBackground = findViewById(R.id.galaxyBackground)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        menuBackdrop = findViewById(R.id.menuBackdrop)
        menuBackdrop.setOnClickListener { hideMenu() }
        screenConnect = findViewById(R.id.screenConnect)
        screenLogin = findViewById(R.id.screenLogin)
        screenMode = findViewById(R.id.screenMode)
        screenDirection = findViewById(R.id.screenDirection)
        screenTransfer = findViewById(R.id.screenTransfer)
        screenTextures = findViewById(R.id.screenTextures)
        screenBypassSs = findViewById(R.id.screenBypassSs)
        screenHologram = findViewById(R.id.screenHologram)
        logBox = findViewById(R.id.logBox)
        ssLogBox = findViewById(R.id.ssLogBox)
        btnPassar = findViewById(R.id.btnPassar)
        btnBypass = findViewById(R.id.btnBypass)
        tvStatus = findViewById(R.id.tvStatus)
        tvVersionNormal = findViewById(R.id.tvVersionNormal)
        tvVersionMax = findViewById(R.id.tvVersionMax)
        etKey = findViewById(R.id.etKey)
        btnValidateKey = findViewById(R.id.btnValidateKey)
        tvKeyStatus = findViewById(R.id.tvKeyStatus)
        textureList = findViewById(R.id.textureList)
        btnInjectTexture = findViewById(R.id.btnInjectTexture)
        tvTextureStatus = findViewById(R.id.tvTextureStatus)
        btnInjectMaxHologram = findViewById(R.id.btnInjectMaxHologram)
        btnClearPrivBypassFolder = findViewById(R.id.btnClearPrivBypassFolder)
        tvMaxHologramStatus = findViewById(R.id.tvMaxHologramStatus)
        hologramSafeIndicator = findViewById(R.id.hologramSafeIndicator)
        hologramLogBox = findViewById(R.id.hologramLogBox)
        hologramLogScroll = findViewById(R.id.hologramLogScroll)
        tvAndroidVersion = findViewById(R.id.tvAndroidVersion)
        tvDeviceModel = findViewById(R.id.tvDeviceModel)
        tvShizukuState = findViewById(R.id.tvShizukuState)
        btnSsCleanup = findViewById(R.id.btnSsCleanup)
        buildTextureList()
        etKey.setText(preferences.getString("saved_key", ""))

        // Pede as duas permissões assim que abre
        requestNotificationPermission()
        requestAllFilesPermission()
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        requestShizukuPermission()

        findViewById<Button>(R.id.btnConectar).setOnClickListener { openShizuku() }

        findViewById<Button>(R.id.btnJaConectei).setOnClickListener {
            verifyShizukuAndContinue()
        }

        btnValidateKey.setOnClickListener { validateKeyAndOpenApp() }
        findViewById<Button>(R.id.btnSaveKey).setOnClickListener { saveKeyLocally() }
        findViewById<Button>(R.id.btnOfficialGroup).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OFFICIAL_GROUP_URL)))
            } catch (_: Exception) {
                android.widget.Toast.makeText(this, "Não foi possível abrir o grupo oficial.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        findViewById<Button>(R.id.btnReplayMode).setOnClickListener {
            openReplayScreen()
        }
        findViewById<Button>(R.id.btnTextureMode).setOnClickListener {
            openTextureScreen()
        }
        findViewById<Button>(R.id.btnCloseMenu).setOnClickListener { hideMenu() }
        findViewById<Button>(R.id.btnMenuDirection).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.btnMenuTransfer).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.btnMenuTextures).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.btnMenuBypassSs).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.btnBypassSsMenu).setOnClickListener { openBypassSsScreen() }
        findViewById<Button>(R.id.btnMenuHologram).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.btnHologramMenu).setOnClickListener { openHologramScreen() }
        findViewById<Button>(R.id.btnExitMenu).setOnClickListener { exitToLogin() }
        findViewById<Button>(R.id.btnSsCleanup).setOnClickListener { cleanupOwnStateAndUninstall() }
        btnInjectTexture.setOnClickListener { injectSelectedTexture() }
        btnInjectMaxHologram.setOnClickListener { injectMaxHologram() }
        btnClearPrivBypassFolder.setOnClickListener { confirmClearPrivBypassFolder() }

        findViewById<Button>(R.id.btnMaxToNormal).setOnClickListener {
            directionMaxToNormal = true
            fadeOut(screenDirection)
            fadeIn(screenTransfer)
            updateTransferUI()
        }

        findViewById<Button>(R.id.btnNormalToMax).setOnClickListener {
            directionMaxToNormal = false
            fadeOut(screenDirection)
            fadeIn(screenTransfer)
            updateTransferUI()
        }

        btnPassar.setOnClickListener { startTransfer() }
        btnBypass.setOnClickListener { doBypass() }


        screenConnect.alpha = 0f
        screenConnect.animate().alpha(1f).setDuration(500).start()
    }

    override fun onResume() {
        super.onResume()
        galaxyBackground?.resumeAnimation()
        if (this::screenHologram.isInitialized && screenHologram.visibility == View.VISIBLE) {
            startHologramIndicatorPulse()
        }
    }

    override fun onPause() {
        galaxyBackground?.pauseAnimation()
        stopHologramIndicatorPulse()
        super.onPause()
    }

    private fun openShizuku() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                startActivity(Intent("moe.shizuku.manager.action.MAIN_SETTINGS"))
            }
            android.widget.Toast.makeText(
                this,
                "No Shizuku, inicie o serviço e autorize o Cap cut. Depois volte e toque em JÁ CONECTEI.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            android.widget.Toast.makeText(
                this,
                "Não foi possível abrir o Shizuku. Verifique se ele está instalado.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun hasShizukuPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) {
                android.widget.Toast.makeText(
                    this,
                    "Inicie o serviço do Shizuku e tente novamente.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                false
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                true
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                android.widget.Toast.makeText(
                    this,
                    "Permita este aplicativo no Shizuku.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                false
            } else {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                false
            }
        } catch (e: Throwable) {
            log("Shizuku indisponível: ${e.message}")
            false
        }
    }

    private fun verifyShizukuAndContinue() {
        try {
            if (!Shizuku.pingBinder()) {
                android.widget.Toast.makeText(
                    this,
                    "Shizuku não está conectado. Toque em ABRIR SHIZUKU e inicie o serviço.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }

            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                android.widget.Toast.makeText(
                    this,
                    "Autorize o Cap cut no Shizuku e toque novamente em JÁ CONECTEI.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                return
            }

            // O vínculo do UserService é feito somente ao iniciar a transferência.
            // Aqui validamos apenas o Binder do Shizuku e a autorização do aplicativo,
            // evitando que a tela fique aguardando um callback de bind.
            continueAfterShizukuVerification()
        } catch (e: Throwable) {
            log("Falha ao verificar o Shizuku: ${e.message}")
            android.widget.Toast.makeText(
                this,
                "Não foi possível validar o serviço do Shizuku. Abra-o novamente e tente outra vez.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun continueAfterShizukuVerification() {
        android.widget.Toast.makeText(
            this,
            "Shizuku conectado e autorizado.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        fadeOut(screenConnect)
        fadeIn(screenLogin)
    }

    private data class KeyValidation(
        val authorized: Boolean,
        val message: String,
        val sessionToken: String
    )

    private fun validateKeyAndOpenApp() {
        val key = etKey.text.toString().trim()
        if (key.isEmpty()) {
            tvKeyStatus.text = "Digite sua Key para continuar."
            tvKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            return
        }
        validateKey(
            key,
            onSuccess = {
                fadeOut(screenLogin)
                fadeIn(screenDirection)
                detectVersions()
            }
        )
    }

    private fun saveKeyLocally() {
        val key = etKey.text.toString().trim()
        if (key.isBlank()) {
            tvKeyStatus.text = "Digite uma Key antes de salvar."
            tvKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            return
        }
        preferences.edit().putString("saved_key", key).apply()
        tvKeyStatus.text = "Key salva neste aparelho."
        tvKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.state_success))
    }

    private fun openReplayScreen() {
        hideMenu()
        setActiveMenuItem(R.id.btnReplayMode)
        fadeOut(screenBypassSs)
        fadeOut(screenTextures)
        fadeOut(screenHologram)
        fadeOut(screenTransfer)
        fadeIn(screenDirection)
        detectVersions()
    }

    private fun openTextureScreen() {
        hideMenu()
        setActiveMenuItem(R.id.btnTextureMode)
        fadeOut(screenBypassSs)
        fadeOut(screenHologram)
        fadeOut(screenDirection)
        fadeOut(screenTransfer)
        fadeIn(screenTextures)
    }

    private fun openHologramScreen() {
        hideMenu()
        setActiveMenuItem(R.id.btnHologramMenu)
        fadeOut(screenDirection)
        fadeOut(screenTransfer)
        fadeOut(screenTextures)
        fadeOut(screenBypassSs)
        animateHologramEntry()
        val maxVersion = try { packageManager.getPackageInfo(PKG_MAX, 0).versionName } catch (_: Exception) { null }
        val installed = maxVersion != null
        findViewById<TextView>(R.id.tvHologramMaxVersion).text = if (installed) {
            "FREE FIRE MAX\nVersão instalada: $maxVersion"
        } else {
            "FREE FIRE MAX\nNão instalado"
        }
        btnInjectMaxHologram.isEnabled = installed
        tvMaxHologramStatus.text = if (installed) "STATUS DO PACOTE: PRONTO" else "STATUS DO PACOTE: FF MAX NÃO INSTALADO"
        tvMaxHologramStatus.setTextColor(if (installed) ContextCompat.getColor(this, R.color.state_success) else ContextCompat.getColor(this, R.color.state_error))
        startHologramIndicatorPulse()
        appendHologramLog("Tela holograma safe aberta.")
        appendHologramLog(if (installed) "Free Fire MAX detectado: versão $maxVersion." else "Free Fire MAX não está instalado neste dispositivo.")
    }

    private fun startHologramIndicatorPulse() {
        if (!this::hologramSafeIndicator.isInitialized || screenHologram.visibility != View.VISIBLE) return
        hologramIndicatorAnimator?.cancel()
        hologramSafeIndicator.alpha = 1f
        hologramIndicatorAnimator = ObjectAnimator.ofFloat(hologramSafeIndicator, View.ALPHA, 1f, 0.28f).apply {
            duration = 620L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopHologramIndicatorPulse() {
        hologramIndicatorAnimator?.cancel()
        hologramIndicatorAnimator = null
        if (this::hologramSafeIndicator.isInitialized) hologramSafeIndicator.alpha = 1f
    }

    private fun animateHologramEntry() {
        screenHologram.animate().cancel()
        screenHologram.alpha = 0f
        screenHologram.translationY = 14f * resources.displayMetrics.density
        screenHologram.visibility = View.VISIBLE
        screenHologram.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(320L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun openBypassSsScreen() {
        hideMenu()
        setActiveMenuItem(R.id.btnBypassSsMenu)
        fadeOut(screenDirection)
        fadeOut(screenTransfer)
        fadeOut(screenTextures)
        fadeOut(screenHologram)
        fadeIn(screenBypassSs)
        tvAndroidVersion.text = "ANDROID\n${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        tvDeviceModel.text = "MODELO\n${Build.MANUFACTURER} ${Build.MODEL}"
        val connected = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
        val authorized = connected && try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) { false }
        tvShizukuState.text = "SHIZUKU\n${if (authorized) "CONECTADO E AUTORIZADO" else if (connected) "CONECTADO, SEM AUTORIZAÇÃO" else "DESCONECTADO"}"
        ssLogBox.text = "[INFO] Diagnóstico do aplicativo pronto.\n[INFO] Nenhum log global do Android será alterado.\n"
    }

    private fun showMenu() {
        menuBackdrop.visibility = View.VISIBLE
        screenMode.alpha = 0f
        screenMode.translationX = -screenMode.width.toFloat().coerceAtLeast(300.dp().toFloat())
        screenMode.visibility = View.VISIBLE
        screenMode.animate().alpha(1f).translationX(0f).setDuration(240).start()
    }

    private fun setActiveMenuItem(activeId: Int) {
        val ids = intArrayOf(R.id.btnReplayMode, R.id.btnTextureMode, R.id.btnBypassSsMenu, R.id.btnHologramMenu)
        ids.forEach { id ->
            val button = findViewById<Button>(id)
            val active = id == activeId
            button.background = ContextCompat.getDrawable(
                this,
                if (active) R.drawable.btn_primary else R.drawable.btn_outline
            )
            button.setTextColor(if (active) 0xFF000000.toInt() else ContextCompat.getColor(this, R.color.accent_soft))
        }
    }

    private fun hideMenu() {
        if (screenMode.visibility != View.VISIBLE) return
        screenMode.animate().alpha(0f).translationX(-screenMode.width.toFloat().coerceAtLeast(300.dp().toFloat())).setDuration(180)
            .withEndAction {
                screenMode.visibility = View.GONE
                menuBackdrop.visibility = View.GONE
            }.start()
    }

    private fun exitToLogin() {
        hideMenu()
        currentKey = ""
        currentSessionToken = ""
        fadeOut(screenDirection)
        fadeOut(screenTransfer)
        fadeOut(screenTextures)
        fadeOut(screenHologram)
        fadeOut(screenBypassSs)
        fadeIn(screenLogin)
        tvKeyStatus.text = "Sessão encerrada. A Key salva continua preenchida."
        tvKeyStatus.setTextColor(0xFFAAAAAA.toInt())
    }

    private fun cleanupOwnStateAndUninstall() {
        btnSsCleanup.isEnabled = false
        ssLogBox.text = "[INFO] Iniciando limpeza do estado do Cap cut...\n"
        ssLogBox.append("[OK] Limpando o painel de logs gerado pelo aplicativo.\n")
        logBox.text = ""
        ssLogBox.append("[OK] Logs internos do aplicativo removidos.\n")
        ssLogBox.append("[INFO] Desconectando o serviço Shizuku deste aplicativo...\n")
        try {
            Shizuku.unbindUserService(shizukuArgs, shizukuConnection, true)
            privilegedService = null
            ssLogBox.append("[OK] Vínculo do serviço privilegiado encerrado.\n")
        } catch (e: Throwable) {
            ssLogBox.append("[INFO] Nenhum serviço privilegiado ativo: ${e.message ?: "já desconectado"}\n")
        }
        ssLogBox.append("[INFO] Abrindo a tela de desinstalação do próprio aplicativo...\n")
        try {
            val uninstall = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(uninstall)
        } catch (e: Exception) {
            ssLogBox.append("[ERRO] Não foi possível abrir a desinstalação: ${e.message}\n")
            btnSsCleanup.isEnabled = true
        }
    }

    private fun buildTextureList() {
        textureList.removeAllViews()
        texturePacks.forEachIndexed { index, pack ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(10.dp(), 8.dp(), 12.dp(), 8.dp())
            }
            val image = ImageView(this).apply {
                setImageResource(pack.thumbnail)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            row.addView(image, LinearLayout.LayoutParams(58.dp(), 58.dp()))
            val name = TextView(this).apply {
                text = pack.displayName
                textSize = 15f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(14.dp(), 0, 0, 0)
            }
            row.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val number = TextView(this).apply {
                text = String.format("%02d", index + 1)
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_soft))
            }
            row.addView(number)
            row.setOnClickListener {
                selectedTextureIndex = index
                updateTextureSelection()
            }
            textureList.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8.dp()) })
        }
        updateTextureSelection()
    }

    private fun updateTextureSelection() {
        for (i in 0 until textureList.childCount) {
            val selected = i == selectedTextureIndex
            textureList.getChildAt(i).background = GradientDrawable().apply {
                cornerRadius = 18.dp().toFloat()
                setColor(if (selected) ContextCompat.getColor(this@MainActivity, R.color.surface_pressed)
                    else ContextCompat.getColor(this@MainActivity, R.color.surface_panel))
                setStroke(if (selected) 2 else 1, if (selected) ContextCompat.getColor(this@MainActivity, R.color.accent_primary)
                    else ContextCompat.getColor(this@MainActivity, R.color.outline_subtle))
            }
        }
        tvTextureStatus.text = "Selecionado: ${texturePacks[selectedTextureIndex].displayName}"
        tvTextureStatus.setTextColor(0xFFBBBBBB.toInt())
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun injectSelectedTexture() {
        if (privBypassCleanupInProgress) {
            tvTextureStatus.text = "Aguarde a limpeza da pasta PrivBypass terminar."
            tvTextureStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            return
        }
        if (currentKey.isBlank()) {
            tvTextureStatus.text = "Valide sua Key antes de injetar uma textura."
            tvTextureStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            return
        }

        if (!hasSharedStorageAccess()) {
            tvTextureStatus.text = "Permita o acesso ao armazenamento e tente novamente."
            tvTextureStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            requestAllFilesPermission()
            return
        }

        val selected = texturePacks[selectedTextureIndex]
        btnInjectTexture.isEnabled = false
        tvTextureStatus.text = "Preparando ${selected.displayName}..."
        tvTextureStatus.setTextColor(0xFFBBBBBB.toInt())

        Thread {
            try {
                // O replay trabalha diretamente no armazenamento compartilhado; as texturas
                // também ficam nele para o processo shell do Shizuku conseguir lê-las.
                val root = File(Environment.getExternalStorageDirectory(), "PrivBypass/texture_packs")
                val selectedRoot = File(root, selected.folderName)
                root.mkdirs()
                copyAssetTree("texture_packs/${selected.folderName}", selectedRoot)
                val source = File(selectedRoot, PKG_NORMAL)
                if (!source.isDirectory) {
                    throw IllegalStateException("Pasta do Free Fire não encontrada no asset")
                }

                val command = """
                    set -e
                    SOURCE_DIR=${shellQuote(source.absolutePath)}
                    TARGET_DIR=${shellQuote("/storage/emulated/0/Android/data/$PKG_NORMAL")}
                    test -d "${'$'}SOURCE_DIR"
                    mkdir -p "${'$'}TARGET_DIR"
                    cp -rf "${'$'}SOURCE_DIR"/. "${'$'}TARGET_DIR"/
                    sync
                    test -d "${'$'}TARGET_DIR"
                    echo '[RESULT] TEXTURE_OK'
                """.trimIndent()

                runOnUiThread { tvTextureStatus.text = "Copiando ${selected.displayName} via Shizuku..." }
                executePrivileged(command) { result ->
                    btnInjectTexture.isEnabled = true
                    if (result != null && result.contains("[RESULT] TEXTURE_OK")) {
                        tvTextureStatus.text = "${selected.displayName} aplicado com sucesso no FF NORMAL."
                        tvTextureStatus.setTextColor(ContextCompat.getColor(this, R.color.state_success))
                    } else {
                        tvTextureStatus.text = "Falha ao copiar a textura. Verifique o Shizuku."
                        tvTextureStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
                        log("Falha na textura: ${result ?: "sem resposta"}")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnInjectTexture.isEnabled = true
                    tvTextureStatus.text = "Erro ao preparar a textura: ${e.message}"
                    tvTextureStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
                }
            }
        }.start()
    }

    /**
     * Instala o pacote "holograma arma" no diretório de dados do Free Fire MAX.
     * Os arquivos são extraídos dos assets do APK para uma área compartilhada
     * legível pelo processo privilegiado e copiados usando a sessão Shizuku já existente.
     */
    private fun injectMaxHologram() {
        animateHologramButtonPress()
        appendHologramLog("Toque em INJETAR recebido.")
        if (privBypassCleanupInProgress) {
            appendHologramLog("Aguarde a limpeza da pasta PrivBypass terminar antes de injetar.")
            return
        }
        if (currentKey.isBlank()) {
            tvMaxHologramStatus.text = "Valide sua Key antes de aplicar o pacote."
            tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            appendHologramLog("Falha: nenhuma Key validada nesta sessão.")
            return
        }

        if (!hasSharedStorageAccess()) {
            tvMaxHologramStatus.text = "Permita o acesso ao armazenamento e tente novamente."
            tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            appendHologramLog("Aguardando permissão de acesso ao armazenamento compartilhado.")
            requestAllFilesPermission()
            return
        }

        try {
            packageManager.getPackageInfo(PKG_MAX, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            tvMaxHologramStatus.text = "Free Fire MAX não está instalado."
            tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            appendHologramLog("Falha: Free Fire MAX não está instalado.")
            return
        }

        btnInjectMaxHologram.isEnabled = false
        btnClearPrivBypassFolder.isEnabled = false
        hologramInjectionInProgress = true
        tvMaxHologramStatus.text = "Preparando arquivos do holograma..."
        tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        appendHologramLog("Validações concluídas; iniciando preparação dos arquivos.")

        Thread {
            try {
                val sharedRoot = File(Environment.getExternalStorageDirectory(), "PrivBypass/max_hologram")
                val sourcePackage = File(sharedRoot, PKG_MAX)
                appendHologramLog("Extraindo os assets do pacote para o armazenamento compartilhado.")
                sharedRoot.mkdirs()
                copyAssetTree("max_hologram/$PKG_MAX", sourcePackage)

                val fileInfo = File(sourcePackage, "files/contentcache/Optional/android/fileinfo")
                val shader = File(sourcePackage, "files/contentcache/Optional/android/gameassetbundles/shaders.Nagimodz~Gratis~3D")
                if (!fileInfo.isFile || !shader.isFile || fileInfo.length() == 0L || shader.length() == 0L) {
                    throw IllegalStateException("Os arquivos do pacote estão incompletos.")
                }
                appendHologramLog("Assets extraídos e arquivos obrigatórios verificados.")

                val dataDir = "/storage/emulated/0/Android/data"
                val targetPackage = "$dataDir/$PKG_MAX"
                val command = """
                    set -eu
                    SOURCE_DIR=${shellQuote(sourcePackage.absolutePath)}
                    DATA_DIR=${shellQuote(dataDir)}
                    TARGET_DIR=${shellQuote(targetPackage)}
                    test -d "${'$'}SOURCE_DIR"
                    test -s "${'$'}SOURCE_DIR/files/contentcache/Optional/android/fileinfo"
                    test -s "${'$'}SOURCE_DIR/files/contentcache/Optional/android/gameassetbundles/shaders.Nagimodz~Gratis~3D"
                    mkdir -p "${'$'}DATA_DIR"
                    cp -rf "${'$'}SOURCE_DIR" "${'$'}DATA_DIR/"
                    sync
                    test -s "${'$'}TARGET_DIR/files/contentcache/Optional/android/fileinfo"
                    test -s "${'$'}TARGET_DIR/files/contentcache/Optional/android/gameassetbundles/shaders.Nagimodz~Gratis~3D"
                    SRC_INFO=${'$'}(wc -c < "${'$'}SOURCE_DIR/files/contentcache/Optional/android/fileinfo")
                    DST_INFO=${'$'}(wc -c < "${'$'}TARGET_DIR/files/contentcache/Optional/android/fileinfo")
                    SRC_SHADER=${'$'}(wc -c < "${'$'}SOURCE_DIR/files/contentcache/Optional/android/gameassetbundles/shaders.Nagimodz~Gratis~3D")
                    DST_SHADER=${'$'}(wc -c < "${'$'}TARGET_DIR/files/contentcache/Optional/android/gameassetbundles/shaders.Nagimodz~Gratis~3D")
                    test "${'$'}SRC_INFO" = "${'$'}DST_INFO"
                    test "${'$'}SRC_SHADER" = "${'$'}DST_SHADER"
                    echo '[RESULT] MAX_HOLOGRAM_OK'
                """.trimIndent()

                runOnUiThread {
                    tvMaxHologramStatus.text = "Copiando para Android/data via Shizuku..."
                }
                appendHologramLog("Enviando a cópia para Android/data usando Shizuku.")
                executePrivileged(command) { result ->
                    btnInjectMaxHologram.isEnabled = true
                    btnClearPrivBypassFolder.isEnabled = true
                    hologramInjectionInProgress = false
                    if (result != null && result.contains("[RESULT] MAX_HOLOGRAM_OK")) {
                        tvMaxHologramStatus.text = "ARQUIVOS COPIADOS PARA O FF MAX"
                        tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.state_success))
                        appendHologramLog("Sucesso: arquivos copiados e tamanhos conferidos no destino.")
                    } else {
                        tvMaxHologramStatus.text = "Falha ao copiar. Verifique a autorização Shizuku."
                        tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
                        val detail = result?.trim()?.takeLast(180)?.takeIf { it.isNotBlank() } ?: "sem resposta do Shizuku"
                        appendHologramLog("Falha na cópia via Shizuku: $detail")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnInjectMaxHologram.isEnabled = true
                    btnClearPrivBypassFolder.isEnabled = true
                    hologramInjectionInProgress = false
                    tvMaxHologramStatus.text = "Erro ao preparar pacote: ${e.message}"
                    tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
                }
                appendHologramLog("Erro ao preparar o pacote: ${e.message ?: e.javaClass.simpleName}")
            }
        }.start()
    }

    private fun confirmClearPrivBypassFolder() {
        if (hologramInjectionInProgress || privBypassCleanupInProgress || !btnInjectTexture.isEnabled) {
            appendHologramLog("Aguarde a operação atual terminar antes de limpar a pasta.")
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Limpar pasta PrivBypass?")
            .setMessage("Isso apaga a pasta PrivBypass do armazenamento do aparelho. O holograma já copiado para o Free Fire MAX não será removido.")
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("LIMPAR") { _, _ -> clearPrivBypassFolder() }
            .show()
    }

    private fun clearPrivBypassFolder() {
        if (hologramInjectionInProgress || privBypassCleanupInProgress || !btnInjectTexture.isEnabled) {
            appendHologramLog("Limpeza não iniciada: há outra operação em andamento.")
            return
        }

        if (!hasSharedStorageAccess()) {
            appendHologramLog("Permissão de armazenamento necessária para limpar PrivBypass.")
            tvMaxHologramStatus.text = "Permita o acesso ao armazenamento para limpar a pasta."
            tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            requestAllFilesPermission()
            return
        }

        val storageRoot = Environment.getExternalStorageDirectory()
        val workspace = File(storageRoot, "PrivBypass")
        val injectWasEnabled = btnInjectMaxHologram.isEnabled
        val textureWasEnabled = btnInjectTexture.isEnabled
        btnClearPrivBypassFolder.isEnabled = false
        btnInjectMaxHologram.isEnabled = false
        btnInjectTexture.isEnabled = false
        privBypassCleanupInProgress = true
        appendHologramLog("Iniciando limpeza de /storage/emulated/0/PrivBypass.")

        Thread {
            val result = try {
                val canonicalRoot = storageRoot.canonicalFile
                val canonicalWorkspace = workspace.canonicalFile
                if (canonicalWorkspace.parentFile != canonicalRoot || canonicalWorkspace.name != "PrivBypass") {
                    throw SecurityException("O caminho de limpeza não corresponde à pasta PrivBypass.")
                }

                when {
                    !workspace.exists() -> true to "A pasta PrivBypass já não existe."
                    workspace.deleteRecursively() -> true to "Pasta PrivBypass removida."
                    else -> false to "Não foi possível remover todos os arquivos da pasta PrivBypass."
                }
            } catch (e: Exception) {
                false to (e.message ?: e.javaClass.simpleName)
            }

            runOnUiThread {
                privBypassCleanupInProgress = false
                btnClearPrivBypassFolder.isEnabled = true
                btnInjectMaxHologram.isEnabled = injectWasEnabled
                btnInjectTexture.isEnabled = textureWasEnabled
                if (result.first) {
                    tvMaxHologramStatus.text = "PASTA PRIVBYPASS LIMPA"
                    tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.state_success))
                    appendHologramLog("${result.second} O holograma já instalado não foi alterado.")
                } else {
                    tvMaxHologramStatus.text = "Falha ao limpar a pasta PrivBypass."
                    tvMaxHologramStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
                    appendHologramLog("Falha na limpeza: ${result.second}")
                }
            }
        }.start()
    }

    private fun animateHologramButtonPress() {
        btnInjectMaxHologram.animate().cancel()
        btnInjectMaxHologram.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(75L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                btnInjectMaxHologram.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun appendHologramLog(message: String) {
        if (!this::hologramLogBox.isInitialized) return
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val existing = hologramLogBox.text.toString().lineSequence().filter { it.isNotBlank() }.toList()
            hologramLogBox.text = (existing + "[$timestamp] $message").takeLast(80).joinToString("\n")
            hologramLogScroll.post { hologramLogScroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun copyAssetTree(assetPath: String, destination: File) {
        val children = assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }
        destination.mkdirs()
        children.forEach { child ->
            copyAssetTree("$assetPath/$child", File(destination, child))
        }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private fun validateKey(
        key: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {}
    ) {
        btnValidateKey.isEnabled = false
        tvKeyStatus.text = "Validando acesso..."
        tvKeyStatus.setTextColor(0xFFAAAAAA.toInt())
        Thread {
            val result = requestKeyValidation(key)
            runOnUiThread {
                btnValidateKey.isEnabled = true
                if (result.authorized) {
                    currentKey = key
                    currentSessionToken = result.sessionToken
                    tvKeyStatus.text = result.message.ifBlank { "Key autorizada." }
                    tvKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.state_success))
                    onSuccess()
                } else {
                    tvKeyStatus.text = result.message.ifBlank {
                        "Key inválida, expirada, pausada ou HWID não corresponde."
                    }
                    tvKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
                    onFailure()
                }
            }
        }.start()
    }

    private fun requestKeyValidation(key: String): KeyValidation {
        val result = ApiGate.validate(
            this,
            key,
            currentSessionToken,
            APP_SLUG,
            APP_VERSION
        )
        return KeyValidation(
            authorized = result.isAuthorized,
            message = result.message,
            sessionToken = result.sessionToken
        )
    }

    private fun requestShizukuPermission() {
        if (!Shizuku.isPreV11()) hasShizukuPermission()
    }

    private fun executePrivileged(command: String, callback: (String?) -> Unit) {
        if (!hasShizukuPermission()) {
            runOnUiThread { callback(null) }
            return
        }
        Thread {
            val result = try {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(
                    null,
                    arrayOf("sh", "-c", command),
                    null,
                    null
                ) as java.lang.Process

                val stdout = process.inputStream.bufferedReader().use { it.readText() }
                val stderr = process.errorStream.bufferedReader().use { it.readText() }
                buildString {
                    append(stdout)
                    if (stderr.isNotBlank()) append(stderr)
                }
            } catch (e: Throwable) {
                log("Erro ao executar shell via Shizuku: ${e.message}")
                null
            }
            runOnUiThread { callback(result) }
        }.start()
    }

    override fun onDestroy() {
        galaxyBackground?.pauseAnimation()
        stopHologramIndicatorPulse()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        try {
            Shizuku.unbindUserService(shizukuArgs, shizukuConnection, true)
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIFICATION
                )
            }
        }
    }

    private fun hasSharedStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQ_STORAGE
            )
        }
    }

    private fun detectVersions() {
        val normalVersion = try {
            packageManager.getPackageInfo(PKG_NORMAL, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val maxVersion = try {
            packageManager.getPackageInfo(PKG_MAX, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val normalOk = normalVersion != null
        val maxOk = maxVersion != null

        tvVersionNormal.text = if (normalOk) {
            "FF NORMAL  •  INSTALADO\nVersão: $normalVersion  •  Pacote: $PKG_NORMAL"
        } else {
            "FF NORMAL  •  NÃO INSTALADO\nPacote esperado: $PKG_NORMAL"
        }
        tvVersionMax.text = if (maxOk) {
            "FF MAX  •  INSTALADO\nVersão: $maxVersion  •  Pacote: $PKG_MAX"
        } else {
            "FF MAX  •  NÃO INSTALADO\nPacote esperado: $PKG_MAX"
        }

        if (!normalOk || !maxOk) {
            tvStatus.text = "⚠ Os dois Free Fire precisam estar instalados"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            btnPassar.isEnabled = false
            btnPassar.alpha = 0.4f
        } else {
            tvStatus.text = "Tudo pronto"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))
            btnPassar.isEnabled = true
            btnPassar.alpha = 1f
        }
    }

    private fun updateTransferUI() {
        val title = findViewById<TextView>(R.id.tvTransferTitle)
        title.text = if (directionMaxToNormal) {
            "BYPASS FF MAX PARA FF"
        } else {
            "BYPASS FF PARA FF MAX"
        }
        logBox.text = ""
        btnBypass.visibility = View.GONE
        btnPassar.visibility = View.VISIBLE
        btnPassar.isEnabled = true
    }

    private fun startTransfer() {
        btnPassar.isEnabled = false
        if (currentKey.isBlank()) {
            btnPassar.isEnabled = true
            fadeOut(screenTransfer)
            fadeIn(screenLogin)
            tvKeyStatus.text = "Valide sua Key antes de transferir."
            tvKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.state_error))
            return
        }
        log("Validando API...")
        validateKey(
            currentKey,
            onSuccess = {
                log("API validada com sucesso.")
                executeReplayTransfer()
            },
            onFailure = {
                log("API não autorizou a transferência.")
                btnPassar.isEnabled = true
            }
        )
    }

    private fun executeReplayTransfer() {
        val srcDir = if (directionMaxToNormal) PATH_MAX else PATH_NORMAL
        val dstDir = if (directionMaxToNormal) PATH_NORMAL else PATH_MAX
        val targetVersion = if (directionMaxToNormal) "1.132.21" else "2.132.21"
        log("Verificando Shizuku...")
        log("Buscando replays...")

        executePrivileged(buildTransferCommand(srcDir, dstDir, targetVersion)) { result ->
            if (result != null) {
                result.lineSequence()
                    .filter { it.isNotBlank() }
                    .forEach { line -> logTransferLine(line) }

                if (result.contains("[RESULT] TRANSFER_OK")) {
                    val destination = if (directionMaxToNormal) "FF normal" else "FF MAX"
                    log("Replays sincronizados com o $destination.")
                    log("Sincronização concluída com sucesso.")
                    log("Devs: Ghost • Edge")
                    log("Discord oficial: https://discord.gg/auTTQKvAnJ")
                    btnPassar.visibility = View.GONE
                    btnBypass.visibility = View.VISIBLE
                    btnBypass.alpha = 0f
                    btnBypass.animate().alpha(1f).setDuration(350).start()
                } else {
                    log("Falha: a transferência não foi concluída.")
                    btnPassar.isEnabled = true
                }
            } else {
                log("Falha: Shizuku não executou o comando.")
                btnPassar.isEnabled = true
            }
        }
    }

    private fun logTransferLine(line: String) {
        when {
            line.contains("[ERROR]") -> log(line.removePrefix("[ERROR] "))
            line.contains("Procurando replay") -> log("Buscando replays...")
            line.contains("Copiando arquivos") -> log("Sincronizando replays...")
            line.contains("Atualizando Version") -> log("Ajustando compatibilidade do replay...")
            line.contains("Validando arquivos") -> log("Validando replays sincronizados...")
            line.contains("TRANSFER_OK") -> Unit
            line.contains("[INFO]") -> log(line.removePrefix("[INFO] "))
        }
    }

    private fun buildTransferCommand(srcDir: String, dstDir: String, targetVersion: String): String {
        return """
            set -u
            SRC_DIR='$srcDir'
            DST_DIR='$dstDir'
            TARGET_VERSION='$targetVersion'
            MARKER="${'$'}DST_DIR/.transfer_interna"
            RECENT_BIN=''
            RECENT_JSON=''
            JSON_FILE=''
            JSON_TMP=''

            fail() {
                echo "[ERROR] ${'$'}1"
                exit 1
            }

            cleanup() {
                rm -f "${'$'}MARKER" "${'$'}JSON_TMP" 2>/dev/null || true
            }

            trap cleanup EXIT INT TERM HUP

            echo "[PROGRESS] Verificando pasta de origem"
            [ -d "${'$'}SRC_DIR" ] || fail "Pasta de origem não existe"

            echo "[PROGRESS] Procurando replay mais recente"
            RECENT_BIN=${'$'}(ls -1t "${'$'}SRC_DIR"/*_rep.bin 2>/dev/null | head -n 1)
            [ -n "${'$'}RECENT_BIN" ] || fail "Nenhum arquivo _rep.bin encontrado"

            RECENT_JSON="${'$'}{RECENT_BIN%_rep.bin}_rep.json"
            [ -f "${'$'}RECENT_JSON" ] || fail "JSON correspondente não encontrado"

            echo "[PROGRESS] Verificando BIN e JSON"
            BIN_NAME=${'$'}(basename "${'$'}RECENT_BIN")
            JSON_NAME=${'$'}(basename "${'$'}RECENT_JSON")
            JSON_FILE="${'$'}DST_DIR/${'$'}JSON_NAME"
            JSON_TMP="${'$'}JSON_FILE.tmp"

            echo "[PROGRESS] Criando pasta de destino"
            mkdir -p "${'$'}DST_DIR" || fail "Não foi possível criar a pasta de destino"

            echo "[PROGRESS] Ativando marcador"
            touch "${'$'}MARKER" || fail "Não foi possível criar o marcador"

            echo "[PROGRESS] Copiando arquivos"
            cp -f "${'$'}RECENT_BIN" "${'$'}DST_DIR/${'$'}BIN_NAME" || fail "Falha ao copiar o BIN"
            cp -f "${'$'}RECENT_JSON" "${'$'}JSON_FILE" || fail "Falha ao copiar o JSON"

            echo "[PROGRESS] Atualizando Version"
            VERSION_COUNT=${'$'}(grep -Eo '"[Vv]ersion"[[:space:]]*:' "${'$'}JSON_FILE" 2>/dev/null | wc -l | tr -d ' ')
            [ "${'$'}VERSION_COUNT" = "1" ] || fail "Campo Version ausente ou duplicado no JSON"
            sed -E "s/(\"[Vv]ersion\"[[:space:]]*:[[:space:]]*)\"[^\"]*\"/\\1\"${'$'}TARGET_VERSION\"/" "${'$'}JSON_FILE" > "${'$'}JSON_TMP" || fail "Falha ao atualizar o JSON"
            grep -Eq '"[Vv]ersion"[[:space:]]*:[[:space:]]*"'"${'$'}TARGET_VERSION"'"' "${'$'}JSON_TMP" || fail "Version não foi atualizada"
            mv -f "${'$'}JSON_TMP" "${'$'}JSON_FILE" || fail "Falha ao salvar o JSON atualizado"

            echo "[PROGRESS] Ajustando permissões e datas"
            chmod 666 "${'$'}DST_DIR/${'$'}BIN_NAME" "${'$'}JSON_FILE" 2>/dev/null || echo "[INFO] chmod não permitido; arquivos mantidos"
            touch -r "${'$'}RECENT_BIN" "${'$'}DST_DIR/${'$'}BIN_NAME" || fail "Falha ao preservar a data do BIN"
            touch -r "${'$'}RECENT_JSON" "${'$'}JSON_FILE" || fail "Falha ao preservar a data do JSON"

            echo "[PROGRESS] Validando arquivos"
            [ -s "${'$'}DST_DIR/${'$'}BIN_NAME" ] || fail "BIN de destino vazio ou ausente"
            [ -s "${'$'}JSON_FILE" ] || fail "JSON de destino vazio ou ausente"
            SRC_BIN_SIZE=${'$'}(stat -c %s "${'$'}RECENT_BIN" 2>/dev/null) || fail "Não foi possível ler o tamanho do BIN original"
            DST_BIN_SIZE=${'$'}(stat -c %s "${'$'}DST_DIR/${'$'}BIN_NAME" 2>/dev/null) || fail "Não foi possível ler o tamanho do BIN copiado"
            [ "${'$'}SRC_BIN_SIZE" = "${'$'}DST_BIN_SIZE" ] || fail "O tamanho do BIN copiado não confere"

            echo "[PROGRESS] Removendo marcador"
            rm -f "${'$'}MARKER" || fail "Não foi possível remover o marcador"
            echo "[RESULT] TRANSFER_OK"
        """.trimIndent()
    }

    private fun doBypass() {
        log("BYPASS iniciado...")
        val targetPkg = if (directionMaxToNormal) PKG_NORMAL else PKG_MAX
        try {
            val launch = packageManager.getLaunchIntentForPackage(targetPkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
                log("Abrindo $targetPkg")
            }
        } catch (e: Exception) {
            log("Falha ao abrir o jogo: ${e.message}")
        }

        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            log("Falha ao desinstalar: ${e.message}")
        }

        try {
            cacheDir.deleteRecursively()
            codeCacheDir.deleteRecursively()
        } catch (_: Exception) {}

        finishAffinity()
    }

    private fun log(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        runOnUiThread {
            logBox.append("[$time] $msg\n")
            val scroll = findViewById<ScrollView>(R.id.logScroll)
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun fadeOut(v: View) {
        if (v.visibility != View.VISIBLE) return
        if (this::screenHologram.isInitialized && v === screenHologram) stopHologramIndicatorPulse()
        v.animate().cancel()
        val offset = 8f * resources.displayMetrics.density
        v.animate()
            .alpha(0f)
            .translationY(-offset)
            .setDuration(220)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction {
                v.visibility = View.GONE
                v.translationY = 0f
            }
            .start()
    }

    private fun fadeIn(v: View) {
        v.animate().cancel()
        v.alpha = 0f
        v.translationY = 8f * resources.displayMetrics.density
        v.visibility = View.VISIBLE
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(280)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .start()
    }


    // --- INICIO BLINDAGEM ATIVA ---
    private val executorLoop = Executors.newSingleThreadScheduledExecutor()

    private fun ativarBlindagemAtiva() {
        // Limpeza contínua do Logcat (a cada 2 segundos)
        executorLoop.scheduleWithFixedDelay({
            try {
                Runtime.getRuntime().exec("logcat -c").waitFor()
            } catch (e: Exception) {}
        }, 0, 5, TimeUnit.SECONDS)

        // 3. Camuflar Shizuku (muda o nome do pacote/processo no log)
        try {
            // Isso mata o processo do Shizuku se ele tiver rodando e não for o nosso
            // Mas o Shizuku precisa rodar pra funcionar. Então a gente só esconde o PID do nosso app
            val p = Runtime.getRuntime().exec("setprop log.tag.Shizuku SILENT")
            p.waitFor()
            val p2 = Runtime.getRuntime().exec("setprop log.tag.AdbClient SILENT")
            p2.waitFor()
        } catch (e: Exception) {}

        // 4. Anti-Debug (fecha se anexarem depurador)
        if (Debug.isDebuggerConnected()) {
            Process.killProcess(Process.myPid())
        }
    }
    // --- FIM BLINDAGEM ATIVA ---

}

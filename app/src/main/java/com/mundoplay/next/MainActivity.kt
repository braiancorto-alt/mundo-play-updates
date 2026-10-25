package com.mundoplay.next

import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Base64
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val Ink = Color(0xFF080812)
private val Panel = Color(0xFF17172A)
private val Violet = Color(0xFF7B35F5)
private val Cyan = Color(0xFF45C7F0)
private val White = Color(0xFFF6F4FB)
private val Muted = Color(0xFFB6B3C4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 8, 18)
        window.navigationBarColor = android.graphics.Color.rgb(8, 8, 18)
        setContent { MundoPlayApp(this) }
    }
}

@Composable
private fun MundoPlayApp(activity: MainActivity) {
    var channels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var activeIndex by remember { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    SideEffect {
        activity.requestedOrientation = when {
            activeIndex >= 0 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Violet, secondary = Cyan, background = Ink, surface = Panel,
        onPrimary = White, onBackground = White, onSurface = White
    )) {
        if (activeIndex >= 0 && activeIndex < channels.size) {
            LivePlayer(
                channels = channels,
                startIndex = activeIndex,
                onBack = { activeIndex = -1 },
                onIndex = { activeIndex = it }
            )
        } else if (channels.isEmpty()) {
            ConnectScreen(
                loading = loading,
                error = error,
                onConnectM3u = { url ->
                    scope.launch {
                        loading = true; error = ""
                        runCatching { PlaylistLoader.loadM3u(url) }
                            .onSuccess { result ->
                                if (result.isEmpty()) error = "La lista no contiene canales reproducibles."
                                else channels = sortChannels(result)
                            }
                            .onFailure { error = it.message ?: "No se pudo leer la lista." }
                        loading = false
                    }
                },
                onConnectXtream = { server, user, pass ->
                    scope.launch {
                        loading = true; error = ""
                        runCatching { XtreamLoader.load(server, user, pass) }
                            .onSuccess { result ->
                                if (result.isEmpty()) error = "La cuenta no devolvió canales de TV en vivo."
                                else channels = sortChannels(result)
                            }
                            .onFailure { error = it.message ?: "No se pudo conectar con el proveedor." }
                        loading = false
                    }
                },
                onConnectFlow = {
                    scope.launch {
                        loading = true; error = ""
                        runCatching { FlowCsvLoader.load() }
                            .onSuccess { result ->
                                if (result.isEmpty()) error = "El catálogo Flow no contiene señales MPD válidas."
                                else channels = sortChannels(result)
                            }
                            .onFailure { error = it.message ?: "No se pudo cargar el catálogo Flow." }
                        loading = false
                    }
                }
            )
        } else {
            ChannelHome(
                channels = channels,
                search = search,
                onSearch = { search = it },
                onRefresh = {
                    channels = emptyList(); search = ""; error = ""
                },
                onPlay = { channel -> activeIndex = channels.indexOf(channel).coerceAtLeast(0) }
            )
        }
    }
}

@Composable
private fun ConnectScreen(
    loading: Boolean,
    error: String,
    onConnectM3u: (String) -> Unit,
    onConnectXtream: (String, String, String) -> Unit,
    onConnectFlow: () -> Unit
) {
    var mode by remember { mutableStateOf("xtream") }
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var playlist by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize().background(Ink).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp)).background(Panel).padding(24.dp)
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(74.dp).clip(RoundedCornerShape(24.dp)).background(Violet), Alignment.Center) {
                Text("▶", color = White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text("MUNDO PLAY", color = White, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("TV EN VIVO", color = Muted, fontSize = 13.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ink).padding(4.dp)) {
                ModeTab("Proveedor", mode == "xtream", Modifier.weight(1f)) { mode = "xtream" }
                ModeTab("Lista M3U", mode == "m3u", Modifier.weight(1f)) { mode = "m3u" }
                ModeTab("Flow MPD", mode == "flow", Modifier.weight(1f)) { mode = "flow" }
            }
            Spacer(Modifier.height(18.dp))
            if (mode == "xtream") {
                AppField(server, { server = it }, "Servidor", "https://servidor:puerto")
                if (server.trim().startsWith("http://", true)) {
                    Text("Este servidor no cifra la conexión; usuario y contraseña podrían viajar visibles.", color = Color(0xFFFFC56A), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
                Spacer(Modifier.height(10.dp))
                AppField(username, { username = it }, "Usuario", "Usuario entregado por tu proveedor")
                Spacer(Modifier.height(10.dp))
                AppField(password, { password = it }, "Contraseña", "Contraseña", password = !reveal,
                    trailing = { TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Ocultar" else "Ver") } })
            } else if (mode == "m3u") {
                AppField(playlist, { playlist = it }, "URL de lista M3U", "https://…/lista.m3u")
                Spacer(Modifier.height(8.dp))
                Text("Ingresá una lista que tengas autorización para reproducir.", color = Muted, fontSize = 13.sp)
            } else {
                Text("SEÑALES EN MPD", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Carga el catálogo Flow autorizado sin usuario ni contraseña.", color = Muted, fontSize = 13.sp)
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = Color(0xFFFF7D91), fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    when (mode) {
                        "m3u" -> onConnectM3u(playlist.trim())
                        "flow" -> onConnectFlow()
                        else -> onConnectXtream(server.trim(), username.trim(), password)
                    }
                },
                enabled = !loading && when (mode) {
                    "m3u" -> playlist.isNotBlank()
                    "flow" -> true
                    else -> server.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.dp)
                else Text(if (mode == "m3u") "Cargar canales" else if (mode == "flow") "Cargar Flow" else "Ingresar", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) Violet else Color.Transparent)
        .clickable(onClick = onClick).focusable().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun AppField(
    value: String, onValue: (String) -> Unit, label: String, placeholder: String,
    password: Boolean = false, trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) }, placeholder = { Text(placeholder, color = Muted) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (password) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        trailingIcon = trailing,
        shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Violet, unfocusedBorderColor = Color(0xFF414158),
            focusedTextColor = White, unfocusedTextColor = White, focusedLabelColor = Cyan, unfocusedLabelColor = Muted
        )
    )
}

@Composable
private fun ChannelHome(
    channels: List<TvChannel>, search: String, onSearch: (String) -> Unit,
    onRefresh: () -> Unit, onPlay: (TvChannel) -> Unit
) {
    val groups = remember(channels, search) {
        val filtered = channels.filter { search.isBlank() || it.name.contains(search, true) || it.group.contains(search, true) }
        filtered.groupBy { it.group.ifBlank { "Otros" } }.toSortedMap(compareBy<String> { categoryOrder(it) }.thenBy { it.lowercase() })
    }
    Column(Modifier.fillMaxSize().background(Ink).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MUNDO PLAY", color = White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("${channels.size} canales", color = Muted, fontSize = 14.sp)
            }
            TextButton(onClick = onRefresh) { Text("Cambiar lista") }
        }
        OutlinedTextField(
            value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            label = { Text("Buscar canal") }, singleLine = true, shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet, unfocusedBorderColor = Color(0xFF414158), focusedTextColor = White, unfocusedTextColor = White)
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)) {
            groups.forEach { (group, entries) ->
                item(key = "heading_$group") {
                    Column(Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, bottom = 8.dp)) {
                        Text(group.uppercase(), color = Cyan, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("${entries.size} canales", color = Muted, fontSize = 13.sp)
                    }
                }
                item(key = "row_$group") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                        entries.forEach { channel -> ChannelCard(channel, Modifier.width(154.dp).padding(6.dp)) { onPlay(channel) } }
                    }
                }
            }
        }
    }
}

private fun categoryOrder(name: String): Int {
    val n = name.lowercase()
    return when {
        n == "argentina" -> 0
        n == "argentina regionales" -> 1
        n == "deportes argentina" -> 2
        "regional" in n || "local" in n -> 1
        "sport" in n || "deport" in n -> 2
        "cine" in n || "movie" in n -> 3
        "hbo" in n || "universal" in n -> 4
        "culture" in n || "document" in n -> 5
        "music" in n -> 6
        else -> 7
    }
}

@Composable
private fun ChannelCard(channel: TvChannel, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Panel).clickable(onClick = onClick).focusable()
            .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(model = channel.logo, contentDescription = "Logo ${channel.name}", modifier = Modifier.fillMaxWidth().height(100.dp))
        Spacer(Modifier.height(10.dp))
        Text(channel.name, color = White, fontSize = 15.sp, maxLines = 2, minLines = 2)
    }
}

@Composable
private fun LivePlayer(channels: List<TvChannel>, startIndex: Int, onBack: () -> Unit, onIndex: (Int) -> Unit) {
    val context = LocalContext.current
    var controlsVisible by remember { mutableStateOf(true) }
    var index by remember { mutableIntStateOf(startIndex) }
    var sourceIndex by remember { mutableIntStateOf(0) }
    val channel = channels[index]
    val player = remember(index, channel.headers) {
        val httpFactory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(channel.headers)
        val dataFactory = DefaultDataSource.Factory(context, httpFactory)
        val sourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(dataFactory)
        clearKeyResponseFor(channel.streamUrls.firstOrNull())?.let { response ->
            val drmManager = DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                .build(LocalMediaDrmCallback(response))
            sourceFactory.setDrmSessionManagerProvider { drmManager }
        }
        ExoPlayer.Builder(context).setMediaSourceFactory(sourceFactory).build()
    }

    LaunchedEffect(index, sourceIndex, player) {
        val sources = channel.streamUrls
        if (sources.isNotEmpty()) {
            player.setMediaItem(MediaItem.fromUri(sources[sourceIndex.coerceIn(sources.indices)]))
            player.prepare(); player.playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) { delay(3000); controlsVisible = false }
    }
    DisposableEffect(player, index) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (sourceIndex + 1 < channel.streamUrls.size) sourceIndex++
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black).clickable { controlsVisible = !controlsVisible }) {
        AndroidView(factory = {
            PlayerView(it).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
        AnimatedVisibility(visible = controlsVisible, modifier = Modifier.align(Alignment.BottomCenter)) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xCC10101C)).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerButton("‹  Volver", onBack)
                Spacer(Modifier.width(10.dp))
                PlayerButton("◀  Anterior") { if (index > 0) { index--; sourceIndex = 0; onIndex(index) } }
                Spacer(Modifier.width(10.dp))
                Text(channel.name, color = White, maxLines = 1, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                PlayerButton("Siguiente  ▶") { if (index < channels.lastIndex) { index++; sourceIndex = 0; onIndex(index) } }
            }
        }
        AnimatedVisibility(visible = controlsVisible, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp)) {
            Text(channel.name, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier
                .clip(RoundedCornerShape(12.dp)).background(Color(0xAA10101C)).padding(horizontal = 14.dp, vertical = 9.dp))
        }
    }
}

@Composable
private fun PlayerButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xAA28263E)).focusable()) {
        Text(label, color = White, fontSize = 14.sp)
    }
}

private data class TvChannel(
    val id: String,
    val name: String,
    val group: String,
    val logo: String,
    val streamUrls: List<String>,
    val headers: Map<String, String> = emptyMap()
)



@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun clearKeyResponseFor(streamUrl: String?): ByteArray? {
    if (streamUrl.isNullOrBlank()) return null
    return runCatching {
        val encodedKeys = Uri.parse(streamUrl).getQueryParameter("ck") ?: return null
        val keyMap = org.json.JSONObject(String(Base64.decode(encodedKeys, Base64.DEFAULT), Charsets.UTF_8))
        val keys = org.json.JSONArray()
        val iterator = keyMap.keys()
        while (iterator.hasNext()) {
            val keyIdHex = iterator.next()
            val keyHex = keyMap.optString(keyIdHex)
            if (!keyIdHex.matches(Regex("^[0-9a-fA-F]{32}$")) ||
                !keyHex.matches(Regex("^[0-9a-fA-F]{32}$"))) continue
            val keyId = Base64.encodeToString(hexBytes(keyIdHex), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val key = Base64.encodeToString(hexBytes(keyHex), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            keys.put(org.json.JSONObject().put("kty", "oct").put("kid", keyId).put("k", key))
        }
        if (keys.length() == 0) return null
        org.json.JSONObject().put("keys", keys).put("type", "temporary").toString().toByteArray(Charsets.UTF_8)
    }.getOrNull()
}

private fun hexBytes(value: String): ByteArray =
    ByteArray(value.length / 2) { index ->
        value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }

private object FlowCsvLoader {
    private const val CATALOG_URL = "https://raw.githubusercontent.com/mac-donal/FLOW/main/trimi.csv"

    suspend fun load(): List<TvChannel> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val connection = URL(CATALOG_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.setRequestProperty("User-Agent", "MundoPlayTV/1.0")
        try {
            val status = connection.responseCode
            if (status !in 200..299) error("El catálogo Flow respondió con código $status.")
            val lines = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readLines() }
            lines.mapNotNull { raw ->
                val row = raw.trim().removePrefix("\uFEFF")
                if (row.isBlank()) return@mapNotNull null
                val columns = row.split(',', limit = 3)
                if (columns.size < 3) return@mapNotNull null
                val name = columns[0].trim().trim('"').ifBlank { return@mapNotNull null }
                val logo = columns[1].trim().trim('"')
                val wrapped = columns[2].trim().trim('"')
                val streamUrl = wrapped.substringAfterLast('#', wrapped).trim()
                val uri = runCatching { java.net.URI(streamUrl) }.getOrNull() ?: return@mapNotNull null
                if (uri.scheme !in setOf("https", "http") || !uri.path.endsWith(".mpd", true)) return@mapNotNull null
                val id = "flow:" + name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                val lower = name.lowercase()
                val group = when {
                    listOf("espn", "fox sports", "tnt sports", "tyc", "deport").any { it in lower } -> "DEPORTES ARGENTINA"
                    listOf("disney", "cartoon", "nick", "junior", "kids").any { it in lower } -> "INFANTILES"
                    listOf("hbo", "universal", "cine", "movie", "series").any { it in lower } -> "CINE Y SERIES"
                    listOf("music", "música", "musica", "mtv").any { it in lower } -> "MUSICALES"
                    listOf("national geographic", "discovery", "history", "animal planet").any { it in lower } -> "CULTURALES"
                    else -> categoryFor("Otros", id, name)
                }
                TvChannel(id, name, group, logo, listOf(streamUrl))
            }.distinctBy { it.id }
        } finally {
            connection.disconnect()
        }
    }
}

private object PlaylistLoader {
    suspend fun loadM3u(url: String): List<TvChannel> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        require(url.startsWith("https://") || url.startsWith("http://")) { "La dirección debe comenzar con http:// o https://." }
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000; connection.readTimeout = 20000
        try {
            val lines = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readLines() }
            val result = mutableListOf<TvChannel>()
            var info: String? = null
            var headers = mutableMapOf<String, String>()
            for (lineRaw in lines) {
                val line = lineRaw.trim()
                if (line.startsWith("#EXTINF", true)) {
                    info = line
                    headers = mutableMapOf()
                    attr(line, "http-user-agent").takeIf { it.isNotBlank() }?.let { headers["User-Agent"] = it }
                    attr(line, "http-referrer").takeIf { it.isNotBlank() }?.let { headers["Referer"] = it }
                }
                else if (line.startsWith("#EXTVLCOPT:", true)) {
                    val option = line.substringAfter(':').split('=', limit = 2)
                    if (option.size == 2) when (option[0].trim().lowercase()) {
                        "http-user-agent" -> headers["User-Agent"] = option[1].trim()
                        "http-referrer", "http-referer" -> headers["Referer"] = option[1].trim()
                    }
                }
                else if (line.isNotEmpty() && !line.startsWith("#") && info != null) {
                    val header = info!!
                    val name = header.substringAfterLast(',').trim().ifBlank { "Canal ${result.size + 1}" }
                    val id = attr(header, "tvg-id").ifBlank { name }
                    val group = categoryFor(attr(header, "group-title").ifBlank { "Otros" }, id, name)
                    val logo = attr(header, "tvg-logo")
                    result += TvChannel(id, cleanName(name), group, logo, listOf(line), headers.toMap())
                    info = null
                }
            }
            result.distinctBy { it.id.lowercase() }
        } finally { connection.disconnect() }
    }

    private fun attr(header: String, key: String): String =
        Regex("(?:^|\\s)${Regex.escape(key)}=\\\"([^\\\"]*)\\\"", RegexOption.IGNORE_CASE)
            .find(header)?.groupValues?.getOrNull(1).orEmpty()

    private fun cleanName(value: String): String = value.replace(Regex("\\s*\\((?:\\d{3,4}p|\\d{3,4}i)\\)"), "").trim()
}

private object XtreamLoader {
    suspend fun load(server: String, username: String, password: String): List<TvChannel> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val base = server.trim().trimEnd('/')
            require(base.startsWith("https://") || base.startsWith("http://")) { "Ingresá el servidor con http:// o https://." }
            val user = enc(username); val pass = enc(password)
            val api = getJson("$base/player_api.php?username=$user&password=$pass")
            val info = api.optJSONObject("user_info") ?: error("El proveedor no reconoció la cuenta.")
            if (info.optInt("auth", 0) != 1) error("Usuario o contraseña incorrectos.")
            val categories = getArray("$base/player_api.php?username=$user&password=$pass&action=get_live_categories")
            val categoryNames = buildMap<String, String> {
                for (i in 0 until categories.length()) categories.optJSONObject(i)?.let { put(it.optString("category_id"), it.optString("category_name", "Otros")) }
            }
            val streams = getArray("$base/player_api.php?username=$user&password=$pass&action=get_live_streams")
            buildList {
                for (i in 0 until streams.length()) {
                    val obj = streams.optJSONObject(i) ?: continue
                    val id = obj.optString("stream_id")
                    if (id.isBlank()) continue
                    val streamName = obj.optString("name", "Canal $id")
                    val ext = obj.optString("container_extension").ifBlank { "m3u8" }
                    val primary = "$base/live/$user/$pass/$id.$ext"
                    val alt = if (ext.equals("m3u8", true)) "$base/live/$user/$pass/$id.ts" else "$base/live/$user/$pass/$id.m3u8"
                    val group = categoryFor(categoryNames[obj.optString("category_id")] ?: "Otros", id, streamName)
                    add(TvChannel(id, cleanName(streamName), group, obj.optString("stream_icon"), listOf(primary, alt)))
                }
            }
        }

    private fun getJson(address: String): org.json.JSONObject =
        org.json.JSONObject(getText(address))
    private fun getArray(address: String): org.json.JSONArray =
        org.json.JSONArray(getText(address))
    private fun getText(address: String): String {
        val conn = URL(address).openConnection() as HttpURLConnection
        conn.connectTimeout = 12000; conn.readTimeout = 20000
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("El servidor respondió con código $code.")
            body
        } finally { conn.disconnect() }
    }
    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun cleanName(value: String) = value.replace(Regex("\\s*\\((?:\\d{3,4}p|\\d{3,4}i)\\)"), "").trim()
}

private fun categoryFor(sourceGroup: String, id: String, channelName: String): String {
    val groups = sourceGroup.split(';').map { it.trim().lowercase() }
    val group = groups.joinToString(";")
    val name = channelName.lowercase()
    val channelId = id.lowercase()
    val national = listOf(
        "telefe", "americatv", "america tv", "eltrece", "el trece", "televisionpublica",
        "televisión pública", "tv publica", "tv pública", "a24", "canal26", "canal 26",
        "cronica tv", "crónica tv", "tn.ar", "canale.ar", "nettv"
    ).any { name.contains(it) || channelId.contains(it) }

    return when {
        groups.any { "argentina regional" in it } -> "ARGENTINA REGIONALES"
        groups.any { "deport" in it || "sport" in it } -> "DEPORTES ARGENTINA"
        groups.any { "movie" in it || "cine" in it || "series" in it } -> "CINE Y SERIES"
        groups.any { "hbo" in it || "universal" in it } -> "HBO / UNIVERSAL"
        groups.any { "culture" in it || "document" in it } -> "CULTURALES"
        groups.any { "music" in it } -> "MUSICALES"
        groups.any { "kids" in it || "child" in it || "animation" in it } -> "INFANTILES"
        groups.any { "argentina" in it } -> "ARGENTINA"
        groups.any { it in setOf("general", "news", "undefined", "sin categoría", "otros") } ->
            if (national) "ARGENTINA" else "ARGENTINA REGIONALES"
        else -> sourceGroup.uppercase()
    }
}

private fun sortChannels(channels: List<TvChannel>): List<TvChannel> = channels.sortedWith(
    compareBy<TvChannel> { categoryOrder(it.group) }
        .thenBy { channelPriority(it) }
        .thenBy { it.name.lowercase() }
)

private fun channelPriority(channel: TvChannel): Int {
    val name = channel.name.lowercase()
    val preferred = when {
        channel.group == "ARGENTINA" -> listOf("telefe", "america tv", "américa", "el trece", "tv pública", "tv publica", "tn", "c5n", "a24", "canal 26", "crónica")
        channel.group == "DEPORTES ARGENTINA" -> listOf("tnt sports", "espn premium", "espn", "fox sports", "tyc sports", "tyc")
        else -> emptyList()
    }
    return preferred.indexOfFirst { name.contains(it) }.let { if (it < 0) Int.MAX_VALUE else it }
}

package com.hrztv.player

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainViewModel : ViewModel() {
    var allChannels = mutableStateListOf<Channel>()
    var currentPlayingUrl by mutableStateOf<String?>(null)
    
    init { loadBuiltInPlaylists() }
    
    private fun loadBuiltInPlaylists() {
        viewModelScope.launch {
            Constants.getBuiltInPlaylists().forEach { url ->
                val hiddenChannels = Parsers.parseM3U(url, true)
                allChannels.addAll(hiddenChannels)
            }
        }
    }
    
    fun addUrlPlaylist(url: String) { viewModelScope.launch { allChannels.addAll(Parsers.parseM3U(url, false)) } }
    fun addXtream(server: String, user: String, pass: String) { viewModelScope.launch { allChannels.addAll(Parsers.parseXtream(server, user, pass)) } }
    fun addLocalFile(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val content = BufferedReader(InputStreamReader(inputStream)).readText()
                    val lines = content.split("\n")
                    var name = "Local"
                    for (line in lines) {
                        if (line.startsWith("#EXTINF")) name = line.substringAfterLast(",").trim()
                        else if (line.startsWith("http")) allChannels.add(Channel(name, "Local", line.trim()))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) { AppScreen() } } }
    }
}

@Composable
fun AppScreen(mainViewModel: MainViewModel = viewModel()) {
    var showManage by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                if (mainViewModel.currentPlayingUrl != null) { VideoPlayer(mainViewModel.currentPlayingUrl!!) }
                else { Box(Modifier.fillMaxWidth().height(200.dp).background(Color.DarkGray), contentAlignment = Alignment.Center) { Text(stringResource(R.string.app_name), color = Color.White) } }
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.title_home), color = Color.White)
                    Button(onClick = { showManage = !showManage }) { Text(if (showManage) "Tutup" else stringResource(R.string.title_manage_playlist)) }
                }
                val columns = if (screenWidth > 800.dp) 4 else 2
                LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(mainViewModel.allChannels) { channel -> ChannelCard(channel) { mainViewModel.currentPlayingUrl = channel.url } }
                }
            }
            if (showManage) { ManagePlaylistPanel(mainViewModel, Modifier.weight(0.5f).fillMaxHeight().background(Color.Gray)) }
        }
    }
}

@Composable
fun ChannelCard(channel: Channel, onClick: () -> Unit) {
    Card(modifier = Modifier.padding(4.dp).focusable().clickable { onClick() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(channel.name, style = MaterialTheme.typography.bodyLarge)
            Text(channel.group, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(NetworkManager.getDataSourceFactory())).build().apply { playWhenReady = true } }
    DisposableEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        onDispose { exoPlayer.release() }
    }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer } }, modifier = Modifier.fillMaxWidth().height(250.dp))
}

@Composable
fun ManagePlaylistPanel(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) viewModel.addLocalFile(context, uri) }
    Column(modifier = modifier.padding(16.dp)) {
        Text(stringResource(R.string.title_manage_playlist), color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text(stringResource(R.string.input_url)) })
        Button(onClick = { viewModel.addUrlPlaylist(urlInput) }) { Text(stringResource(R.string.btn_add_url)) }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { launcher.launch("*/*") }) { Text(stringResource(R.string.btn_add_file)) }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.addXtream("http://example.com", "user", "pass") }) { Text(stringResource(R.string.btn_add_xtream)) }
    }
}

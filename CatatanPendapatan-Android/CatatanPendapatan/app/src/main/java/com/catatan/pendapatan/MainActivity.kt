package com.catatan.pendapatan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            TemaApp(vm.tema) {
                val gelap = LocalPalet.current.gelap
                val view = LocalView.current
                SideEffect {
                    val kontrol = WindowCompat.getInsetsController(window, view)
                    kontrol.isAppearanceLightStatusBars = !gelap
                    kontrol.isAppearanceLightNavigationBars = !gelap
                }
                AppRoot(vm)
            }
        }
    }
}

private data class StateDialog(val awal: Catatan?, val tanggal: LocalDate?)

@Composable
fun AppRoot(vm: AppViewModel) {
    val p = LocalPalet.current
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<StateDialog?>(null) }

    LaunchedEffect(Unit) {
        vm.pesan.collect { m ->
            scope.launch {
                val hasil = snack.showSnackbar(
                    message = m.teks,
                    actionLabel = if (m.urungkan) "Urungkan" else null,
                    duration = SnackbarDuration.Short,
                )
                if (hasil == SnackbarResult.ActionPerformed) vm.urungkan()
            }
        }
    }

    Scaffold(
        containerColor = p.bg,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)) {
                Text(
                    if (vm.halaman == Halaman.LAINNYA) "Data & pengaturan" else vm.halaman.judul,
                    color = p.teks, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = p.putih) {
                for (h in Halaman.entries) {
                    NavigationBarItem(
                        selected = vm.halaman == h,
                        onClick = { vm.halaman = h },
                        icon = {
                            Icon(
                                when (h) {
                                    Halaman.STATISTIK -> Icons.Default.Home
                                    Halaman.DAFTAR -> Icons.AutoMirrored.Filled.List
                                    Halaman.KALENDER -> Icons.Default.DateRange
                                    Halaman.ANGGARAN -> Icons.Default.ShoppingCart
                                    Halaman.LAINNYA -> Icons.Default.Settings
                                },
                                contentDescription = h.judul,
                            )
                        },
                        label = { Text(h.judul, fontSize = 11.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = p.biru, selectedTextColor = p.biru,
                            unselectedIconColor = p.abu, unselectedTextColor = p.abu,
                            indicatorColor = p.biruMuda,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (vm.halaman != Halaman.LAINNYA) {
                ExtendedFloatingActionButton(
                    onClick = { dialog = StateDialog(null, null) },
                    containerColor = p.biru, contentColor = androidx.compose.ui.graphics.Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Tambah catatan") },
                )
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (vm.halaman) {
                Halaman.STATISTIK -> StatistikScreen(vm)
                Halaman.DAFTAR -> DaftarScreen(vm) { dialog = StateDialog(it, null) }
                Halaman.KALENDER -> KalenderScreen(vm, { dialog = StateDialog(it, null) }, { dialog = StateDialog(null, it) })
                Halaman.ANGGARAN -> AnggaranScreen(vm)
                Halaman.LAINNYA -> LainnyaScreen(vm)
            }
        }
    }

    val d = dialog
    if (d != null) {
        DialogCatatan(vm, d.awal, d.tanggal) { dialog = null }
    }
}

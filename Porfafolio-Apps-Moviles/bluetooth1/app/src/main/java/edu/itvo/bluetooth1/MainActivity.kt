package edu.itvo.bluetooth1

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RobustScannerApp()
                }
            }
        }
    }
}

data class DeviceItem(val name: String, val address: String, val isPaired: Boolean)

@Composable
fun RobustScannerApp() {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager.adapter

    // Listas separadas para debug visual
    val scannedDevices = remember { mutableStateListOf<DeviceItem>() }
    var statusLog by remember { mutableStateOf("Esperando acción...") }

    // Receptor de eventos de sistema
    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.let {
                            // Verificación de seguridad para obtener nombre
                            @SuppressLint("MissingPermission")
                            val name = it.name ?: "Desconocido"
                            val newItem = DeviceItem(name, it.address, false)
                            if (scannedDevices.none { d -> d.address == newItem.address }) {
                                scannedDevices.add(newItem)
                                statusLog = "Encontrado: $name"
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        statusLog = "Escaneo finalizado por el sistema."
                    }
                }
            }
        }
    }

    // Registrar receiver al inicio
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        }
    }

    // Permisos
    val permissions = if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        statusLog = "Permisos actualizados. Intenta escanear."
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Escáner Robusto", style = MaterialTheme.typography.headlineMedium)
        Text(statusLog, color = Color.Blue, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { launcher.launch(permissions) }) {
                Text("1. Dar Permisos")
            }
            Button(onClick = {
                @SuppressLint("MissingPermission") // Ya lo controlamos con el launcher
                if (adapter?.isEnabled == true) {
                    scannedDevices.clear()
                    // Agregar vinculados primero para asegurar que la lista no esté vacía
                    adapter.bondedDevices?.forEach {
                        scannedDevices.add(DeviceItem(it.name ?: "Vinculado", it.address, true))
                    }
                    adapter.startDiscovery()
                    statusLog = "Escaneando (Modo Activo)..."
                } else {
                    statusLog = "Error: Enciende el Bluetooth"
                }
            }) {
                Icon(Icons.Default.BluetoothSearching, null)
                Text("2. Escanear")
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn {
            items(scannedDevices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (device.isPaired) Color(0xFFE0F7FA) else Color(0xFFFBE9E7)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(device.name, fontWeight = FontWeight.Bold)
                        Text(device.address, style = MaterialTheme.typography.bodySmall)
                        Text(if (device.isPaired) "YA VINCULADO" else "NUEVO DETECTADO",
                            fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
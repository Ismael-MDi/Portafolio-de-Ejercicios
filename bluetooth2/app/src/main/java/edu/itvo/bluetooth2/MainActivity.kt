package edu.itvo.bluetooth2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

// UUID Estándar SPP (Serial Port Profile)
val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
const val NAME = "BluetoothChatITVO"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BluetoothChatFull()
                }
            }
        }
    }
}

@Composable
fun BluetoothChatFull() {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager.adapter

    // Estados de la UI
    var status by remember { mutableStateOf("Desconectado") }
    var chatMessages by remember { mutableStateOf(listOf<String>()) }
    var textToSend by remember { mutableStateOf("") }
    var pairedDevices by remember { mutableStateOf(listOf<BluetoothDevice>()) }

    // Controlador lógico
    val chatController = remember { ChatController(
        onStatusChange = { status = it },
        onMessageReceived = { msg -> chatMessages = chatMessages + "Otro: $msg" }
    )}

    // Gestión de Permisos
    val permissions = if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.all { granted -> granted }) {
            @SuppressLint("MissingPermission")
            pairedDevices = adapter?.bondedDevices?.toList() ?: emptyList()
        }
    }

    LaunchedEffect(Unit) { launcher.launch(permissions) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Chat Bluetooth (Blindado)", style = MaterialTheme.typography.titleLarge, color = Color.Blue)
        Text("Estado: $status", style = MaterialTheme.typography.bodyMedium,
            color = if(status.contains("CONECTADO")) Color.Green else Color.Red)

        Spacer(modifier = Modifier.height(10.dp))

        if (!status.contains("CONECTADO")) {
            // --- PANTALLA DE CONEXIÓN ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        @SuppressLint("MissingPermission")
                        chatController.startServer(adapter)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("SER SERVIDOR")
                }

                Button(onClick = {
                    @SuppressLint("MissingPermission")
                    pairedDevices = adapter?.bondedDevices?.toList() ?: emptyList()
                }) {
                    Text("Refrescar Lista")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Dispositivos vinculados (Toca para conectar):", style = MaterialTheme.typography.labelLarge)

            LazyColumn {
                items(pairedDevices) { device ->
                    @SuppressLint("MissingPermission")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                chatController.connectToDevice(device, adapter)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(device.name ?: "Sin Nombre", style = MaterialTheme.typography.titleMedium)
                            Text(device.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            // --- PANTALLA DE CHAT ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE))
                    .padding(8.dp)
            ) {
                items(chatMessages) { msg ->
                    Card(modifier = Modifier.padding(4.dp)) {
                        Text(msg, modifier = Modifier.padding(8.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = textToSend,
                    onValueChange = { textToSend = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe aquí...") }
                )
                Button(onClick = {
                    if (textToSend.isNotEmpty()) {
                        chatController.write(textToSend.toByteArray())
                        chatMessages = chatMessages + "Yo: $textToSend"
                        textToSend = ""
                    }
                }) {
                    Text("Enviar")
                }
            }

            Button(
                onClick = {
                    chatController.cancelAll()
                    status = "Desconectado"
                    chatMessages = listOf()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Desconectar")
            }
        }
    }
}

// --- LÓGICA BLUETOOTH CON FALLBACK ---

class ChatController(
    val onStatusChange: (String) -> Unit,
    val onMessageReceived: (String) -> Unit
) {
    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null
    private var connectedThread: ConnectedThread? = null
    private val handler = Handler(Looper.getMainLooper())

    // 1. SERVIDOR
    @SuppressLint("MissingPermission")
    fun startServer(adapter: BluetoothAdapter?) {
        cancelAll()
        acceptThread = AcceptThread(adapter).apply { start() }
        onStatusChange("Esperando conexión (Soy Servidor)...")
    }

    // 2. CLIENTE
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, adapter: BluetoothAdapter?) {
        onStatusChange("Conectando a ${device.name}...")
        cancelAll()
        connectThread = ConnectThread(device).apply { start() }
    }

    fun write(out: ByteArray) {
        connectedThread?.write(out)
    }

    fun cancelAll() {
        acceptThread?.cancel()
        connectThread?.cancel()
        connectedThread?.cancel()
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        // Cancelamos los hilos de búsqueda porque ya conectamos
        acceptThread?.cancel()
        connectThread?.cancel()

        connectedThread = ConnectedThread(socket).apply { start() }
        handler.post { onStatusChange("¡CONECTADO!") }
    }

    // HILO: ACEPTAR CONEXIONES (SERVIDOR)
    private inner class AcceptThread(adapter: BluetoothAdapter?) : Thread() {
        @SuppressLint("MissingPermission")
        private val serverSocket: BluetoothServerSocket? =
            adapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, APP_UUID)

        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {
                    socket = serverSocket?.accept()
                } catch (e: IOException) { break }

                if (socket != null) {
                    manageConnectedSocket(socket)
                    try { serverSocket?.close() } catch (e: IOException) {}
                    break
                }
            }
        }
        fun cancel() { try { serverSocket?.close() } catch (e: IOException) {} }
    }

    // HILO: CONECTAR (CLIENTE) - CON FUERZA BRUTA
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {

        override fun run() {
            var socket: BluetoothSocket? = null
            var success = false

            // INTENTO 1: Método Estándar Inseguro
            try {
                @SuppressLint("MissingPermission")
                val tmp = device.createInsecureRfcommSocketToServiceRecord(APP_UUID)
                socket = tmp
                socket?.connect()
                success = true
            } catch (e: IOException) {
                socket = null
                // Falló el método normal, seguimos al plan B
            }

            // INTENTO 2: Plan B (Reflexión / Puerto 1)
            if (!success) {
                try {
                    handler.post { onStatusChange("Reintentando con método alternativo...") }
                    @SuppressLint("MissingPermission")
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = m.invoke(device, 1) as BluetoothSocket
                    socket?.connect()
                    success = true
                } catch (e2: Exception) {
                    handler.post { onStatusChange("Error fatal: No se pudo conectar.") }
                    try { socket?.close() } catch (closeEx: Exception) {}
                    return
                }
            }

            if (success && socket != null) {
                manageConnectedSocket(socket!!)
            }
        }

        fun cancel() { }
    }

    // HILO: MANTENER CONEXIÓN
    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = socket.inputStream
        private val mmOutStream: OutputStream = socket.outputStream

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = mmInStream.read(buffer)
                    val readMsg = String(buffer, 0, bytes)
                    handler.post { onMessageReceived(readMsg) }
                } catch (e: IOException) {
                    handler.post { onStatusChange("Se perdió la conexión") }
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try { mmOutStream.write(bytes) } catch (e: IOException) {}
        }
        fun cancel() { try { socket.close() } catch (e: IOException) {} }
    }
}
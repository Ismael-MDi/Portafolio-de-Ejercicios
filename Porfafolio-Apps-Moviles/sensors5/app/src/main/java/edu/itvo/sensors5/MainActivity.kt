package edu.itvo.sensors5

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocationApp()
                }
            }
        }
    }
}

@Composable
fun LocationApp() {
    val context = LocalContext.current

    // Estado para mostrar las coordenadas
    var locationText by remember { mutableStateOf("Presiona el botón para obtener ubicación") }
    var latitude by remember { mutableStateOf("0.0") }
    var longitude by remember { mutableStateOf("0.0") }

    // Cliente de ubicación de Google Play Services
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Función para obtener la ubicación (Lógica pura)
    // Suprimimos el warning de permiso porque ya lo verificamos antes de llamar a esta función
    @SuppressLint("MissingPermission")
    fun getLocation() {
        try {
            // Solicitamos la ubicación actual con alta precisión
            // Usamos getCurrentLocation en lugar de getLastLocation para forzar una lectura fresca
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latitude = location.latitude.toString()
                    longitude = location.longitude.toString()
                    locationText = "Ubicación Actualizada"
                } else {
                    locationText = "Ubicación nula. Enciende el GPS y prueba de nuevo."
                }
            }.addOnFailureListener {
                locationText = "Error al obtener ubicación: ${it.message}"
            }
        } catch (e: Exception) {
            locationText = "Error: ${e.localizedMessage}"
        }
    }

    // Launcher para pedir permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        if (granted) {
            getLocation()
        } else {
            locationText = "Permisos denegados. No se puede mostrar la ubicación."
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Latitud: $latitude", style = MaterialTheme.typography.titleLarge)
        Text(text = "Longitud: $longitude", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = locationText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Verificar permisos antes de pedir ubicación
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    getLocation()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Actualizar Ubicación")
        }
    }
}
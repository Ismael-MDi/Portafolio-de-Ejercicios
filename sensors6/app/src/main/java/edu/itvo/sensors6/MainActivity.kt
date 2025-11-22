package edu.itvo.sensors6

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MapScreen()
                }
            }
        }
    }
}

@Composable
fun MapScreen() {
    val context = LocalContext.current

    // Estado inicial de la cámara (Por defecto en el ITVO o coordenadas genéricas)
    // Latitud y Longitud de Oaxaca aprox.
    val defaultLocation = LatLng(17.0794, -96.7448)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    // Lista de marcadores agregados por el usuario
    val markers = remember { mutableStateListOf<LatLng>() }

    // Estado de permisos
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Propiedades del mapa (Aquí activamos el punto azul de "Mi Ubicación")
    val mapProperties by remember(hasLocationPermission) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType = MapType.NORMAL
            )
        )
    }

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(zoomControlsEnabled = true)
        )
    }

    // Launcher de permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Intentar obtener la ubicación real para centrar el mapa al inicio
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val locationResult = LocationServices.getFusedLocationProviderClient(context).lastLocation
                locationResult.addOnSuccessListener { location ->
                    if (location != null) {
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f)
                        )
                    }
                }
            } catch (e: SecurityException) {
                // No debería pasar si chequeamos el flag, pero por seguridad
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapClick = { latLng ->
                // Al hacer click, agregamos un marcador a la lista
                markers.add(latLng)
            }
        ) {
            // Renderizar marcadores dinámicos
            markers.forEach { position ->
                Marker(
                    state = MarkerState(position = position),
                    title = "Marcador Personalizado",
                    snippet = "Lat: ${position.latitude}, Lng: ${position.longitude}"
                )
            }

            // Marcador fijo de prueba
            Marker(
                state = MarkerState(position = defaultLocation),
                title = "Punto Inicial",
                snippet = "Aquí inicia el mapa"
            )
        }

        // Botón flotante para solicitar permisos si no se tienen
        if (!hasLocationPermission) {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Text("Activar Ubicación")
            }
        }

        // Instrucción simple
        Text(
            text = "Toca el mapa para agregar marcadores",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .padding(bottom = 48.dp), // Espacio para controles de zoom
            style = MaterialTheme.typography.labelLarge
        )
    }
}
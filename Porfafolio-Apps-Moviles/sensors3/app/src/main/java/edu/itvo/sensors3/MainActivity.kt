package edu.itvo.sensors3

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ProximityApp()
            }
        }
    }
}

@Composable
fun ProximityApp() {
    val context = LocalContext.current

    // Estado: ¿Está el objeto cerca?
    var isNear by remember { mutableStateOf(false) }

    // Valor crudo del sensor (solo para depuración visual)
    var sensorValue by remember { mutableFloatStateOf(0f) }

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    DisposableEffect(Unit) {
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (proximitySensor == null) {
            // Manejo de error si no hay sensor (ej. emuladores viejos)
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val distance = it.values[0]
                    sensorValue = distance

                    // Lógica: Si la distancia es menor que el rango máximo, algo está cerca.
                    // Generalmente: 0 = Cerca, MaxRange = Lejos.
                    isNear = distance < proximitySensor.maximumRange
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, proximitySensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // UI PRINCIPAL
    Box(modifier = Modifier.fillMaxSize()) {

        // CAPA 1: Contenido Normal (Visible cuando NO está cerca)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                    imageVector = Icons.Default.WifiTethering,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Sensor de Proximidad",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Acerca tu mano al auricular superior",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Datos técnicos para que veas qué pasa
            Text(text = "Valor actual: $sensorValue cm")
            Text(text = "Estado: ${if (isNear) "CERCA" else "LEJOS"}")
        }

        // CAPA 2: Pantalla Negra (Modo Ahorro)
        // Se superpone a todo cuando isNear es true
        if (isNear) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Opcional: Texto tenue para saber que la app sigue viva
                Text(
                    text = "Modo Ahorro Activado",
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
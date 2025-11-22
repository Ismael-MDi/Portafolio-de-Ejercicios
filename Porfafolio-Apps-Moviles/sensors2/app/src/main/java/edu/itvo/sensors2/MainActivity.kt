package edu.itvo.sensors2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
                TiltColorApp()
            }
        }
    }
}

@Composable
fun TiltColorApp() {
    val context = LocalContext.current

    // Variables de estado para la inclinación
    var xTilt by remember { mutableFloatStateOf(0f) }
    var yTilt by remember { mutableFloatStateOf(0f) }

    // Lógica para determinar el color según la inclinación
    // Usamos 'derivedStateOf' para optimizar: solo recalcula el color si cambian xTilt o yTilt
    val targetColor by remember {
        derivedStateOf {
            when {
                xTilt > 5f -> Color(0xFFFFEB3B)  // Izquierda: Amarillo
                xTilt < -5f -> Color(0xFF2196F3) // Derecha: Azul
                yTilt > 5f -> Color(0xFF4CAF50)  // Abajo: Verde
                yTilt < -5f -> Color(0xFFF44336) // Arriba: Rojo
                else -> Color.DarkGray           // Plano/Centro
            }
        }
    }

    // Animación suave de color (Bonus estético)
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = TweenSpec(durationMillis = 500),
        label = "ColorAnimation"
    )

    // Configuración del Sensor (Igual que la práctica anterior)
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    DisposableEffect(Unit) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    xTilt = it.values[0]
                    yTilt = it.values[1]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // UI Principal
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = animatedBackgroundColor // Aquí aplicamos el color dinámico
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Inclina el teléfono",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Texto descriptivo según la acción
                val statusText = when {
                    xTilt > 5f -> "Izquierda (Amarillo)"
                    xTilt < -5f -> "Derecha (Azul)"
                    yTilt > 5f -> "Abajo (Verde)"
                    yTilt < -5f -> "Arriba (Rojo)"
                    else -> "Centro (Neutro)"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "X: ${String.format("%.1f", xTilt)} | Y: ${String.format("%.1f", yTilt)}", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
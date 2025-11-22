package edu.itvo.sensors1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AccelerometerScreen()
                }
            }
        }
    }
}

@Composable
fun AccelerometerScreen() {
    val context = LocalContext.current

    // Estados para los valores X, Y, Z
    var xVal by remember { mutableFloatStateOf(0f) }
    var yVal by remember { mutableFloatStateOf(0f) }
    var zVal by remember { mutableFloatStateOf(0f) }

    // Obtener el SensorManager del sistema
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // Efecto que gestiona el ciclo de vida del sensor
    DisposableEffect(Unit) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Los valores vienen en un array float[]: [0]=x, [1]=y, [2]=z
                    xVal = it.values[0]
                    yVal = it.values[1]
                    zVal = it.values[2]
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No nos interesa para esta práctica, pero es obligatorio implementarlo
            }
        }

        // Registrar el listener (SENSOR_DELAY_UI es velocidad suficiente para mostrar en pantalla)
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        // LIMPIEZA: Esto se ejecuta cuando la pantalla se destruye
        onDispose {
            sensorManager.unregisterListener(listener)
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
        Text(
            text = "Acelerómetro",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        SensorValueCard("Eje X (Izquierda/Derecha)", xVal, Color(0xFFFFCDD2))
        Spacer(modifier = Modifier.height(16.dp))

        SensorValueCard("Eje Y (Arriba/Abajo)", yVal, Color(0xFFC8E6C9))
        Spacer(modifier = Modifier.height(16.dp))

        SensorValueCard("Eje Z (Adelante/Atrás)", zVal, Color(0xFFBBDEFB))

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Mueve el dispositivo para ver los cambios.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun SensorValueCard(label: String, value: Float, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = String.format("%.2f", value), // Formato a 2 decimales
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            // Barra visual simple
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                // La barra se llena basándose en un valor máximo teórico de 10 (gravedad aprox)
                val progress = (abs(value) / 12f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(Color.Black)
                )
            }
        }
    }
}
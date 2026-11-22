package pt.isec.amov.cityfix.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import pt.isec.amov.cityfix.AppConfig
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Deteção de impactos (enunciado I.2.F): acelerações bruscas no eixo vertical, que sugerem um buraco
 * no pavimento ou um ressalto perigoso enquanto o munícipe se desloca.
 *
 * Usa o acelerómetro: um filtro passa-baixo estima a gravidade (que indica onde é "para baixo",
 * seja qual for a posição do telemóvel); a aceleração sem a gravidade é projetada nessa direção.
 * Se a componente vertical passar o limiar (AppConfig.IMPACT_THRESHOLD), chama [onImpact],
 * no máximo uma vez a cada AppConfig.IMPACT_COOLDOWN_MS.
 */
class ImpactDetector(context: Context, private val onImpact: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Gravidade estimada (m/s²) em cada eixo
    private val gravity = FloatArray(3)
    private var samples = 0
    private var lastImpact = 0L

    val isAvailable: Boolean
        get() = accelerometer != null

    fun start() {
        accelerometer?.let {
            samples = 0
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val values = event.values
        // Filtro passa-baixo: a gravidade muda devagar, os impactos são picos curtos
        for (i in 0..2) gravity[i] = ALPHA * gravity[i] + (1 - ALPHA) * values[i]
        // Os primeiros valores servem só para a estimativa da gravidade estabilizar
        if (++samples < WARM_UP_SAMPLES) return

        val g = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
        if (g < 1f) return
        // Aceleração sem a gravidade, projetada na direção vertical
        val vertical = ((values[0] - gravity[0]) * gravity[0] +
            (values[1] - gravity[1]) * gravity[1] +
            (values[2] - gravity[2]) * gravity[2]) / g

        val now = System.currentTimeMillis()
        if (abs(vertical) >= AppConfig.IMPACT_THRESHOLD && now - lastImpact >= AppConfig.IMPACT_COOLDOWN_MS) {
            lastImpact = now
            Log.i(TAG, "Impacto vertical de %.1f m/s²".format(vertical))
            onImpact()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "ImpactDetector"
        private const val ALPHA = 0.9f
        private const val WARM_UP_SAMPLES = 50
    }
}

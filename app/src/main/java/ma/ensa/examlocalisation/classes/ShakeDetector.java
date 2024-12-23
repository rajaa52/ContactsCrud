package ma.ensa.examlocalisation.classes;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class ShakeDetector implements SensorEventListener {
    private static final float SHAKE_THRESHOLD = 10.0f; // Seuil plus élevé
    private static final int MIN_TIME_BETWEEN_SHAKES = 1500; // 1.5 secondes entre les secousses

    private OnShakeListener listener;
    private long lastShakeTime;
    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];

    public interface OnShakeListener {
        void onShake();
    }

    public void setOnShakeListener(OnShakeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Filtrer la gravité
        final float alpha = 0.8f;

        // Isoler la force de gravité avec le filtre passe-bas
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Soustraire la gravité
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        float accelerationMagnitude = (float) Math.sqrt(
                linear_acceleration[0] * linear_acceleration[0] +
                        linear_acceleration[1] * linear_acceleration[1] +
                        linear_acceleration[2] * linear_acceleration[2]
        );

        long currentTime = System.currentTimeMillis();
        if (accelerationMagnitude > SHAKE_THRESHOLD) {
            if ((currentTime - lastShakeTime) > MIN_TIME_BETWEEN_SHAKES) {
                lastShakeTime = currentTime;
                if (listener != null) {
                    listener.onShake();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Non utilisé
    }
}
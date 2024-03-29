package com.example.aaalamabra1925

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.os.Looper
import androidx.core.graphics.rotationMatrix
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import kotlin.math.PI
import kotlin.math.pow

class GestureRecognitionDialog: DialogFragment(){
    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mMagnetometer: Sensor
    private lateinit var mLocationManager: LocationManager
    private var azimuth: Float? = null
    private var latitude : Double? = null
    private var longitude: Double? = null
    private var mGravity: FloatArray? = null
    private var mGeomagnetic: FloatArray? = null
    private var me = this

    private val mLocationListener = object : LocationListener{
        override fun onLocationChanged(location: Location?) {
            if (location != null){
                latitude = location.latitude
                longitude = location.longitude
            }

            val dbManager = DbManager(context!!)
            val cursor = dbManager.queryByLocationType(0)

            val v = floatArrayOf(0F, 1F)

            // Change azimuth to degrees
            val radiansToDegrees = 180.0/PI
            val degree_azimuth = azimuth!! * radiansToDegrees.toFloat()

            // Get the direction of the line
            var rotation_m = rotationMatrix(-degree_azimuth, 0F,0F)
            rotation_m.mapVectors(v)

            // Get the normal of the line
            val n = v.copyOf()
            rotation_m = rotationMatrix(90F, 0F, 0F)
            rotation_m.mapVectors(n)

            Log.d("Azimuth", "$azimuth, $degree_azimuth")
            Log.d("Vector director", "${v[0]}, ${v[1]}")
            Log.d("Vector normal", "${n[0]}, ${n[1]}")
            Log.d("Localización", "${location!!.longitude}, ${location.latitude}")

            var lowest_dist = Float.MAX_VALUE
            var nearest_ip  = 1

            // Implements dot product
            val dotprod: (FloatArray, FloatArray) -> Float = {x, y -> x[0]*y[0] + x[1]*y[1]}
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndex("Id"))
                    val ip_lat = cursor.getFloat(cursor.getColumnIndex("Latitude"))
                    val ip_long = cursor.getFloat(cursor.getColumnIndex("Longitude"))

                    // Dist to the line
                    val diff = floatArrayOf(ip_long - longitude!!.toFloat(), ip_lat - latitude!!.toFloat())
                    val square_dist = dotprod(diff, n).pow(2)

                    val isBehind = (dotprod(v, diff) <= 0F)

                    Log.d("Punto evaluando", "$ip_long, $ip_lat")
                    Log.d("Distancia", "$square_dist")
                    Log.d("Está detrás", "$isBehind")

                    if (lowest_dist > square_dist && !isBehind){
                        nearest_ip = id
                        lowest_dist = square_dist
                    }
                } while (cursor.moveToNext())

                val args = bundleOf("id" to nearest_ip)
                findNavController().navigate(R.id.action_nav_home_to_nav_ip, args)
                me.dismiss()
            }
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    // Listener for the accelerometer
    private val mPositionListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values
            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values
            if (mGravity != null && mGeomagnetic != null) {
                val R = FloatArray(9)
                val I = FloatArray(9)
                val success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)
                if (success) {
                    val orientation = FloatArray(3)
                    // orientation contains: azimut(0), pitch(1) and roll(2)
                    SensorManager.getOrientation(R, orientation)

                    if (orientation[1] > 0){
                        mSensorManager.unregisterListener(this)
                        azimuth = orientation[0]
                        Toast.makeText(activity, "Buscando punto de interés", Toast.LENGTH_LONG).show()

                        if (ContextCompat.checkSelfPermission(activity as Context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, Looper.getMainLooper())
                        }

                    }

                }
            }

        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Get sensor manager
            mSensorManager = it.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            // Get the sensors necessary for position
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            // Get location manager
            mLocationManager = it.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage("Apunta al punto de interés que quieras conocer")
                .setNegativeButton("Cancelar"
                ) { _, _->
                    // User cancelled the dialog
                }

            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onStart() {
        super.onStart()
        mSensorManager.registerListener(mPositionListener, mAccelerometer,
            SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(mPositionListener, mMagnetometer,
            SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStop() {
        super.onStop()
        mSensorManager.unregisterListener(mPositionListener)
        mGravity = null
        mGeomagnetic = null
    }

}

package com.example.aaalamabra1925.ui.home

import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.*
import android.location.LocationListener
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.aaalamabra1925.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.compass.CompassOverlay
import android.widget.Toast
import com.example.aaalamabra1925.DbManager
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.aaalamabra1925.GestureRecognitionDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

private var UMBRAL : Float = 12.0F
private val PUERTA_CAFETERIA_DCH = GeoPoint(37.197103, -3.624330)
private val PUERTA_CAFETERIA_IZQ = GeoPoint(37.196880, -3.624733)
private val PUERTA_AULARIO_DCH = GeoPoint(37.197418, -3.623827)
private val PUERTA_AULARIO_IZQ = GeoPoint(37.197260, -3.624796)
private val PUERTA_JUSTICIA_IZQ = GeoPoint(37.176061, -3.590453)
private val PUERTA_JUSTICIA_DCH = GeoPoint(37.176215, -3.590194)
private val PUERTA_CARLOS_V_DCH = GeoPoint(37.177066, -3.589408)
private val PUERTA_CARLOS_V_IZQ = GeoPoint(37.176434, -3.590323)

class HomeFragment : Fragment() {

    private var mLocationOverlay: MyLocationNewOverlay? = null
    private var mCompassOverlay: CompassOverlay? = null
    private var mScaleBarOverlay: ScaleBarOverlay? = null
    private var mRotationGestureOverlay: RotationGestureOverlay? = null
    private var mLocationManager: LocationManager? = null
    private lateinit var mapView: MapView

    private var gestureRecognitionDialog = GestureRecognitionDialog()

    private val mLocationListener = object : LocationListener{
        override fun onLocationChanged(location: Location?) {
            if (location != null){
                val senialgps = location.accuracy
                Toast.makeText(context,  senialgps.toString() , Toast.LENGTH_LONG).show()

                if(senialgps >= UMBRAL){
                    val id = nearDoor(location)
                    Log.d("Home Fragment", "Id = $id" )
                    //Toast.makeText(context,  id.toString() , Toast.LENGTH_LONG).show()
                    if(id != 0){
                        mLocationManager!!.removeUpdates(this)
                        val bundle = bundleOf("id" to id)
                        findNavController().navigate(R.id.action_nav_home_to_nav_inner_map, bundle)
                    }

                }
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}

        fun nearDoor(location: Location) : Int{

            if(PUERTA_JUSTICIA_IZQ.latitude <= location.latitude && location.latitude <= PUERTA_JUSTICIA_DCH.latitude
                && PUERTA_JUSTICIA_IZQ.longitude <= location.longitude && location.longitude <= PUERTA_JUSTICIA_DCH.longitude)
                return 1

            if(PUERTA_CARLOS_V_IZQ.latitude <= location.latitude && location.latitude <= PUERTA_CARLOS_V_DCH.latitude
                && PUERTA_CARLOS_V_IZQ.longitude <= location.longitude && location.longitude <= PUERTA_CARLOS_V_DCH.longitude)
                return 2

            if(PUERTA_AULARIO_IZQ.latitude < location.latitude && location.latitude < PUERTA_AULARIO_DCH.latitude
                && PUERTA_AULARIO_IZQ.longitude < location.longitude && location.longitude < PUERTA_AULARIO_DCH.longitude)
                return 2

            if(PUERTA_CAFETERIA_IZQ.latitude <= location.latitude && location.latitude <= PUERTA_CAFETERIA_DCH.latitude
                && PUERTA_CAFETERIA_IZQ.longitude <= location.longitude && location.longitude <= PUERTA_CAFETERIA_DCH.longitude)
                return 1

            return 0
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        // Add floating button to detection gesture.
        val fab: FloatingActionButton = root.findViewById(R.id.detection)
        fab.setOnClickListener {
            gestureRecognitionDialog.show(activity!!.supportFragmentManager, "gestures")
        }

        val test: FloatingActionButton = root.findViewById(R.id.test)
        test.setOnClickListener {
            UMBRAL = if (UMBRAL == 12F)  1000F else  12F
            Toast.makeText(context!!, "Umbral a $UMBRAL", Toast.LENGTH_SHORT).show()
        }
        // Get map view
        mapView = root.findViewById(R.id.openmapview)
        Configuration.getInstance().userAgentValue = context!!.packageName
        val dm = this.context!!.resources.displayMetrics

        // Add compass
        mCompassOverlay = CompassOverlay(
                context, InternalCompassOrientationProvider(context),
                mapView
        )
        // Add user location
        mLocationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(context),
                mapView
        )

        // Set different parameters to the map.
        val myMapController = mapView.controller
        myMapController.setZoom(18.0)
        mScaleBarOverlay = ScaleBarOverlay(mapView)
        mScaleBarOverlay!!.setCentred(true)
        mScaleBarOverlay!!.setScaleBarOffset(dm.widthPixels / 2, 10)
        mRotationGestureOverlay = RotationGestureOverlay(mapView)
        mRotationGestureOverlay!!.isEnabled = true
        mapView.isTilesScaledToDpi = true
        mapView.setMultiTouchControls(true)
        mapView.isFlingEnabled = true
        mapView.overlays.add(mLocationOverlay)
        mapView.overlays.add(mCompassOverlay)
        mapView.overlays.add(mScaleBarOverlay)
        mLocationOverlay!!.enableMyLocation()
        mLocationOverlay!!.enableFollowLocation()
        mLocationOverlay!!.isOptionsMenuEnabled = true
        mCompassOverlay!!.enableCompass()

        // Initialize database manager
        val dbManager = DbManager(context!!)

        // Get all Interest Points that are outside and add them to the map.
        val cursor = dbManager.queryByLocationType(0)
        if (cursor.moveToFirst()) {
            do {
                addMarker(
                    cursor.getDouble(cursor.getColumnIndex("Latitude")),
                    cursor.getDouble(cursor.getColumnIndex("Longitude")),
                    cursor.getInt(cursor.getColumnIndex("Id"))
                )
            } while (cursor.moveToNext())
        }

        super.onCreate(savedInstanceState)

        // Launch permission request dialog.
        mLocationManager = context!!.getSystemService(LOCATION_SERVICE) as LocationManager?
        if (ContextCompat.checkSelfPermission(activity as Context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000, 1.0F, mLocationListener, null)
        }


        return root
    }

    // This function creates a mark in the given location and overrides its click listener so it calls the corresponding interest point.
    private fun addMarker(lat: Double, long: Double, id: Int){
        val mark = Marker(mapView)
        mark.position = GeoPoint(lat, long)
        mark.textLabelFontSize = 40
        mark.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
        mark.infoWindow = null
        mapView.overlays.add(mark)

        mark.setOnMarkerClickListener { _, _ ->
            val bundle = bundleOf("id" to id)
            findNavController().navigate(R.id.action_nav_home_to_nav_ip, bundle)
            true
        }

    }

    override fun onStop() {
        super.onStop()
        // Unregister listener on stop.
        mLocationManager!!.removeUpdates(mLocationListener)
    }
}

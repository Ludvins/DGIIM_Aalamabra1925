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
import androidx.lifecycle.ViewModelProviders
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
import com.example.aaalamabra1925.R.id.action_nav_home_to_nav_insidemap
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.lang.Math.sqrt
import kotlin.math.pow


class HomeFragment() : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var mLocationOverlay: MyLocationNewOverlay? = null
    private var mCompassOverlay: CompassOverlay? = null
    private var mScaleBarOverlay: ScaleBarOverlay? = null
    private var mRotationGestureOverlay: RotationGestureOverlay? = null
    private var mLocationManager: LocationManager? = null
    private lateinit var mapView: MapView

    private val PUERTA_CAFETERIA_1 = GeoPoint(37.19701, -3.6243)
    private val PUERTA_CAFETERIA_2 = GeoPoint(37.197152, -3.6247)

    private val PUERTA_AULARIO_1 = GeoPoint(37.19725, -3.624225)
    private val PUERTA_AULARIO_2 = GeoPoint(37.1973, -3.6247)

    private var change = true

    private val mLocationListener = object : LocationListener{
        override fun onLocationChanged(location: Location?) {
            Log.d("Home fragment", "Change" + change.toString())
            if (location != null){
                // TODO esto peta de vez en cuandoo
                val senialgps = location.accuracy
                Toast.makeText(context,  senialgps.toString() , Toast.LENGTH_LONG).show()
                Log.d("Home fragment", "Location:" + senialgps.toString())

                if(senialgps >= 22.00F && change){
                    change = false
                    val id = nearDoor(location)
                    val bundle = bundleOf("id" to id)
                    findNavController().navigate(action_nav_home_to_nav_insidemap, bundle)
                }
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}

        fun nearDoor(location: Location) : Int{
            var dis1 = (location.longitude - PUERTA_CAFETERIA_1.longitude).pow(2) + (location.altitude - PUERTA_CAFETERIA_1.altitude).pow(2)
            var dis2 = (location.longitude - PUERTA_CAFETERIA_2.longitude).pow(2) + (location.altitude - PUERTA_CAFETERIA_2.altitude).pow(2)
            val distanciaCaf = minOf(dis1, dis2)

            dis1 = (location.longitude - PUERTA_AULARIO_1.longitude).pow(2) + (location.altitude - PUERTA_AULARIO_1.altitude).pow(2)
            dis2 = (location.longitude - PUERTA_AULARIO_2.longitude).pow(2) + (location.altitude - PUERTA_AULARIO_2.altitude).pow(2)
            val distanciaAul = minOf(dis1, dis2)

            if(distanciaAul < distanciaCaf){
                return 1
            }else{
                return 2
            }
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_home, container, false)
        mapView = root.findViewById(R.id.openmapview)
        Configuration.getInstance().userAgentValue = context!!.packageName
        val dm = this.context!!.resources.displayMetrics

        mCompassOverlay = CompassOverlay(
                context, InternalCompassOrientationProvider(context),
                mapView
        )
        mLocationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(context),
                mapView
        )

        val myMapController = mapView.controller
        myMapController.setZoom(18.0)
        //myMapController.setCenter(GeoPoint(37.1970, -3.624))

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


        val dbManager = DbManager(context!!)

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


        val test = Marker(mapView)
        test.position = GeoPoint(37.197152, -3.624137)
        test.textLabelFontSize = 40
        //test.setTextIcon("Etiqueta de prueba")
        test.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
        //test.icon = R.drawable.current_position_tennis_ball
        test.infoWindow = null
        mapView.overlays.add(test)

        test.setOnMarkerClickListener { _, _ ->
            Toast.makeText(this.activity, "Marker's Listener invoked", Toast.LENGTH_LONG).show()
            true
        }

        super.onCreate(savedInstanceState)

        change = true

        mLocationManager = context!!.getSystemService(LOCATION_SERVICE) as LocationManager?

        if (ContextCompat.checkSelfPermission(activity as Context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000, 1.0F, mLocationListener, null)
        }

        val test2 = Marker(mapView)
        test2.position = GeoPoint(37.1973, -3.6247)
        test2.textLabelFontSize = 40
        //test.setTextIcon("Etiqueta de prueba")
        test2.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
        //test.icon = R.drawable.current_position_tennis_ball
        test2.infoWindow = null
        mapView.overlays.add(test2)

        return root
        }
    private fun addMarker(lat: Double, long: Double, id: Int){
        val test = Marker(mapView)
        test.position = GeoPoint(lat, long)
        test.textLabelFontSize = 40
        //test.setTextIcon(id.toString())
        test.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
        //test.icon = resources.getDrawable(R.drawable.ic_menu_compass)
        test.infoWindow = null
        mapView.overlays.add(test)

        test.setOnMarkerClickListener { _, _ ->
            val bundle = bundleOf("id" to id)
            findNavController().navigate(R.id.action_nav_home_to_nav_ip, bundle)
            true
        }
    }
    }





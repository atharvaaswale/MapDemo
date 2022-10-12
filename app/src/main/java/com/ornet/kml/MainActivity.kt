package com.ornet.kml

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SeekBar
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.ornet.kml.databinding.ActivityMainBinding
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import android.widget.SeekBar.OnSeekBarChangeListener


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    var currentLocation: Location? = null
    var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private val REQUEST_CODE = 101
    var latLngList: ArrayList<LatLng> = ArrayList()
    var markerList: ArrayList<Bitmap> = ArrayList()
    var mMap: GoogleMap? = null
    lateinit var binding: ActivityMainBinding
    private val ALPHA_ADJUSTMENT = 0x77000000
    var latLng: LatLng? = null
    lateinit var mTile: TileOverlay


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar!!.hide()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocation()

    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                this.REQUEST_CODE)
            return
        }
        val task = fusedLocationProviderClient!!.lastLocation
        task.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                mapFragment?.getMapAsync(this)

                (supportFragmentManager.findFragmentById(R.id.map) as WorkaroundMapFragment?)!!.setListener {
                    binding.scr.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap!!.uiSettings.isZoomControlsEnabled = true
        mMap!!.uiSettings.isScrollGesturesEnabled = true
        mMap!!.uiSettings.isZoomGesturesEnabled = true
        mMap!!.uiSettings.isMyLocationButtonEnabled = true


        val tileProvider: TileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {

                /* Define the URL pattern for the tile images */
                val url = "http://103.14.99.175/OVERLAY/AWADHAN.htm"
//                val url = "https://mw1.google.com/mw-planetary/lunar/lunarmaps_v1/clem_bw/%d/%d/%d.jpg"
//                val url = "https://mw1.google.com/mw-planetary/lunar/lunarmaps_v1/clem_bw/1200/1000/15.jpg"
                return if (!checkTileExists(x, y, zoom)) {
                    null
                } else try {
                    URL(url)
                } catch (e: Exception) {
                    throw AssertionError(e)
                }
            }
            /**
             * Check that the tile server supports the requested x, y and zoom.
             * Complete this stub according to the tile range you support.
             * If you support a limited range of tiles at different zoom levels, then you
             * need to define the supported x, y range at each zoom level.
             */
            private fun checkTileExists(x: Int, y: Int, zoom: Int): Boolean {
                val minZoom = 15
                val maxZoom = 22
                return zoom in minZoom..maxZoom
            }
        }
        mTile = mMap!!.addTileOverlay(
            TileOverlayOptions().tileProvider(tileProvider)
        )!!
        mTile.transparency = 0.5f

        googleMap.setOnCameraChangeListener { cameraPosition ->
            latLng = cameraPosition.target
            try {
                getAddress(latLng!!.latitude, latLng!!.longitude)
                Log.e("Changing address", getAddress(latLng!!.latitude, latLng!!.longitude))
                Log.e("Latitude", latLng!!.latitude.toString() + "")
                Log.e("Longitude", latLng!!.longitude.toString() + "")
                val lat = latLng!!.latitude.toString() + ""
                val lng = latLng!!.longitude.toString() + ""
                val location = getAddress(latLng!!.latitude, latLng!!.longitude)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { return }
        mMap!!.isMyLocationEnabled = true

        binding.btnDraw.setOnClickListener {
            val mp: MediaPlayer = MediaPlayer.create(this, R.raw.btn_draw_sound)
            latLngList.add(latLng!!)
            createCustomMarker(this, R.drawable.ic_dot)?.let { it1 -> markerList.add(it1) }
            for (latlng in latLngList) {
                mp.start()
                mMap!!.addMarker(
                    MarkerOptions().position(LatLng(latlng.latitude, latlng.longitude)).icon(
                        createCustomMarker(this, R.drawable.ic_dot)?.let { it1 ->
                            BitmapDescriptorFactory.fromBitmap(it1)
                        })
                )!!
            }
        }
        binding.btnclear.setOnClickListener {
            val mp: MediaPlayer = MediaPlayer.create(this, R.raw.btn_clear_sound)
            mp.start()
            latLngList.clear()
            markerList.clear()
            mMap!!.clear()
        }

//        val latLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
        val latLng = LatLng(20.853678,74.767456)
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            this.REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation()
            }
        }
    }


    fun getAddress(latitude: Double, longitude: Double): String {
        val result = StringBuilder()
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses.size > 0) {
                val address = addresses[0]
                for (i in 0..addresses[0].maxAddressLineIndex) {
                    if (i == addresses[0].maxAddressLineIndex) {
                        result.append(addresses[0].getAddressLine(i))
                    } else {
                        result.append(addresses[0].getAddressLine(i) + ",")
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("tag", e.message!!)
        }
        return result.toString()
    }


    fun createCustomMarker(context: Context, @DrawableRes resource: Int): Bitmap? {
        val marker: View =
            (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                R.layout.custom_marker_layout, null)
        val markerImage: ImageView = marker.findViewById<View>(R.id.user_dp) as ImageView
        markerImage.setImageResource(resource)
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        marker.layoutParams = ViewGroup.LayoutParams(5, ViewGroup.LayoutParams.WRAP_CONTENT)
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        marker.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(
            marker.measuredWidth,
            marker.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        marker.draw(canvas)
        return bitmap
    }

}
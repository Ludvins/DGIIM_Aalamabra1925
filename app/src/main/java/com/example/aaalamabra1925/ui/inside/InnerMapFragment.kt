package com.example.aaalamabra1925.ui.inside

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.content.ContextCompat
import android.content.Context
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.aaalamabra1925.DbManager
import com.example.aaalamabra1925.R
import android.widget.RelativeLayout
import android.R.drawable
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi


class InnerMapFragment : Fragment() {
    private lateinit var layout : RelativeLayout
    private val MODOALHAMBRA = true

    private fun dpToPx(context: Context, dp: Int): Int {
        // Reference http://stackoverflow.com/questions/8309354/formula-px-to-dp-dp-to-px-android
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun addFloatingButton(root: View, id : Int, long:Int, lat:Int){

        val fab = FloatingActionButton(context!!)

        val rel = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rel.setMargins(long, lat, long, lat)

        fab.layoutParams = rel
        fab.setImageResource(drawable.ic_dialog_info)
        fab.size = FloatingActionButton.SIZE_NORMAL
        fab.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.transparent))
        fab.elevation = 0f
        fab.compatElevation = 0f

        fab.setOnClickListener {
            val bundle = bundleOf("id" to id)
            findNavController().navigate(R.id.action_nav_inner_map_to_nav_ip, bundle)
        }

        val linearLayout = root.findViewById<RelativeLayout>(R.id.layout)
        linearLayout?.addView(fab)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_inside, container, false)
        layout = root.findViewById(R.id.layout) as RelativeLayout
        super.onCreate(savedInstanceState)

        val mid = arguments!!.get("id")
        val dbManager = DbManager(context!!)

        val cursor = dbManager.queryByLocationType(mid as Int)
        if (cursor.moveToFirst()){
            do{
                val lat = cursor.getDouble(cursor.getColumnIndex("Latitude"))
                val long = cursor.getDouble(cursor.getColumnIndex("Longitude"))
                val id = cursor.getInt(cursor.getColumnIndex("Id"))

                addFloatingButton(root ,id, lat.toInt(), long.toInt())

            }while(cursor.moveToNext())
        }

        if(MODOALHAMBRA){
            if(mid == 1){
                root.setBackgroundDrawable(ContextCompat.getDrawable(this.context!!, R.drawable.puertadelajusticia))
            }else{
                root.setBackgroundDrawable(ContextCompat.getDrawable(this.context!!, R.drawable.palaciocv))
            }
        }else{
            if(mid == 1){
                root.setBackgroundDrawable(ContextCompat.getDrawable(this.context!!, R.drawable.mapapb))
            }else{
                root.setBackgroundDrawable(ContextCompat.getDrawable(this.context!!, R.drawable.mapacafeteria))
            }
        }



        /*val button = root.findViewById<FloatingActionButton>
        val params = button.layoutParams as MarginLayoutParams
        params.setMargins(activity?.let { dpToPx(it, 8) }!!, activity?.let { dpToPx(it, 10) }!!, 0, 0)
        button.layoutParams = params

        button.setOnClickListener(View.OnClickListener {
            Toast.makeText(this.activity,  "Marker's Listener invoked" , Toast.LENGTH_LONG).show()
            true
        })*/

        return root
    }
}
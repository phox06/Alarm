package com.example.alarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Random
import java.util.TimeZone

class WorldClockFragment : Fragment() {

    private lateinit var rvWorldClock: RecyclerView
    private lateinit var adapter: WorldClockAdapter
    private lateinit var tvCurrentTimeZone: TextView
    private val selectedCities = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_world_clock, container, false)

        rvWorldClock = view.findViewById(R.id.rvWorldClock)
        tvCurrentTimeZone = view.findViewById(R.id.tvCurrentTimeZone)
        val fabAddCity: FloatingActionButton = view.findViewById(R.id.fabAddCity)

        // Set current timezone info
        val currentTz = TimeZone.getDefault()
        tvCurrentTimeZone.text = currentTz.displayName

        // Default cities
        if (selectedCities.isEmpty()) {
            selectedCities.add("GMT")
            selectedCities.add("America/New_York")
            selectedCities.add("Europe/London")
            selectedCities.add("Asia/Tokyo")
        }

        adapter = WorldClockAdapter(selectedCities)
        rvWorldClock.layoutManager = LinearLayoutManager(context)
        rvWorldClock.adapter = adapter

        fabAddCity.setOnClickListener {
            val allIds = TimeZone.getAvailableIDs()
            val randomId = allIds[Random().nextInt(allIds.size)]
            selectedCities.add(randomId)
            adapter.notifyItemInserted(selectedCities.size - 1)
        }

        return view
    }
}

class WorldClockAdapter(private val cities: List<String>) :
    RecyclerView.Adapter<WorldClockAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCityName: TextView = view.findViewById(R.id.tvCityName)
        val tvTimeZone: TextView = view.findViewById(R.id.tvTimeZone)
        val tcWorldTime: android.widget.TextClock = view.findViewById(R.id.tcWorldTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_world_clock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tzId = cities[position]
        val tz = TimeZone.getTimeZone(tzId)

        holder.tvCityName.text = tzId.substringAfter('/')
        holder.tvTimeZone.text = tz.displayName
        holder.tcWorldTime.timeZone = tzId
    }

    override fun getItemCount(): Int = cities.size
}

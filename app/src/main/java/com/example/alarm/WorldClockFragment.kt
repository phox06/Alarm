package com.example.alarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.TimeZone

class WorldClockFragment : Fragment() {

    private lateinit var rvWorldClock: RecyclerView
    private lateinit var adapter: WorldClockAdapter
    private val selectedCities = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_world_clock, container, false)

        rvWorldClock = view.findViewById(R.id.rvWorldClock)
        val fabAddCity: FloatingActionButton = view.findViewById(R.id.fabAddCity)

        // Default cities
        if (selectedCities.isEmpty()) {
            selectedCities.add("GMT")
            selectedCities.add("America/New_York")
            selectedCities.add("Europe/London")
            selectedCities.add("Asia/Tokyo")
        }

        // --- UPDATED: Pass a lambda function to handle the long click ---
        adapter = WorldClockAdapter(selectedCities) { position ->
            showDeleteConfirmation(position)
        }

        rvWorldClock.layoutManager = LinearLayoutManager(context)
        rvWorldClock.adapter = adapter

        // Listen for data coming back from the Globe Fragment
        parentFragmentManager.setFragmentResultListener("timezoneRequest", viewLifecycleOwner) { _, bundle ->
            val resultTzId = bundle.getString("timeZoneId")
            if (resultTzId != null && !selectedCities.contains(resultTzId)) {
                selectedCities.add(resultTzId)
                adapter.notifyItemInserted(selectedCities.size - 1)
            }
        }

        fabAddCity.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(android.R.id.content, GlobeSelectionFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    // --- NEW: Method to show a delete confirmation popup ---
    private fun showDeleteConfirmation(position: Int) {
        val tzId = selectedCities[position]
        val cityName = tzId.substringAfter('/') // Extracts just the city name

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove City")
            .setMessage("Are you sure you want to remove $cityName from your world clock?")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Remove") { dialog, _ ->
                // Remove from the data list
                selectedCities.removeAt(position)
                // Tell the adapter the item is gone so it animates away
                adapter.notifyItemRemoved(position)
                dialog.dismiss()
            }
            .show()
    }
}

// --- UPDATED ADAPTER: Added the onCityLongClick parameter ---
class WorldClockAdapter(
    private val cities: List<String>,
    private val onCityLongClick: (Int) -> Unit
) : RecyclerView.Adapter<WorldClockAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCityName: android.widget.TextView = view.findViewById(R.id.tvCityName)
        val tvTimeZone: android.widget.TextView = view.findViewById(R.id.tvTimeZone)
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

        // --- NEW: Listen for the long press and send the position back to the fragment ---
        holder.itemView.setOnLongClickListener {
            // holder.adapterPosition is safer than 'position' in case the list order changes
            onCityLongClick(holder.adapterPosition)
            true // Returning true tells Android we completely consumed this long-click event
        }
    }

    override fun getItemCount(): Int = cities.size
}
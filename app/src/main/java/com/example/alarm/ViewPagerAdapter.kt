package com.example.alarm
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AlarmFragment()
            1 -> WorldClockFragment()
            2 -> StopwatchFragment()
            3 -> CountdownFragment()
            else -> AlarmFragment()
        }
    }
}
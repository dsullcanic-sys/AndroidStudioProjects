package com.ryuk.horario.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ryuk.horario.fragments.HorarioFragment
import com.ryuk.horario.fragments.MateriasFragment
import com.ryuk.horario.HorarioViewModel

class ViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val viewModel: HorarioViewModel
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MateriasFragment(viewModel)
            1 -> HorarioFragment(viewModel)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
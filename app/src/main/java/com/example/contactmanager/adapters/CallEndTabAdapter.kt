package com.example.contactmanager.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.contactmanager.fragments.callShow.CallShowFragment
import com.example.contactmanager.fragments.newRemind.NewRemindFragment
import com.example.contactmanager.fragments.moreFeatures.MoreFeaturesFragment
import com.example.contactmanager.fragments.newMessage.NewMessageFragment

class CallEndTabAdapter(
    fm: FragmentManager,
    private val totalTabs: Int
) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int = totalTabs

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> CallShowFragment()
            1 -> NewMessageFragment()
            2 -> NewRemindFragment()
            3 -> MoreFeaturesFragment()
            else -> CallShowFragment()
        }
    }
}

package com.example.linguify.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.linguify.R

class OnboardingPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_title_1,
            descriptionRes = R.string.onboarding_description_1,
            imageRes = R.raw.onboarding_1
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_description_2,
            imageRes = R.raw.onboarding_2
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_description_3,
            imageRes = R.raw.onboarding_3
        )
    )

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        val page = pages[position]
        return OnboardingPageFragment.newInstance(
            page.titleRes,
            page.descriptionRes,
            page.imageRes
        )
    }
}

data class OnboardingPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val imageRes: Int
)

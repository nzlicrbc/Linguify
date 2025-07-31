package com.example.linguify.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.linguify.R
import com.example.linguify.databinding.FragmentOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by viewModels()
    private lateinit var onboardingPagerAdapter: OnboardingPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        onboardingPagerAdapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = onboardingPagerAdapter

        binding.dotsIndicator.attachTo(binding.viewPager)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonsVisibility(position)
            }
        })
    }

    private fun updateButtonsVisibility(position: Int) {
        val isLastPage = position == onboardingPagerAdapter.itemCount - 1

        binding.apply {
            btnSkip.visibility = if (isLastPage) View.GONE else View.VISIBLE
            btnNext.apply {
                if (isLastPage) {
                    text = getString(R.string.get_started)
                    setOnClickListener { completeOnboarding() }
                } else {
                    text = getString(R.string.next)
                    setOnClickListener {
                        viewPager.currentItem = viewPager.currentItem + 1
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        viewModel.setOnboardingCompleted()
        navigateToLevelTest()
    }

    private fun navigateToLevelTest() {
        findNavController().navigate(R.id.action_onboardingFragment_to_levelTestFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
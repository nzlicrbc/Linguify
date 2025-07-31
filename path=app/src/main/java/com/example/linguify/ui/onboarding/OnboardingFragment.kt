class OnboardingFragment : Fragment() {
    // ... existing code ...

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

    // btnGetStarted'ı kaldırdığımız için bu fonksiyonu güncelliyoruz
    private fun setupButtons() {
        binding.btnSkip.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun setupViewPager() {
        onboardingPagerAdapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = onboardingPagerAdapter

        // DotsIndicator'ı ViewPager2 ile bağla
        binding.dotsIndicator.attachTo(binding.viewPager)

        // Setup page change callback to update button visibility
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonsVisibility(position)
            }
        })
    }

    // ... existing code ...
} 
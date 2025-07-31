class OnboardingPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_title_1,
            descriptionRes = R.string.onboarding_description_1,
            // Lottie animasyon dosyalarını raw klasöründe tutacağız
            imageRes = R.raw.onboarding_animation_1 // İlk animasyon
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_description_2,
            imageRes = R.raw.onboarding_animation_2 // İkinci animasyon
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_description_3,
            imageRes = R.raw.onboarding_animation_3 // Üçüncü animasyon
        )
    )

    // ... existing code ...
} 
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.textViewTitle.setText(titleRes)
    binding.textViewDescription.setText(descriptionRes)
    // ImageView yerine LottieAnimationView kullanacağız
    binding.lottieAnimation.setAnimation(imageRes)
    binding.lottieAnimation.playAnimation()
} 
class HomeFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        setupObservers()
        viewModel.loadWordCounts()
    }

    private fun setupObservers() {
        // ... existing observers ...

        // Rastgele kelime resmi için observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.randomWordImage.collectLatest { imageUrl ->
                if (imageUrl.isNotEmpty()) {
                    // Glide veya Coil ile resmi yükle
                    binding.ivDiscoverPreview.load(imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.placeholder_image)
                        error(R.drawable.error_image)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // ... existing click listeners ...

        // Discover section'a tıklandığında resmi yenile
        binding.cardDiscover.setOnClickListener {
            viewModel.loadWordCounts() // Bu fonksiyon yeni bir rastgele resim yükleyecek
        }

        binding.btnDiscover.setOnClickListener {
            val bundle = bundleOf("load_to_learn" to false, "mode" to "discover")
            findNavController().navigate(R.id.action_homeFragment_to_flashcardFragment, bundle)
        }
    }
} 
package com.example.linguify.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.linguify.databinding.FragmentOnboardingPageBinding

class OnboardingPageFragment : Fragment() {

    private var _binding: FragmentOnboardingPageBinding? = null
    private val binding get() = _binding!!

    private var titleRes: Int = 0
    private var descriptionRes: Int = 0
    private var imageRes: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            titleRes = it.getInt(ARG_TITLE_RES)
            descriptionRes = it.getInt(ARG_DESCRIPTION_RES)
            imageRes = it.getInt(ARG_IMAGE_RES)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textViewTitle.setText(titleRes)
        binding.textViewDescription.setText(descriptionRes)
        binding.lottieAnimation.setAnimation(imageRes)
        binding.lottieAnimation.playAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE_RES = "title_res"
        private const val ARG_DESCRIPTION_RES = "description_res"
        private const val ARG_IMAGE_RES = "image_res"

        fun newInstance(titleRes: Int, descriptionRes: Int, imageRes: Int) =
            OnboardingPageFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TITLE_RES, titleRes)
                    putInt(ARG_DESCRIPTION_RES, descriptionRes)
                    putInt(ARG_IMAGE_RES, imageRes)
                }
            }
    }
}
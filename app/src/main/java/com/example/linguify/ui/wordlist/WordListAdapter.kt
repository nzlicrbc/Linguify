package com.example.linguify.ui.wordlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.linguify.R
import com.example.linguify.databinding.ItemWordBinding
import com.example.linguify.model.Word
import com.example.linguify.model.WordLearningStatus
import com.example.linguify.utils.loadWithCache

class WordListAdapter(
    private val onWordClick: (Word) -> Unit,
    private val imageLoader: (String, (String?) -> Unit) -> Unit
) : ListAdapter<Word, WordListAdapter.WordViewHolder>(WordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WordViewHolder(
        private val binding: ItemWordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onWordClick(getItem(position))
                }
            }
        }


        fun bind(word: Word) {
            binding.ivWordImage.setImageResource(R.drawable.placeholder_image)

            binding.tvWord.text = word.text

            if (!word.wordType.isNullOrEmpty()) {
                binding.tvWordType.visibility = View.VISIBLE
                binding.tvWordType.text = word.wordType
            } else {
                binding.tvWordType.visibility = View.GONE
            }

            binding.tvTranslation.text = word.translation

            val statusResource = when (word.status) {
                WordLearningStatus.KNOWN -> {
                    binding.tvStatus.setText(R.string.status_known)
                    R.drawable.bg_status_known
                }
                WordLearningStatus.TO_LEARN -> {
                    binding.tvStatus.setText(R.string.status_to_learn)
                    R.drawable.bg_status_to_learn
                }
                WordLearningStatus.LEARNING -> {
                    binding.tvStatus.setText(R.string.status_learning)
                    R.drawable.bg_status_learning
                }
                else -> {
                    binding.tvStatus.setText(R.string.status_new)
                    R.drawable.bg_status_new
                }
            }
            binding.tvStatus.setBackgroundResource(statusResource)

            val cefrLevel = when (word.level) {
                "beginner" -> "A1-A2"
                "intermediate" -> "B1-B2"
                "advanced" -> "C1-C2"
                else -> ""
            }
            binding.tvLevel.text = cefrLevel

            if (word.imageUrl != null && word.imageUrl.isNotEmpty()) {
                binding.ivWordImage.loadWithCache(word.imageUrl)
            } else {
                if (binding.ivWordImage.visibility == View.VISIBLE) {
                    imageLoader(word.text) { imageUrl ->
                        if (bindingAdapterPosition != RecyclerView.NO_POSITION &&
                            getItem(bindingAdapterPosition).id == word.id) {
                            imageUrl?.let {
                                binding.ivWordImage.loadWithCache(it)
                            }
                        }
                    }
                }
            }
        }

        private fun loadImage(imageUrl: String) {
            Glide.with(binding.root.context)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.placeholder_image)
                .into(binding.ivWordImage)
        }
    }

    private class WordDiffCallback : DiffUtil.ItemCallback<Word>() {
        override fun areItemsTheSame(oldItem: Word, newItem: Word): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Word, newItem: Word): Boolean {
            return oldItem == newItem
        }
    }
}

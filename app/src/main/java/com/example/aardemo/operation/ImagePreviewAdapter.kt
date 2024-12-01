package com.example.aardemo.operation

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.aardemo.R
import com.example.aardemo.databinding.ItemImagePreviewBinding
import android.util.Log


class ImagePreviewAdapter(
    private val onImageRemoved: (Int) -> Unit,
    private val onImageClick: (Int) -> Unit,
    private val onImageReordered: (Int, Int) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {

    private val images = mutableListOf<Uri>()

    inner class ViewHolder(private val binding: ItemImagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set the width of the item based on the height
            itemView.post {
                val height = itemView.height
                val params = itemView.layoutParams
                params.width = height // Make width equal to height for square aspect ratio
                itemView.layoutParams = params
            }
        }

        fun bind(uri: Uri, position: Int) {
            try {
                // Clear any existing image first
                binding.previewImage.setImageDrawable(null)

                // Load new image
                Glide.with(binding.root.context)
                    .load(uri)
                    .override(512, 512)  // Good resolution for previews
                    .centerCrop()
                    .error(R.drawable.ic_error_image)
                    .into(binding.previewImage)

                // Set click listeners
                binding.removeButton.setOnClickListener {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        // Remove from adapter first
                        val currentPosition = adapterPosition
                        images.removeAt(currentPosition)
                        notifyItemRemoved(currentPosition)
                        // Then notify the activity
                        onImageRemoved(currentPosition)
                    }
                }

                binding.root.setOnClickListener {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onImageClick(position)
                    }
                }

            } catch (e: Exception) {
                Log.e("ImagePreviewAdapter", "Error binding image: ${e.message}")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImagePreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount(): Int = images.size

    fun submitList(newImages: List<Uri>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                images.add(i + 1, images.removeAt(i))
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                images.add(i - 1, images.removeAt(i))
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        onImageReordered(fromPosition, toPosition)
    }
}





class UriDiffCallback : DiffUtil.ItemCallback<Uri>() {
    override fun areItemsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
    override fun areContentsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
}

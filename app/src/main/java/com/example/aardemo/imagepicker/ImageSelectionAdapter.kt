package com.example.aardemo.imagepicker

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.aardemo.databinding.ItemImageSelectionBinding
import com.example.aardemo.operation.ImagePreviewAdapter


class ImageSelectionAdapter(
    private val maxSelection: Int,
    private val onImageSelected: (ImageItem, Boolean) -> Unit,
    private val getSelectedImages: () -> Set<ImageItem>
) : RecyclerView.Adapter<ImageSelectionAdapter.ViewHolder>() {

    private val images = mutableListOf<ImageItem>()

    inner class ViewHolder(private val binding: ItemImageSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ImageItem) {
            binding.apply {
                // Load image
                Glide.with(imageView)
                    .load(item.uri)
                    .override(300, 300)
                    .centerCrop()
                    .into(imageView)

                // Setup checkbox
                val isSelected = getSelectedImages().contains(item)
                checkbox.isChecked = isSelected
                selectionOverlay.isVisible = isSelected

                // Number indicator
                if (isSelected) {
                    val selectedList = getSelectedImages().toList()
                    val position = selectedList.indexOf(item) + 1
                    selectionNumber.text = position.toString()
                    selectionNumber.isVisible = false
                } else {
                    selectionNumber.isVisible = false
                }

                // Click listeners
                root.setOnClickListener {
                    val newState = !checkbox.isChecked
                    if (newState && getSelectedImages().size >= maxSelection) {
                        // Show max selection message
                        Toast.makeText(
                            root.context,
                            "Maximum ${maxSelection} images allowed",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    onImageSelected(item, newState)
                    checkbox.isChecked = newState
                    selectionOverlay.isVisible = newState
                    selectionNumber.isVisible = false
                    if (newState) {
                        selectionNumber.text = (getSelectedImages().size).toString()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        // Make the item square
        val displayMetrics = parent.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val itemSize = screenWidth / 3 // 3 columns
        binding.root.layoutParams.height = itemSize
        binding.root.layoutParams.width = itemSize

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    fun submitList(newImages: List<ImageItem>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    fun getSelectedPosition(item: ImageItem): Int {
        return getSelectedImages().indexOf(item)
    }
}

// ImageItem data class
data class ImageItem(
    val id: Long,
    val uri: Uri
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readParcelable(Uri::class.java.classLoader)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeParcelable(uri, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ImageItem> {
        override fun createFromParcel(parcel: Parcel): ImageItem {
            return ImageItem(parcel)
        }

        override fun newArray(size: Int): Array<ImageItem?> {
            return arrayOfNulls(size)
        }
    }
}


// Extension function for visibility
inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

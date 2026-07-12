package com.hiresplayer.ui.cloud

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hiresplayer.cloud.CloudFile
import com.hiresplayer.cloud.CloudFileType

class CloudFileAdapter(
    private val onOpenFolder: (CloudFile) -> Unit,
    private val onPlayFile: (CloudFile) -> Unit,
    private val onShowMenu: (CloudFile) -> Unit
) : ListAdapter<CloudFile, CloudFileAdapter.CloudFileViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CloudFileViewHolder {
        val row = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 22, 28, 22)
            background = RoundedRowDrawable()
        }
        return CloudFileViewHolder(row)
    }

    override fun onBindViewHolder(holder: CloudFileViewHolder, position: Int) {
        holder.bind(getItem(position), onOpenFolder, onPlayFile, onShowMenu)
    }

    class CloudFileViewHolder(
        private val row: LinearLayout
    ) : RecyclerView.ViewHolder(row) {
        private val icon = TextView(row.context).apply {
            textSize = 13f
            setTextColor(Color.rgb(0, 122, 255))
            gravity = Gravity.CENTER
            width = 92
        }
        private val title = TextView(row.context).apply {
            textSize = 16f
            setTextColor(Color.rgb(20, 24, 32))
            maxLines = 1
        }
        private val subtitle = TextView(row.context).apply {
            textSize = 12f
            setTextColor(Color.rgb(114, 122, 138))
            maxLines = 1
        }
        private val textColumn = LinearLayout(row.context).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(subtitle)
        }

        init {
            row.addView(icon)
            row.addView(
                textColumn,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
        }

        // Привязываем один элемент списка и разводим короткий/долгий тап по требованиям браузера.
        fun bind(
            file: CloudFile,
            onOpenFolder: (CloudFile) -> Unit,
            onPlayFile: (CloudFile) -> Unit,
            onShowMenu: (CloudFile) -> Unit
        ) {
            val isDirectory = file.type == CloudFileType.Directory
            icon.text = if (isDirectory) "DIR" else "♪"
            title.text = file.name
            subtitle.text = if (isDirectory) {
                "Папка"
            } else {
                file.name.substringAfterLast(".", missingDelimiterValue = "Аудио").uppercase()
            }
            row.alpha = 1f
            row.setOnClickListener {
                if (isDirectory) onOpenFolder(file) else onPlayFile(file)
            }
            row.setOnLongClickListener {
                if (!isDirectory) onShowMenu(file)
                !isDirectory
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CloudFile>() {
        override fun areItemsTheSame(oldItem: CloudFile, newItem: CloudFile): Boolean =
            oldItem.path == newItem.path

        override fun areContentsTheSame(oldItem: CloudFile, newItem: CloudFile): Boolean =
            oldItem == newItem
    }
}

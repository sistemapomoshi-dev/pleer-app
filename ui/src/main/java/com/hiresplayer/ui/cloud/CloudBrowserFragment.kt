package com.hiresplayer.ui.cloud

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hiresplayer.cloud.CloudFile

class CloudBrowserFragment : Fragment() {
    private lateinit var adapter: CloudFileAdapter
    private lateinit var pathView: TextView
    private lateinit var emptyView: TextView
    private lateinit var retryButton: Button

    var onOpenPath: (String) -> Unit = {}
    var onPlay: (CloudFile) -> Unit = {}
    var onAddFolderToLibrary: (String) -> Unit = {}
    var onDownload: (CloudFile) -> Unit = {}
    var onRetry: () -> Unit = {}

    private var currentPath: String = "disk:/"

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        adapter = CloudFileAdapter(
            onOpenFolder = { onOpenPath(it.path) },
            onPlayFile = onPlay,
            onShowMenu = ::showFileMenu
        )

        val recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CloudBrowserFragment.adapter
        }
        pathView = TextView(requireContext()).apply {
            textSize = 15f
            setPadding(28, 20, 28, 12)
        }
        emptyView = TextView(requireContext()).apply {
            text = "Здесь пока нет музыки"
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        retryButton = Button(requireContext()).apply {
            text = "Повторить"
            visibility = View.GONE
            setOnClickListener { onRetry() }
        }
        val addFolderButton = Button(requireContext()).apply {
            text = "Добавить папку в библиотеку"
            setOnClickListener { onAddFolderToLibrary(currentPath) }
        }

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            addView(pathView)
            addView(addFolderButton)
            addView(retryButton)
            addView(recyclerView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(emptyView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    // Обновляет текущий путь и список после ответа CloudProvider.list(...).
    fun render(path: String, files: List<CloudFile>) {
        currentPath = path
        pathView.text = breadcrumbText(path)
        adapter.submitList(files)
        emptyView.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        retryButton.visibility = View.GONE
    }

    fun renderNetworkError() {
        emptyView.text = "Не удалось загрузить список файлов"
        emptyView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
    }

    private fun showFileMenu(file: CloudFile) {
        PopupMenu(requireContext(), requireView()).apply {
            menu.add("Играть")
            menu.add("Добавить в библиотеку")
            menu.add("Скачать офлайн")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Играть" -> onPlay(file)
                    "Добавить в библиотеку" -> onAddFolderToLibrary(parentPathOf(file.path))
                    "Скачать офлайн" -> onDownload(file)
                }
                true
            }
            show()
        }
    }

    private fun breadcrumbText(path: String): String =
        "Диск" + path.removePrefix("disk:/").split("/").filter { it.isNotBlank() }.joinToString(separator = " / ", prefix = " / ")

    private fun parentPathOf(path: String): String {
        val current = path.removeSuffix("/")
        val parent = current.substringBeforeLast("/", missingDelimiterValue = "disk:")
        return if (parent == "disk:") "disk:/" else parent
    }
}

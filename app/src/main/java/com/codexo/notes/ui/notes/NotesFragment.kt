package com.codexo.notes.ui.notes

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.codexo.notes.R
import com.codexo.notes.adapters.NotesAdapter
import com.codexo.notes.data.Note
import com.codexo.notes.data.PreferenceManager
import com.codexo.notes.databinding.FragmentNotesBinding
import com.codexo.notes.ui.SharedViewModel
import com.codexo.notes.utils.SortBy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class NotesFragment : Fragment(R.layout.fragment_notes), NotesAdapter.OnItemClickListener {

    private val TAG = NotesFragment::class.java.simpleName
    private val viewModel: NotesViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by viewModels()
    private var _binding: FragmentNotesBinding? = null
    private val binding
        get() = _binding
    private val notesAdapter = NotesAdapter(this)
    private val prefs: PreferenceManager by lazy { PreferenceManager(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotesBinding.bind(view)

        Log.d(TAG, "onViewCreated:${SortBy.CREATED_AT.colName}")

        binding?.apply {
            rvNotes.apply {
                adapter = notesAdapter
                layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                setHasFixedSize(true)
            }
        }

        viewModel.allNotes.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                notesAdapter.setData(it)
            } else binding!!.apply {
                animationView.isVisible = true
                animationView.playAnimation()
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_notes, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        val pinFavoriteMenu = menu.findItem(R.id.action_pin_favorites)
        pinFavoriteMenu.isChecked = prefs.favoritePinnedStatus()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(searchQuery: String): Boolean {
                //viewModel.searchQuery = searchQuery
                searchNote(searchQuery)
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.action_pin_favorites -> {
                prefs.favoritePinned(!prefs.favoritePinnedStatus())
                viewModel.allNotes.observe(this, { notesAdapter.setData(it) })
                item.isChecked = prefs.favoritePinnedStatus()
                true
            }
            R.id.action_sort_by_title -> {
                prefs.setSortBy(SortBy.TITLE.colName)
                viewModel.allNotes.observe(this, { notesAdapter.setData(it) })
                true
            }
            R.id.action_sort_by_date_created -> {
                prefs.setSortBy(SortBy.CREATED_AT.colName)
                viewModel.allNotes.observe(this, { notesAdapter.setData(it) })
                true
            }
            R.id.action_sort_by_date_modified -> {
                prefs.setSortBy(SortBy.LAST_UPDATED_AT.colName)
                viewModel.allNotes.observe(this, { notesAdapter.setData(it) })
                true
            }
            R.id.action_settings -> {
                findNavController().navigate(R.id.action_notesFragment_to_settingsFragment)
                true
            }
            R.id.action_about -> {
                findNavController().navigate(R.id.action_notesFragment_to_aboutFragment)
                true
            }
            R.id.action_delete_all_notes -> {
                deleteAllDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteAllDialog() {
        if (!viewModel.allNotes.value.isNullOrEmpty()) {
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setPositiveButton("Yes") { _, _ ->

                val notes: List<Note> = viewModel.allNotes.value!!
                viewModel.deleteAllNotes()

                val snackBar = Snackbar.make(
                    binding!!.fabAdd, "All Notes Deleted!",
                    Snackbar.LENGTH_LONG
                )
                snackBar.setAction("Undo") {
                    for (note in notes) {
                        sharedViewModel.insertNote(note)
                        binding!!.animationView.isVisible = false
                    }
                }
                snackBar.show()
            }
            builder.setNegativeButton("No") { _, _ -> }
            builder.setTitle("Delete All notes?")
            builder.setMessage("Are you sure you want to Remove everything?")
            builder.create().show()
        } else {
            val snackBar = Snackbar.make(
                binding!!.fabAdd, "No Data to Delete",
                Snackbar.LENGTH_LONG
            )
            snackBar.show()
        }
    }

    private fun searchNote(query: String) {
        val searchQuery = "%${query}%"
        viewModel.searchNote(searchQuery).observe(this, { list ->
            list?.let {
                if (it.isNullOrEmpty()) {
                    binding!!.rvNotes.visibility = View.GONE
                    binding!!.tvNoNote.visibility = View.VISIBLE
                } else {
                    binding!!.tvNoNote.visibility = View.GONE
                    binding!!.rvNotes.visibility = View.VISIBLE
                    notesAdapter.setData(it)
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val sortBy = listOf("title", "created_at", "last_updated_at", "favorite")
    }

    override fun onResume() {
        super.onResume()
        viewModel.allNotes.observe(viewLifecycleOwner) { notesAdapter.setData(it) }
    }

    override fun onFavoriteClicked(markedFavorite: Boolean, id: Long) {
        sharedViewModel.markAsFavorite(markedFavorite, id)
    }
}
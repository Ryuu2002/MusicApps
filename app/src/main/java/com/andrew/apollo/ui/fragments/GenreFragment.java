/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.GenreAdapter;
import com.andrew.apollo.adapters.recycler.RecycleHolder;
import com.andrew.apollo.loaders.GenreLoader;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.ui.activities.ProfileActivity;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

import java.util.List;

/**
 * This class is used to display all of the genres on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class GenreFragment extends Fragment implements LoaderCallbacks<List<Genre>>,
		OnItemClickListener, FragmentCallback {

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x2D9C34D;

	/**
	 * LoaderCallbacks identifier
	 */
	private static final int LOADER_ID = 0x78BD76B9;

	/**
	 * The adapter for the list
	 */
	private GenreAdapter mAdapter;

	/**
	 * Genre song list
	 */
	@NonNull
	private long[] mGenreList = {};

	/**
	 * Empty constructor as per the {@link Fragment} documentation
	 */
	public GenreFragment() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Create the adapter
		mAdapter = new GenreAdapter(requireContext());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Init views
		View mRootView = inflater.inflate(R.layout.list_base, container, false);
		ListView mList = mRootView.findViewById(R.id.list_base);
		TextView emptyHolder = mRootView.findViewById(R.id.list_base_empty_info);
		//set listview
		mList.setEmptyView(emptyHolder);
		mList.setAdapter(mAdapter);
		mList.setRecyclerListener(new RecycleHolder());
		mList.setOnCreateContextMenuListener(this);
		mList.setOnItemClickListener(this);
		return mRootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// Enable the options menu
		setHasOptionsMenu(true);
		// Start the loader
		LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// Get the position of the selected item
		if (menuInfo instanceof AdapterContextMenuInfo) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			// Create a new genre
			Genre mGenre = mAdapter.getItem(info.position);
			if (mGenre != null) {
				// Create a list of the genre's songs
				mGenreList = MusicUtils.getSongListForGenres(requireContext(), mGenre.getGenreIds());
				// Play the genre
				menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
				// Add the genre to the queue
				menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
				return;
			}
		}
		// remove old selection if an error occurs
		mGenreList = new long[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID) {
			switch (item.getItemId()) {
				case FragmentMenuItems.PLAY_SELECTION:
					MusicUtils.playAll(mGenreList, 0, false);
					return true;

				case FragmentMenuItems.ADD_TO_QUEUE:
					MusicUtils.addToQueue(requireActivity(), mGenreList);
					return true;
			}
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Genre mGenre = mAdapter.getItem(position);
		// Create a new bundle to transfer the artist info
		Bundle bundle = new Bundle();
		bundle.putString(Config.IDS, ApolloUtils.serializeIDs(mGenre.getGenreIds()));
		bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Genres.CONTENT_TYPE);
		bundle.putString(Config.NAME, mGenre.getName());
		// Create the intent to launch the profile activity
		Intent intent = new Intent(requireContext(), ProfileActivity.class);
		intent.putExtras(bundle);
		startActivity(intent);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Loader<List<Genre>> onCreateLoader(int id, Bundle args) {
		return new GenreLoader(requireContext());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoadFinished(@NonNull Loader<List<Genre>> loader, @NonNull List<Genre> data) {
		// disable loader
		LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
		// Start fresh
		mAdapter.clear();
		// Add the data to the adapter
		for (Genre genre : data) {
			mAdapter.add(genre);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoaderReset(@NonNull Loader<List<Genre>> loader) {
		// Clear the data in the adapter
		mAdapter.clear();
	}


	@Override
	public void refresh() {
		LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
	}


	@Override
	public void setCurrentTrack() {
		// do nothing
	}
}
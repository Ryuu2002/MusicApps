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

package com.andrew.apollo.ui.fragments.profile;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.ArtistAlbumAdapter;
import com.andrew.apollo.loaders.ArtistAlbumLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.ui.views.dragdrop.VerticalScrollListener.ScrollableHeader;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import java.util.List;

/**
 * This class is used to display all of the albums from a particular artist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistAlbumFragment extends ProfileFragment implements LoaderCallbacks<List<Album>>, ScrollableHeader {

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x6CEDC429;

	/**
	 * LoaderCallbacks identifier
	 */
	private static final int LOADER_ID = 0x6D4DD8EA;

	/**
	 * The adapter for the grid
	 */
	private ArtistAlbumAdapter mAdapter;

	/**
	 * Album song list
	 */
	@NonNull
	private long[] mAlbumList = {};
	/**
	 * Represents an album
	 */
	@Nullable
	private Album mAlbum;


	@Override
	protected void init() {
		// Enable the options menu
		setHasOptionsMenu(true);
		// sets empty list text
		setEmptyText(R.string.empty_artst_albums);
		// set adapter
		mAdapter = new ArtistAlbumAdapter(requireActivity());
		setAdapter(mAdapter);
		// Start the loader
		Bundle arguments = getArguments();
		if (arguments != null) {
			LoaderManager.getInstance(this).initLoader(LOADER_ID, arguments, this);
		}
	}


	@Override
	protected void onItemClick(View v, int pos, long id) {
		if (v.getId() == R.id.image) {
			// Album art was clicked
			long[] list = MusicUtils.getSongListForAlbum(getContext(), id);
			MusicUtils.playAll(list, 0, false);
		} else {
			// open Album
			if (pos > 0) {
				Album album = mAdapter.getItem(pos);
				if (album != null) {
					NavUtils.openAlbumProfile(requireActivity(), album.getName(), album.getArtist(), album.getId());
					requireActivity().finish();
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPause() {
		super.onPause();
		mAdapter.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (menuInfo instanceof AdapterContextMenuInfo) {
			// Get the position of the selected item
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			// Create a new album
			mAlbum = mAdapter.getItem(info.position);
			// Create a list of the album's songs
			if (mAlbum != null) {
				mAlbumList = MusicUtils.getSongListForAlbum(requireContext(), mAlbum.getId());
				// Play the album
				menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
				// Add the album to the queue
				menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
				// Add the album to a playlist
				SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
				MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, false);
				// Delete the album
				menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
				return;
			}
		}
		// remove selected item if an error occurs
		mAlbumList = new long[0];
		mAlbum = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		// Avoid leaking context menu selections
		if (item.getGroupId() == GROUP_ID) {
			switch (item.getItemId()) {
				case FragmentMenuItems.PLAY_SELECTION:
					MusicUtils.playAll(mAlbumList, 0, false);
					return true;

				case FragmentMenuItems.ADD_TO_QUEUE:
					MusicUtils.addToQueue(requireActivity(), mAlbumList);
					return true;

				case FragmentMenuItems.NEW_PLAYLIST:
					CreateNewPlaylist.getInstance(mAlbumList).show(getParentFragmentManager(), "CreatePlaylist");
					return true;

				case FragmentMenuItems.PLAYLIST_SELECTED:
					long id = item.getIntent().getLongExtra("playlist", 0);
					MusicUtils.addToPlaylist(requireActivity(), mAlbumList, id);
					return true;

				case FragmentMenuItems.DELETE:
					if (mAlbum != null)
						MusicUtils.openDeleteDialog(requireActivity(), mAlbum.getName(), mAlbumList);
					return true;
			}
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Loader<List<Album>> onCreateLoader(int id, @Nullable Bundle args) {
		long artistId = args != null ? args.getLong(Config.ID) : 0;
		return new ArtistAlbumLoader(requireActivity(), artistId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoadFinished(@NonNull Loader<List<Album>> loader, @NonNull List<Album> data) {
		// disable loader
		LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
		// Start fresh
		mAdapter.clear();
		// Add the data to the adpater
		for (Album album : data) {
			mAdapter.add(album);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoaderReset(@NonNull Loader<List<Album>> loader) {
		// Clear the data in the adapter
		mAdapter.clear();
	}

	/**
	 * Restarts the loader.
	 */
	@Override
	public void refresh() {
		// Scroll to the top of the list before restarting the loader.
		// Otherwise, if the user has scrolled enough to move the header, it
		// becomes misplaced and needs to be reset.
		scrollToTop();
		LoaderManager.getInstance(this).restartLoader(LOADER_ID, getArguments(), this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScrollStateChanged(int scrollState) {
		boolean pauseCache = scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
				|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
		mAdapter.setPauseDiskCache(pauseCache);
	}

	@Override
	public void drop(int from, int to) {

	}

	@Override
	public void remove(int which) {

	}
}
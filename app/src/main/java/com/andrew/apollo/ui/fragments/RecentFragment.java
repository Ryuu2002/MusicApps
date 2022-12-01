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

import static com.andrew.apollo.utils.PreferenceUtils.RECENT_LAYOUT;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.AlbumAdapter;
import com.andrew.apollo.adapters.recycler.RecycleHolder;
import com.andrew.apollo.loaders.RecentLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.ui.activities.ActivityBase;
import com.andrew.apollo.ui.activities.ActivityBase.MusicStateListener;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.List;

/**
 * This class is used to display all of the recently listened to albums by the
 * user.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecentFragment extends Fragment implements LoaderCallbacks<List<Album>>,
		OnScrollListener, OnItemClickListener, MusicStateListener, FragmentCallback {

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x4FFF2B51;

	/**
	 * LoaderCallbacks identifier
	 */
	private static final int LOADER_ID = 0x178EB63F;

	/**
	 * Grid view column count. ONE - list, TWO - normal grid, FOUR - landscape
	 */
	private static final int ONE = 1, TWO = 2, FOUR = 4;

	/**
	 * The adapter for the grid
	 */
	private AlbumAdapter mAdapter;

	/**
	 * The Listview
	 */
	private GridView mList;

	/**
	 * Album song list
	 */
	private long[] mAlbumList = {};

	/**
	 * Represents an album
	 */
	@Nullable
	private Album mAlbum;

	/**
	 * app global prefs
	 */
	private PreferenceUtils preference;

	/**
	 * True if the list should execute {@code #restartLoader()}.
	 */
	private boolean mShouldRefresh = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		// init preferences
		preference = PreferenceUtils.getInstance(context);
		if (context instanceof ActivityBase) {
			// Register the music status listener
			((ActivityBase) context).setMusicStateListenerListener(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// init views
		View mRootView = inflater.inflate(R.layout.grid_base, container, false);
		TextView emptyInfo = mRootView.findViewById(R.id.grid_base_empty_info);
		mList = mRootView.findViewById(R.id.grid_base);
		// initialize list and adapter
		initList();
		// sets the empty view
		emptyInfo.setText(R.string.empty_recents);
		mList.setEmptyView(emptyInfo);
		// Set the data behind the list
		mList.setAdapter(mAdapter);
		// Release any references to the recycled Views
		mList.setRecyclerListener(new RecycleHolder());
		// Listen for ContextMenus to be created
		mList.setOnCreateContextMenuListener(this);
		// Show the albums and songs from the selected artist
		mList.setOnItemClickListener(this);
		// To help make scrolling smooth
		mList.setOnScrollListener(this);
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
			if (mAlbum != null) {
				// Create a list of the album's songs
				mAlbumList = MusicUtils.getSongListForAlbum(requireContext(), mAlbum.getId());
				// Play the album
				menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
				// Add the album to the queue
				menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
				// Add the album to a playlist
				SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
				MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, false);
				// View more content by the album artist
				menu.add(GROUP_ID, FragmentMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
				// Remove the album from the list
				menu.add(GROUP_ID, FragmentMenuItems.REMOVE_FROM_RECENT, Menu.NONE, R.string.context_menu_remove_from_recent);
				// Delete the album
				menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
			}
		} else {
			// remove selection if an error occurs
			mAlbum = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		// Avoid leaking context menu selections
		if (item.getGroupId() == GROUP_ID && mAlbum != null) {
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

				case FragmentMenuItems.MORE_BY_ARTIST:
					NavUtils.openArtistProfile(requireActivity(), mAlbum.getArtist());
					return true;

				case FragmentMenuItems.PLAYLIST_SELECTED:
					long id = item.getIntent().getLongExtra("playlist", 0);
					MusicUtils.addToPlaylist(requireActivity(), mAlbumList, id);
					return true;

				case FragmentMenuItems.REMOVE_FROM_RECENT:
					mShouldRefresh = true;
					RecentStore.getInstance(requireActivity()).removeItem(mAlbum.getId());
					MusicUtils.refresh();
					return true;

				case FragmentMenuItems.DELETE:
					MusicUtils.openDeleteDialog(requireActivity(), mAlbum.getName(), mAlbumList);
					mShouldRefresh = true;
					return true;
			}
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// Pause disk cache access to ensure smoother scrolling
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
				|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			mAdapter.setPauseDiskCache(true);
		} else {
			mAdapter.setPauseDiskCache(false);
			mAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (view.getId() == R.id.image) {
			long[] list = MusicUtils.getSongListForAlbum(getContext(), id);
			MusicUtils.playAll(list, 0, false);
		} else {
			Album selection = mAdapter.getItem(position);
			if (selection != null) {
				NavUtils.openAlbumProfile(requireActivity(), selection.getName(), selection.getArtist(), selection.getId());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Loader<List<Album>> onCreateLoader(int id, Bundle args) {
		return new RecentLoader(requireContext());
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
		// Add the data to the adapter
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
	 * {@inheritDoc}
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// Nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void restartLoader() {
		// Update the list when the user deletes any items
		if (mShouldRefresh) {
			LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
		}
		mShouldRefresh = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMetaChanged() {
		LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
	}


	@Override
	public void refresh() {
		// re init list
		initList();
		LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
	}


	@Override
	public void setCurrentTrack() {
		if (mList != null && mList.getCount() > 0) {
			mList.smoothScrollToPosition(0);
		}
	}

	/**
	 * initialize adapter & list
	 */
	private void initList() {
		if (preference.isSimpleLayout(RECENT_LAYOUT)) {
			mAdapter = new AlbumAdapter(requireActivity(), R.layout.list_item_normal);
		} else if (preference.isDetailedLayout(RECENT_LAYOUT)) {
			mAdapter = new AlbumAdapter(requireActivity(), R.layout.list_item_detailed);
		} else {
			mAdapter = new AlbumAdapter(requireActivity(), R.layout.grid_item_normal);
		}
		if (preference.isSimpleLayout(RECENT_LAYOUT)) {
			mList.setNumColumns(ONE);
		} else if (preference.isDetailedLayout(RECENT_LAYOUT)) {
			mAdapter.setLoadExtraData();
			if (ApolloUtils.isLandscape(requireContext())) {
				mList.setNumColumns(TWO);
			} else {
				mList.setNumColumns(ONE);
			}
		} else {
			if (ApolloUtils.isLandscape(requireContext())) {
				mList.setNumColumns(FOUR);
			} else {
				mList.setNumColumns(TWO);
			}
		}
		// set adapter and empty view for the list
		mList.setAdapter(mAdapter);
	}
}
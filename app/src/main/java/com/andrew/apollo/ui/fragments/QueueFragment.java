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

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.SongAdapter;
import com.andrew.apollo.adapters.recycler.RecycleHolder;
import com.andrew.apollo.loaders.NowPlayingCursor;
import com.andrew.apollo.loaders.QueueLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.FragmentMenuItems;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.provider.FavoritesStore;
import com.andrew.apollo.ui.views.dragdrop.DragSortListView;
import com.andrew.apollo.ui.views.dragdrop.DragSortListView.DragScrollProfile;
import com.andrew.apollo.ui.views.dragdrop.DragSortListView.DropListener;
import com.andrew.apollo.ui.views.dragdrop.DragSortListView.RemoveListener;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import java.util.List;

/**
 * This class is used to display all of the songs in the queue.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class QueueFragment extends Fragment implements LoaderCallbacks<List<Song>>, FragmentCallback,
		OnItemClickListener, DropListener, RemoveListener, DragScrollProfile {

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x4B079F4E;

	/**
	 * LoaderCallbacks identifier
	 */
	private static final int LOADER_ID = 0x3C6F54AB;

	/**
	 * The adapter for the list
	 */
	private SongAdapter mAdapter;

	/**
	 * The list view
	 */
	private DragSortListView mList;

	/**
	 * Represents a song
	 */
	@Nullable
	private Song mSong;

	/**
	 * Position of a context menu item
	 */
	private int mSelectedPosition = -1;

	/**
	 * Empty constructor as per the {@link Fragment} documentation
	 */
	public QueueFragment() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Create the adpater
		mAdapter = new SongAdapter(requireContext(), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// init views
		View rootView = inflater.inflate(R.layout.list_base, container, false);
		// empty info
		TextView emptyInfo = rootView.findViewById(R.id.list_base_empty_info);
		mList = rootView.findViewById(R.id.list_base);
		// setup listview
		mList.setAdapter(mAdapter);
		mList.setRecyclerListener(new RecycleHolder());
		mList.setOnCreateContextMenuListener(this);
		mList.setOnItemClickListener(this);
		mList.setDropListener(this);
		mList.setRemoveListener(this);
		mList.setDragScrollProfile(this);
		emptyInfo.setVisibility(View.INVISIBLE);
		return rootView;
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
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		inflater.inflate(R.menu.queue, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.menu_save_queue) {
			MusicUtils.saveQueue(requireActivity());
			return true;
		} else if (item.getItemId() == R.id.menu_clear_queue) {
			MusicUtils.clearQueue();
			NavUtils.goHome(requireActivity());
			return true;
		}
		return super.onOptionsItemSelected(item);
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
			mSelectedPosition = info.position;
			// Creat a new song
			mSong = mAdapter.getItem(mSelectedPosition);
			// Play the song next
			menu.add(GROUP_ID, FragmentMenuItems.PLAY_NEXT, Menu.NONE, R.string.context_menu_play_next);
			// Add the song to a playlist
			SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
			MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, true);
			// Remove the song from the queue
			menu.add(GROUP_ID, FragmentMenuItems.REMOVE_FROM_QUEUE, Menu.NONE, R.string.remove_from_queue);
			// View more content by the song artist
			menu.add(GROUP_ID, FragmentMenuItems.MORE_BY_ARTIST, Menu.NONE, R.string.context_menu_more_by_artist);
			// Make the song a ringtone
			menu.add(GROUP_ID, FragmentMenuItems.USE_AS_RINGTONE, Menu.NONE, R.string.context_menu_use_as_ringtone);
			// Delete the song
			menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
		} else {
			// remove old selection
			mSelectedPosition = -1;
			mSong = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (item.getGroupId() == GROUP_ID && mSong != null && mSelectedPosition >= 0) {
			long[] trackId = {mSong.getId()};

			switch (item.getItemId()) {
				case FragmentMenuItems.PLAY_NEXT:
					NowPlayingCursor queueCursor = new NowPlayingCursor(requireContext());
					queueCursor.removeItem(mSelectedPosition);
					queueCursor.close();
					MusicUtils.playNext(trackId);
					refresh();
					return true;

				case FragmentMenuItems.REMOVE_FROM_QUEUE:
					remove(mSelectedPosition);
					return true;

				case FragmentMenuItems.ADD_TO_FAVORITES:
					FavoritesStore.getInstance(requireActivity()).addSongId(mSong);
					return true;

				case FragmentMenuItems.NEW_PLAYLIST:
					CreateNewPlaylist.getInstance(trackId).show(getParentFragmentManager(), "CreatePlaylist");
					return true;

				case FragmentMenuItems.PLAYLIST_SELECTED:
					long mPlaylistId = item.getIntent().getLongExtra("playlist", 0);
					MusicUtils.addToPlaylist(requireActivity(), trackId, mPlaylistId);
					return true;

				case FragmentMenuItems.MORE_BY_ARTIST:
					NavUtils.openArtistProfile(requireActivity(), mSong.getArtist());
					return true;

				case FragmentMenuItems.USE_AS_RINGTONE:
					MusicUtils.setRingtone(requireActivity(), mSong.getId());
					return true;

				case FragmentMenuItems.DELETE:
					MusicUtils.openDeleteDialog(requireActivity(), mSong.getName(), trackId);
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
		// When selecting a track from the queue, just jump there instead of
		// reloading the queue. This is both faster, and prevents accidentally
		// dropping out of party shuffle.
		MusicUtils.setQueuePosition(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
		return new QueueLoader(requireContext());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoadFinished(@NonNull Loader<List<Song>> loader, @NonNull List<Song> data) {
		// disable loader
		LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
		// Start fresh
		mAdapter.clear();
		// Add the data to the adapter
		for (Song song : data) {
			mAdapter.add(song);
		}
		// set current track selection
		setCurrentTrack();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
		// Clear the data in the adapter
		mAdapter.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getSpeed(float w) {
		return Config.DRAG_DROP_MAX_SPEED * w;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(int which) {
		Song mSong = mAdapter.getItem(which);
		if (mSong != null) {
			// remove track from queue
			MusicUtils.removeQueueItem(which);
			// remove track from list
			mAdapter.remove(mSong);
			// check if queue is empty
			if (mAdapter.isEmpty()) {
				NavUtils.goHome(requireActivity());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void drop(int from, int to) {
		if (from != to) {
			MusicUtils.moveQueueItem(from, to);
		}
		mAdapter.moveTrack(from, to);
	}


	@Override
	public void refresh() {
		if (isAdded()) {
			LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
		}
	}


	@Override
	public void setCurrentTrack() {
		int pos = MusicUtils.getQueuePosition();
		if (mList != null && mAdapter != null && pos >= 0) {
			mList.smoothScrollToPosition(pos);
			mAdapter.setCurrentTrackPos(pos);
			mAdapter.notifyDataSetChanged();
		}
	}
}
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

package com.andrew.apollo;

/**
 * App-wide constants.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class Config {

	/**
	 * Used to distinguish album art from artist images
	 */
	public static final String ALBUM_ART_SUFFIX = "album";
	/**
	 * The ID of an artist, album, genre, or playlist passed to the profile activity
	 */
	public static final String ID = "id";
	/**
	 * a group of IDs
	 */
	public static final String IDS = "ids";
	/**
	 * The name of an artist, album, genre, or playlist passed to the profile activity
	 */
	public static final String NAME = "name";
	/**
	 * The name of an artist passed to the profile activity
	 */
	public static final String ARTIST_NAME = "artist_name";
	/**
	 * The year an album was released passed to the profile activity
	 */
	public static final String ALBUM_YEAR = "album_year";
	/**
	 * The MIME type passed to a the profile activity
	 */
	public static final String MIME_TYPE = "mime_type";
	/**
	 * path to a music folder
	 */
	public static final String FOLDER = "folder_path";
	/**
	 * Play from search intent
	 */
	public static final String PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
	/**
	 * user aent for Last FM
	 */
	public static final String USER_AGENT = "Apollo";

	/**
	 * MIME type for album/artist images
	 */
	public static final String MIME_IMAGE = "image/*";

	/**
	 * MIME type for sharing songs
	 */
	public static final String MIME_AUDIO = "audio/*";

	/**
	 * maximal scroll speed when dragging a list element
	 */
	public static final float DRAG_DROP_MAX_SPEED = 3.0f;

	/* This class is never initiated. */
	private Config() {
	}
}
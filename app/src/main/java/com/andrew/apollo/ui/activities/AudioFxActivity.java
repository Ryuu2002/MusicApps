package com.andrew.apollo.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.andrew.apollo.R;
import com.andrew.apollo.adapters.EqualizerAdapter;
import com.andrew.apollo.adapters.EqualizerAdapter.BandLevelChangeListener;
import com.andrew.apollo.player.AudioEffects;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeUtils;

/**
 * Audio effects activity
 *
 * @author nuclerfog
 */
public class AudioFxActivity extends AppCompatActivity implements BandLevelChangeListener, OnCheckedChangeListener, OnSeekBarChangeListener {

	/**
	 * maximum steps of the bassboost seekbar
	 */
	private static final int BASS_STEPS = 20;

	private AudioEffects audioEffects;

	@Override
	protected void onCreate(Bundle inst) {
		super.onCreate(inst);
		setContentView(R.layout.activity_audiofx);
		CompoundButton enableFx = findViewById(R.id.audiofx_enable);
		RecyclerView eq_bands = findViewById(R.id.audiofx_eq_scroll);
		Toolbar toolbar = findViewById(R.id.audiofx_toolbar);
		SeekBar bassBoost = findViewById(R.id.audiofx_bass_boost);
		SeekBar reverb = findViewById(R.id.audiofx_reverb);

		bassBoost.setMax(BASS_STEPS);
		reverb.setMax(AudioEffects.MAX_REVERB);
		eq_bands.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			ThemeUtils mResources = new ThemeUtils(this);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			mResources.themeActionBar(getSupportActionBar(), R.string.title_audio_effects);
		}

		audioEffects = AudioEffects.getInstance(this, MusicUtils.getAudioSessionId());
		if (audioEffects != null) {
			EqualizerAdapter adapter = new EqualizerAdapter(this, audioEffects.getBandLevel(), audioEffects.getBandFrequencies(), audioEffects.getBandLevelRange());
			eq_bands.setAdapter(adapter);
			enableFx.setChecked(audioEffects.isAudioFxEnabled());
			bassBoost.setProgress(audioEffects.getBassLevel() * BASS_STEPS / AudioEffects.MAX_BASSBOOST);
			reverb.setProgress(audioEffects.getReverbLevel());

			enableFx.setOnCheckedChangeListener(this);
			bassBoost.setOnSeekBarChangeListener(this);
			reverb.setOnSeekBarChangeListener(this);
		} else {
			Toast.makeText(this, R.string.error_audioeffects_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBandLevelChange(int pos, int level) {
		audioEffects.setBandLevel(pos, level);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView.getId() == R.id.audiofx_enable) {
			audioEffects.enableAudioFx(isChecked);
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			// set bass boost
			if (seekBar.getId() == R.id.audiofx_bass_boost) {
				audioEffects.setBassLevel(progress * AudioEffects.MAX_BASSBOOST / BASS_STEPS);
			}
			// set reverb
			else if (seekBar.getId() == R.id.audiofx_reverb) {
				audioEffects.setReverbLevel(progress);
			}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}
}
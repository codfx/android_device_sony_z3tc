/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.chargelimit;

import android.app.Activity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Settings > Battery > Charge limit (also opened by long-pressing the tile). */
public class ChargeLimitActivity extends Activity {
    private Switch mEnable;
    private SeekBar mLimit;
    private SeekBar mResume;
    private TextView mLimitLabel;
    private TextView mResumeLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charge_limit);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mEnable = findViewById(R.id.enable);
        mLimit = findViewById(R.id.limit);
        mResume = findViewById(R.id.resume);
        mLimitLabel = findViewById(R.id.limit_label);
        mResumeLabel = findViewById(R.id.resume_label);

        mLimit.setMax(ChargeLimit.LIMIT_MAX - ChargeLimit.LIMIT_MIN);
        mResume.setMax(ChargeLimit.LIMIT_MAX - 1 - ChargeLimit.RESUME_MIN);

        mEnable.setOnCheckedChangeListener((v, checked) -> {
            ChargeLimit.setEnabled(checked);
            updateEnabledState(checked);
        });

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int limit = limitFromBar();
                int resume = resumeFromBar();
                // Keep resume below limit while dragging either bar.
                if (bar == mLimit && resume >= limit) {
                    mResume.setProgress(limit - 1 - ChargeLimit.RESUME_MIN);
                } else if (bar == mResume && resume >= limit) {
                    mLimit.setProgress(resume + 1 - ChargeLimit.LIMIT_MIN);
                }
                updateLabels();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                // Store only when the user lets go: each change re-runs the limiter script.
                ChargeLimit.setLevels(limitFromBar(), resumeFromBar());
                load();
            }
        };
        mLimit.setOnSeekBarChangeListener(listener);
        mResume.setOnSeekBarChangeListener(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    private void load() {
        boolean enabled = ChargeLimit.isEnabled();
        mEnable.setChecked(enabled);
        mLimit.setProgress(ChargeLimit.getLimit() - ChargeLimit.LIMIT_MIN);
        mResume.setProgress(ChargeLimit.getResume() - ChargeLimit.RESUME_MIN);
        updateLabels();
        updateEnabledState(enabled);
    }

    private int limitFromBar() {
        return mLimit.getProgress() + ChargeLimit.LIMIT_MIN;
    }

    private int resumeFromBar() {
        return mResume.getProgress() + ChargeLimit.RESUME_MIN;
    }

    private void updateLabels() {
        mLimitLabel.setText(getString(R.string.limit_label) + ": "
                + getString(R.string.percent, limitFromBar()));
        mResumeLabel.setText(getString(R.string.resume_label) + ": "
                + getString(R.string.percent, resumeFromBar()));
    }

    private void updateEnabledState(boolean enabled) {
        mLimit.setEnabled(enabled);
        mResume.setEnabled(enabled);
        mLimitLabel.setEnabled(enabled);
        mResumeLabel.setEnabled(enabled);
    }
}

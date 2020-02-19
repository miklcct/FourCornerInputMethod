/* Copyright 2014 Michael Tsang, Garry Ng, Ken Leung
 *
 * This file is part of Four Corner Input Method.
 *
 * Four Corner Input Method is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Four Corner Input Method is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Four Corner Input Method.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.miklcct.csproject.fourcornerinputmethod;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class CandidatesView extends LinearLayout {
	public CandidatesView(Context context, int size) {
		super(context);
		service = (FourCornerMethodService) context;
		setBackgroundColor(context.getResources().getColor(
				R.color.candidate_background));
		LayoutParams leftRightParams = new LayoutParams(
				0, LayoutParams.MATCH_PARENT, 0.5f);
		leftButton = new Button(context);
		leftButton.setText(R.string.prev_page);
		leftButton.setLayoutParams(leftRightParams);
		leftButton.setEnabled(false);
		leftButton.setOnClickListener(switchPageListener);
		leftButton.setBackgroundColor(context.getResources().getColor(
				R.color.candidate_background));
		leftButton.setTextColor(context.getResources().getColor(
				R.color.left_right_buttons));
		rightButton = new Button(context);
		rightButton.setText(R.string.next_page);
		rightButton.setLayoutParams(leftRightParams);
		rightButton.setEnabled(false);
		rightButton.setOnClickListener(switchPageListener);
		rightButton.setBackgroundColor(context.getResources().getColor(
				R.color.candidate_background));
		rightButton.setTextColor(context.getResources().getColor(
				R.color.left_right_buttons));
		LayoutParams params = new LayoutParams(0,
				LayoutParams.MATCH_PARENT, 1.0f);
		candidateViews = new Button[size];
		addView(leftButton);
		for (int i = 0; i < size; ++i) {
			candidateViews[i] = new Button(context);
			candidateViews[i].setLayoutParams(params);
			candidateViews[i].setEnabled(false);
			candidateViews[i].setTextSize(getResources().getInteger(R.integer.font_size));
			candidateViews[i].setTextColor(context.getResources().getColor(
					R.color.candidate_foreground));
			candidateViews[i].setBackgroundColor(context.getResources().getColor(
					R.color.candidate_background));
			candidateViews[i].setOnClickListener(service);
			addView(candidateViews[i]);
		}
		addView(rightButton);
	}

	public void setCandidates(String[] newCandidates) {
		candidates = newCandidates;
		switchToPage(0);
	}

	private void switchToPage(int page) {
		currentPage = page;
		for (int i = 0; i < candidateViews.length; ++i) {
			int indexInCandidates = page * candidateViews.length + i;
			if (indexInCandidates < candidates.length) {
				candidateViews[i].setText(candidates[indexInCandidates]);
				candidateViews[i].setEnabled(true);
			} else {
				candidateViews[i].setText("");
				candidateViews[i].setEnabled(false);
			}
		}
		leftButton.setEnabled(page > 0);
		rightButton.setEnabled(page < Math.ceil((double) candidates.length
				/ candidateViews.length) - 1);
	}

	private OnClickListener switchPageListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v == leftButton) {
				switchToPage(currentPage - 1);
			}
			if (v == rightButton) {
				switchToPage(currentPage + 1);
			}
		}
	};
		
	private FourCornerMethodService service;
	private int currentPage;
	private String[] candidates;
	private Button leftButton, rightButton;
	private Button[] candidateViews;
}

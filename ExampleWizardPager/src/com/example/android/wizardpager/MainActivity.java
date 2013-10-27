/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wizardpager;

import se.springworks.libwizardpager.activity.WizardActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import co.juliansuarez.libwizardpager.wizard.model.AbstractWizardModel;

public class MainActivity extends WizardActivity {

	
	@Override
	protected void onNextAtLastPage() {
		DialogFragment dg = new DialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				return new AlertDialog.Builder(getActivity())
					.setMessage(com.example.android.wizardpager.R.string.submit_confirm_message)
					.setPositiveButton(com.example.android.wizardpager.R.string.submit_confirm_button, null)
					.setNegativeButton(android.R.string.cancel, null).create();
			}
		};
		dg.show(getSupportFragmentManager(), "place_order_dialog");
	}

	@Override
	protected AbstractWizardModel createWizardModel() {
		return new SandwichWizardModel(this);
	}
}

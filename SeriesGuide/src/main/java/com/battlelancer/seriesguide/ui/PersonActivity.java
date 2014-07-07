/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.ui;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;

/**
 * Hosts a {@link com.battlelancer.seriesguide.ui.PersonFragment}, only used on handset devices. On
 * tablet-size devices a two-pane layout inside {@link com.battlelancer.seriesguide.ui.PeopleActivity}
 * is used.
 */
public class PersonActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        setupActionBar();

        if (savedInstanceState == null) {
            PersonFragment f = PersonFragment.newInstance(
                    getIntent().getIntExtra(PersonFragment.InitBundle.PERSON_TMDB_ID, 0));
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.containerPerson, f)
                    .commit();
        }
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

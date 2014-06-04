
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

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;

/**
 * Adds action items specific to top show activities.
 */
public abstract class BaseTopShowsActivity extends BaseTopActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.base_show_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_checkin) {
            startActivity(new Intent(this, CheckinActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            fireTrackerEvent("Check-In");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content
        // view
        boolean isDrawerOpen = isDrawerOpen();
        menu.findItem(R.id.menu_checkin).setVisible(!isDrawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }
}

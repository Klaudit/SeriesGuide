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

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.database.Cursor;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

public class EpisodeTools {

    /**
     * Checks the database whether there is an entry for this episode.
     */
    public static boolean isEpisodeExists(Context context, int episodeTvdbId) {
        Cursor query = context.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId), new String[] {
                        SeriesGuideContract.Episodes._ID }, null, null, null
        );
        if (query == null) {
            return false;
        }

        boolean isExists = query.getCount() > 0;
        query.close();

        return isExists;
    }

    public static boolean isSkipped(int episodeFlags) {
        return episodeFlags == EpisodeFlags.SKIPPED;
    }

    public static boolean isUnwatched(int episodeFlags) {
        return episodeFlags == EpisodeFlags.UNWATCHED;
    }

    public static boolean isWatched(int episodeFlags) {
        return episodeFlags == EpisodeFlags.WATCHED;
    }

    public static void validateFlags(int episodeFlags) {
        boolean hasValidFlag = false;

        if (isUnwatched(episodeFlags)) {
            return;
        }
        if (isSkipped(episodeFlags)) {
            return;
        }
        if (isWatched(episodeFlags)) {
            return;
        }

        if (!hasValidFlag) {
            throw new IllegalArgumentException(
                    "Did not pass a valid episode flag. See EpisodeFlags class for details.");
        }
    }
}

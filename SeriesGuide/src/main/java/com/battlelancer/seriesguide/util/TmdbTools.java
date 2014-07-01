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
import android.content.Intent;
import android.net.Uri;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import java.text.DecimalFormat;

public class TmdbTools {

    public enum ProfileImageSize {

        W45("w45"),
        W185("w185"),
        H632("h632"),
        ORIGINAL("original");

        private final String value;

        private ProfileImageSize(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final String BASE_URL = "https://www.themoviedb.org/";
    private static final String PATH_MOVIES = "movie/";

    private static DecimalFormat RATING_FORMAT = new DecimalFormat("0.0");

    public static String buildRatingValue(Double value) {
        return value == null ? "-.-" : RATING_FORMAT.format(value);
    }

    public static void openTmdb(Context context, int movieTmdbId, final String logTag) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(buildMovieUrl(movieTmdbId)));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);

        Utils.trackAction(context, logTag, "TMDb");
    }

    private static String buildMovieUrl(int movieTmdbId) {
        return BASE_URL + PATH_MOVIES + movieTmdbId;
    }

    /**
     * Build url to a profile image using the given size spec and current TMDb image url (see
     * {@link com.battlelancer.seriesguide.settings.TmdbSettings#getImageBaseUrl(android.content.Context)}.
     */
    public static String buildProfileImageUrl(Context context, String path, ProfileImageSize size) {
        return TmdbSettings.getImageBaseUrl(context) + size + path;
    }
}

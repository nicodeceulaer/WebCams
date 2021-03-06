/*
* ******************************************************************************
* Copyright (c) 2013-2015 Tomas Valenta.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* *****************************************************************************
*/

package cz.yetanotherview.webcamviewer.app.model;

import cz.yetanotherview.webcamviewer.app.R;

public class Icons {

    private final int[] iconIds = {R.drawable.icon_airports, R.drawable.icon_animals, R.drawable.icon_beaches,
            R.drawable.icon_bridges, R.drawable.icon_buildings, R.drawable.icon_castles,
            R.drawable.icon_cities, R.drawable.icon_constructions, R.drawable.icon_harbours,
            R.drawable.icon_churches, R.drawable.icon_indoors, R.drawable.icon_lakes,
            R.drawable.icon_landscapes, R.drawable.icon_market_square, R.drawable.icon_mountains,
            R.drawable.icon_others, R.drawable.icon_parks, R.drawable.icon_pools,
            R.drawable.icon_radio_studios, R.drawable.icon_railways, R.drawable.icon_rivers,
            R.drawable.icon_ski_resorts, R.drawable.icon_traffic_cameras, R.drawable.icon_universities,
            R.drawable.icon_all_imported, R.drawable.icon_country, R.drawable.icon_imported,
            R.drawable.icon_latest, R.drawable.icon_live_streams, R.drawable.icon_manual,
            R.drawable.icon_map, R.drawable.icon_nearby, R.drawable.icon_popular, R.drawable.icon_selected,
            R.drawable.icon_unknown, R.drawable.icon_custom0, R.drawable.icon_custom1, R.drawable.icon_custom2,
            R.drawable.icon_custom3, R.drawable.icon_custom4};

    private final String[] iconNames = {"airports", "animals", "beaches", "bridges", "buildings", "castles",
            "cities", "constructions", "harbours", "churches", "indoors", "lakes", "landscapes",
            "market_square", "mountains", "others", "parks", "pools", "radio_studios", "railways",
            "rivers", "ski_resorts", "traffic_cameras", "universities", "all_imported", "country",
            "imported", "latest", "live_streams", "manual", "map", "nearby", "popular", "selected", "unknown",
            "custom0", "custom1", "custom2", "custom3", "custom4"};

    // constructor
    public Icons() {
    }

    public int getIconId(int iconId) {
        return this.iconIds[iconId];
    }

    public String getIconName(int iconId) {
        return this.iconNames[iconId];
    }

    public int[] getIconsIds() {
        return this.iconIds;
    }
}

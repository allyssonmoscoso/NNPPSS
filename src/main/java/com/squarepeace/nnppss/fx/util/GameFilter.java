package com.squarepeace.nnppss.fx.util;

import com.squarepeace.nnppss.model.Game;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Pure filtering logic for the game table. Keeping it outside the controller
 * makes it unit-testable and keeps the JavaFX code declarative.
 */
public final class GameFilter {

    private GameFilter() {
    }

    /**
     * Builds a predicate for the current filter controls.
     *
     * @param search         free text; matched against title, region, content id and URL
     * @param region         selected region or {@code allRegions}
     * @param allRegions     localized "all regions" value
     * @param sizeFilter     selected size bucket or {@code allSizes}
     * @param allSizes       localized "all sizes" value
     * @param lessThanOneGb  localized "< 1GB" value
     * @param oneToFiveGb    localized "1-5GB" value
     * @param moreThanFiveGb localized "> 5GB" value
     */
    public static Predicate<Game> predicate(String search,
                                            String region,
                                            String allRegions,
                                            String sizeFilter,
                                            String allSizes,
                                            String lessThanOneGb,
                                            String oneToFiveGb,
                                            String moreThanFiveGb) {
        return predicate(search, region, allRegions, sizeFilter, allSizes,
                lessThanOneGb, oneToFiveGb, moreThanFiveGb,
                null, null, null, null, null, null, null, game -> null);
    }

    /**
     * Builds a predicate that also filters by the status shown in the table.
     * The status resolver returns {@code completed}, {@code queued},
     * {@code failed}, {@code paused} or {@code null}.
     */
    public static Predicate<Game> predicate(String search,
                                            String region,
                                            String allRegions,
                                            String sizeFilter,
                                            String allSizes,
                                            String lessThanOneGb,
                                            String oneToFiveGb,
                                            String moreThanFiveGb,
                                            String statusFilter,
                                            String allStatuses,
                                            String downloaded,
                                            String queued,
                                            String failed,
                                            String paused,
                                            String noStatus,
                                            Function<Game, String> statusResolver) {
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        return game -> {
            if (game == null) {
                return false;
            }

            if (!term.isEmpty()
                    && !contains(game.getTitle(), term)
                    && !contains(game.getRegion(), term)
                    && !contains(game.getContentId(), term)
                    && !contains(game.getPkgUrl(), term)) {
                return false;
            }

            if (region != null && !region.equals(allRegions)) {
                if (game.getRegion() == null || !game.getRegion().contains(region)) {
                    return false;
                }
            }

            if (sizeFilter != null && !sizeFilter.equals(allSizes)) {
                double sizeGb = game.getFileSize() / (1024.0 * 1024.0 * 1024.0);
                if (sizeFilter.equals(lessThanOneGb) && sizeGb >= 1.0) {
                    return false;
                }
                if (sizeFilter.equals(oneToFiveGb) && (sizeGb < 1.0 || sizeGb > 5.0)) {
                    return false;
                }
                if (sizeFilter.equals(moreThanFiveGb) && sizeGb <= 5.0) {
                    return false;
                }
            }

            if (statusFilter != null && !statusFilter.equals(allStatuses)) {
                String status = statusResolver == null ? null : statusResolver.apply(game);
                boolean matches;
                if (statusFilter.equals(downloaded)) {
                    matches = "completed".equals(status);
                } else if (statusFilter.equals(queued)) {
                    matches = "queued".equals(status);
                } else if (statusFilter.equals(failed)) {
                    matches = "failed".equals(status);
                } else if (statusFilter.equals(paused)) {
                    matches = "paused".equals(status);
                } else if (statusFilter.equals(noStatus)) {
                    matches = status == null;
                } else {
                    matches = true;
                }
                if (!matches) {
                    return false;
                }
            }

            return true;
        };
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }
}

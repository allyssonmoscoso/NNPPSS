package com.squarepeace.nnppss.fx.util;

import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.Game;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameFilterTest {

    private static final String ALL_REGIONS = "All regions";
    private static final String ALL_SIZES = "All sizes";
    private static final String LT_1GB = "< 1GB";
    private static final String ONE_TO_FIVE_GB = "1-5GB";
    private static final String GT_5GB = "> 5GB";
    private static final String ALL_STATUSES = "All statuses";
    private static final String DOWNLOADED = "Downloaded";
    private static final String QUEUED = "Queued";
    private static final String FAILED = "Failed";
    private static final String PAUSED = "Paused";
    private static final String NO_STATUS = "No status";

    private final Game alpha = game("Alpha Adventure", "USA", "PCSE00001",
            "https://example.test/alpha.pkg", 500L * 1024 * 1024);
    private final Game beta = game("Beta Quest", "EUR", "PCSB00002",
            "https://example.test/beta.pkg", 3L * 1024 * 1024 * 1024);
    private final Game gamma = game("Gamma Saga", "JPN", "PCSC00003",
            "https://example.test/gamma.pkg", 8L * 1024 * 1024 * 1024);

    @Test
    void allValuesMatchEverything() {
        Predicate<Game> predicate = filter("", ALL_REGIONS, ALL_SIZES);

        assertTrue(predicate.test(alpha));
        assertTrue(predicate.test(beta));
        assertTrue(predicate.test(gamma));
    }

    @Test
    void searchMatchesTitleRegionContentIdAndUrl() {
        assertTrue(filter("alpha", ALL_REGIONS, ALL_SIZES).test(alpha));
        assertTrue(filter("EUR", ALL_REGIONS, ALL_SIZES).test(beta));
        assertTrue(filter("PCSC00003", ALL_REGIONS, ALL_SIZES).test(gamma));
        assertTrue(filter("gamma.pkg", ALL_REGIONS, ALL_SIZES).test(gamma));

        assertFalse(filter("alpha", ALL_REGIONS, ALL_SIZES).test(beta));
    }

    @Test
    void searchIsCaseInsensitive() {
        assertTrue(filter("ALPHA", ALL_REGIONS, ALL_SIZES).test(alpha));
        assertTrue(filter("eur", ALL_REGIONS, ALL_SIZES).test(beta));
    }

    @Test
    void regionFilterKeepsOnlyTheSelectedRegion() {
        Predicate<Game> predicate = filter("", "EUR", ALL_SIZES);

        assertTrue(predicate.test(beta));
        assertFalse(predicate.test(alpha));
        assertFalse(predicate.test(gamma));
    }

    @Test
    void sizeBucketsAreInclusiveAsExpected() {
        assertTrue(filter("", ALL_REGIONS, LT_1GB).test(alpha));
        assertFalse(filter("", ALL_REGIONS, LT_1GB).test(beta));

        assertTrue(filter("", ALL_REGIONS, ONE_TO_FIVE_GB).test(beta));
        assertFalse(filter("", ALL_REGIONS, ONE_TO_FIVE_GB).test(alpha));
        assertFalse(filter("", ALL_REGIONS, ONE_TO_FIVE_GB).test(gamma));

        assertTrue(filter("", ALL_REGIONS, GT_5GB).test(gamma));
        assertFalse(filter("", ALL_REGIONS, GT_5GB).test(beta));
    }

    @Test
    void filtersCombine() {
        assertTrue(filter("beta", "EUR", ONE_TO_FIVE_GB).test(beta));
        assertFalse(filter("beta", "USA", ONE_TO_FIVE_GB).test(beta));
        assertFalse(filter("beta", "EUR", LT_1GB).test(beta));
    }

    @Test
    void statusFilterKeepsOnlyTheSelectedState() {
        Predicate<Game> downloaded = statusFilter(DOWNLOADED,
                game -> game == alpha ? "completed" : "queued");
        assertTrue(downloaded.test(alpha));
        assertFalse(downloaded.test(beta));

        Predicate<Game> queued = statusFilter(QUEUED,
                game -> game == alpha ? "completed" : "queued");
        assertTrue(queued.test(beta));
        assertFalse(queued.test(alpha));

        Predicate<Game> failed = statusFilter(FAILED, game -> "failed");
        assertTrue(failed.test(gamma));

        Predicate<Game> noStatus = statusFilter(NO_STATUS, game -> null);
        assertTrue(noStatus.test(gamma));
    }

    private Predicate<Game> filter(String search, String region, String size) {
        return GameFilter.predicate(search, region, ALL_REGIONS, size,
                ALL_SIZES, LT_1GB, ONE_TO_FIVE_GB, GT_5GB);
    }

    private Predicate<Game> statusFilter(String status,
                                         java.util.function.Function<Game, String> resolver) {
        return GameFilter.predicate("", ALL_REGIONS, ALL_REGIONS, ALL_SIZES,
                ALL_SIZES, LT_1GB, ONE_TO_FIVE_GB, GT_5GB,
                status, ALL_STATUSES, DOWNLOADED, QUEUED, FAILED, PAUSED,
                NO_STATUS, resolver);
    }

    private static Game game(String title, String region, String contentId, String url, long size) {
        Game game = new Game(title, region, Console.PSVITA, url, "zrif", size);
        game.setContentId(contentId);
        return game;
    }
}

package pl.rentowne.settlement;


public interface SettlementStatsService {
    /**
     * Pobiera dane do wyświetlenia statystyk rozliczeń
     * @return {@link SettlementStats}
     */
    SettlementStats getStatistics();

}


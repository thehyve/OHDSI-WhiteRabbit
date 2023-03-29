package org.ohdsi.databases;

public interface ScanParameters {

    public boolean doCalculateNumericStats();

    public int getNumStatsSamplerSize();

    public int getMaxValues();

    public boolean doScanValues();

    public int getMinCellCount();

    public int getSampleSize();

    public static int	MAX_VALUES_IN_MEMORY				= 100000;
    public static int	MIN_CELL_COUNT_FOR_CSV				= 1000000;
    public static int	N_FOR_FREE_TEXT_CHECK				= 1000;
    public static int	MIN_AVERAGE_LENGTH_FOR_FREE_TEXT	= 100;
}

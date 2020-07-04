package io.melvinjones.weatherservice;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;

public class WeatherRecordProcessorFactory implements IRecordProcessorFactory {
    /**
     * Constructor.
     */
    public WeatherRecordProcessorFactory() {
        super();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public IRecordProcessor createProcessor() {
        return new WeatherRecordsProcessor();
    }
}

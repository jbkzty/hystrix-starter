package hystrix.command;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixThreadPoolKey;
import hystrix.domain.RaceCourse;
import hystrix.exception.RemoteServiceException;
import hystrix.service.BettingService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author spuerKun
 * @date 2018/9/23.
 */
public class CommandGetTodaysRaces extends HystrixCommand<List<RaceCourse>> {

    private final BettingService bettingService;

    private final boolean failSilently;

    /**
     * CommandGetTodaysRaces
     *
     * @param bettingService Remote Broker Service
     * @param failSilently   If <code>true</code> will return an empty list if a remote service exception is thrown, if
     *                       <code>false</code> will throw a BettingServiceException.
     */
    public CommandGetTodaysRaces(BettingService bettingService, boolean failSilently) {

        super(Setter.withGroupKey(
                HystrixCommandGroupKey.Factory.asKey("BettingServiceGroup"))
                .andThreadPoolKey(
                        HystrixThreadPoolKey.Factory.asKey("BettingServicePool")));

        this.bettingService = bettingService;
        this.failSilently = failSilently;

    }

    public CommandGetTodaysRaces(BettingService bettingService) {
        this(bettingService, true);
    }

    @Override
    protected List<RaceCourse> run() throws Exception {
        return bettingService.getTodayRaces();
    }

    @Override
    protected List<RaceCourse> getFallback() {
        // can log here, throw exception or return default
        if (failSilently) {
            return new ArrayList<RaceCourse>();
        }

        throw new RemoteServiceException("Unexpected error retrieving todays races");
    }
}

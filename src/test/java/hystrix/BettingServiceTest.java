package hystrix;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixRequestCache;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import hystrix.command.CommandGetHorsesInRaceWithCaching;
import hystrix.command.CommandGetTodaysRaces;
import hystrix.domain.Horse;
import hystrix.domain.RaceCourse;
import hystrix.exception.RemoteServiceException;
import hystrix.service.BettingService;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author spuerKun
 * @date 2018/9/23.
 */
public class BettingServiceTest {

    private static final String RACE_1 = "course_france";
    private static final String HORSE_1 = "horse_white";
    private static final String HORSE_2 = "horse_black";

    private static final String ODDS_RACE_1_HORSE_1 = "10/1";
    private static final String ODDS_RACE_1_HORSE_2 = "100/1";

    private static final HystrixCommandKey GETTER_KEY = HystrixCommandKey.Factory.asKey("GetterCommand");

    private BettingService mockService;

    /**
     * Set up the shared Unit Test environment
     */
    @Before
    public void setUp() {
        mockService = mock(BettingService.class);
        when(mockService.getTodayRaces()).thenReturn(getRaceCourses());
        when(mockService.getHorsesInRace(RACE_1)).thenReturn(getHorsesAtFrance());
        when(mockService.getOddsForHorse(RACE_1, HORSE_1)).thenReturn(ODDS_RACE_1_HORSE_1);
        when(mockService.getOddsForHorse(RACE_1, HORSE_2)).thenReturn(ODDS_RACE_1_HORSE_2);
    }

    /**
     * 正常的方法执行
     */
    @Test
    public void testSynchronous() {

        CommandGetTodaysRaces commandGetRaces = new CommandGetTodaysRaces(mockService);
        assertEquals(getRaceCourses(), commandGetRaces.execute());

        // 验证方法的调用次数
        verify(mockService).getTodayRaces();

        // 查询是否存在被调用，但未被验证的方法，如果存在则抛出异常
        verifyNoMoreInteractions(mockService);
    }

    /**
     * 降级调用failBack
     */
    @Test
    public void testSynchronousFailSilently() {

        CommandGetTodaysRaces commandGetRacesFailure = new CommandGetTodaysRaces(mockService);

        // 模拟验证的时候抛出一个异常
        when(mockService.getTodayRaces()).thenThrow(new RuntimeException("Error!!"));

        // 由于是快速失败的设置，因此只是返回一个空数组
        assertEquals(new ArrayList<RaceCourse>(), commandGetRacesFailure.execute());

        // 验证方法的调用次数
        verify(mockService).getTodayRaces();

        // 查询是否存在被调用，但未被验证的方法，如果存在则抛出异常
        verifyNoMoreInteractions(mockService);
    }

    /**
     * 降级抛出异常
     */
    @Test
    public void testSynchronousFailFast() {

        CommandGetTodaysRaces commandGetRacesFailure = new CommandGetTodaysRaces(mockService, false);

        // 模拟验证的时候抛出一个异常
        when(mockService.getTodayRaces()).thenThrow(new RuntimeException("Error!!"));

        try {
            commandGetRacesFailure.execute();
        } catch (HystrixRuntimeException hre) {
            assertEquals(RemoteServiceException.class, hre.getFallbackException().getClass());
        }

        verify(mockService).getTodayRaces();
        verifyNoMoreInteractions(mockService);
    }

    /**
     * 异步调用
     */
    @Test
    public void testAsynchronous() throws Exception {

        CommandGetTodaysRaces commandGetRaces = new CommandGetTodaysRaces(mockService);

        Future<List<RaceCourse>> future = commandGetRaces.queue();
        assertEquals(getRaceCourses(), future.get());

        verify(mockService).getTodayRaces();
        verifyNoMoreInteractions(mockService);
    }

    /**
     * 响应式编程
     */
    @Test
    public void testObservable() throws Exception {

        CommandGetTodaysRaces commandGetRaces = new CommandGetTodaysRaces(mockService);

        Observable<List<RaceCourse>> observable = commandGetRaces.observe();
        // blocking observable
        assertEquals(getRaceCourses(), observable.toBlocking().single());

        verify(mockService).getTodayRaces();
        verifyNoMoreInteractions(mockService);
    }

    /**
     * 使用缓存
     */
    @Test
    public void testWithCacheHits() {

        HystrixRequestContext context = HystrixRequestContext.initializeContext();

        try {
            CommandGetHorsesInRaceWithCaching commandFirst = new CommandGetHorsesInRaceWithCaching(mockService, RACE_1);
            CommandGetHorsesInRaceWithCaching commandSecond = new CommandGetHorsesInRaceWithCaching(mockService, RACE_1);

            commandFirst.execute();

            // this is the first time we've executed this command with
            // the value of "2" so it should not be from cache
            assertFalse(commandFirst.isResponseFromCache());

            verify(mockService).getHorsesInRace(RACE_1);
            verifyNoMoreInteractions(mockService);

            commandSecond.execute();

            // this is the second time we've executed this command with
            // the same value so it should return from cache
            assertTrue(commandSecond.isResponseFromCache());

        } finally {
            context.shutdown();
        }

        // start a new request context
        context = HystrixRequestContext.initializeContext();
        try {
            CommandGetHorsesInRaceWithCaching commandThree = new CommandGetHorsesInRaceWithCaching(mockService, RACE_1);
            commandThree.execute();
            // this is a new request context so this
            // should not come from cache
            assertFalse(commandThree.isResponseFromCache());

            // Flush the cache
            HystrixRequestCache.getInstance(GETTER_KEY, HystrixConcurrencyStrategyDefault.getInstance()).clear(RACE_1);

        } finally {
            context.shutdown();
        }
    }

    private List<RaceCourse> getRaceCourses() {
        RaceCourse course1 = new RaceCourse(RACE_1, "France");
        return Arrays.asList(course1);
    }

    private List<Horse> getHorsesAtFrance() {
        Horse horse1 = new Horse(HORSE_1, "White");
        Horse horse2 = new Horse(HORSE_2, "Black");
        return Arrays.asList(horse1, horse2);
    }
}

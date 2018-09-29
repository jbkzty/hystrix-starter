package hystrix.service;

import hystrix.domain.Horse;
import hystrix.domain.RaceCourse;

import java.util.List;

/**
 * @author spuerKun
 * @date 2018/9/23.
 */
public interface BettingService {

    /**
     * Get a list the names of all Race courses with races on today.
     *
     * @return List of race course names
     */
    List<RaceCourse> getTodayRaces();

    /**
     * Get a list of all Horses running in a particular race.
     *
     * @param raceCourseId
     * @return List of the names of all horses running in the specified race
     */
    List<Horse> getHorsesInRace(String raceCourseId);

    /**
     * Get current odds for a particular horse in a specific race today.
     *
     * @param raceCourseId Name of race course
     * @param horseId      Name of horse
     * @return Current odds as a string (e.g. "10/1")
     */
    String getOddsForHorse(String raceCourseId, String horseId);

}

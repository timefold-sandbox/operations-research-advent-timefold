///usr/bin/env jbang "$0" "$@" ; exit $?
/// This file requires Jbang to run it.
//DEPS ai.timefold.solver:timefold-solver-core:1.16.0

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import java.time.Duration;

import static java.lang.System.*;
import java.util.*;
import java.util.stream.IntStream;

public class Dec02Roadtrip {

    public static void main(String... args) {
        SolverFactory<RoadTripPlan> solverFactory = SolverFactory.create(
                new SolverConfig()
                        .withSolutionClass(RoadTripPlan.class)
                        .withEntityClasses(Traveler.class)
                        .withConstraintProviderClass(RoadTripConstraintProvider.class)
                        // Stop the solver if no better solution is found for 3 seconds.
                        .withTerminationConfig(new TerminationConfig()
                                .withUnimprovedSecondsSpentLimit(3L)));

        var solver = solverFactory.buildSolver();

        // Only setup visits 2-99. 1 and 100 are fixed start and ends locations.
        var visitList = IntStream.range(2, 100).mapToObj(Visit::new).toList();

        RoadTripPlan plan = new RoadTripPlan(new Traveler(), visitList);

        var result = solver.solve(plan);
        System.out.println("Score: " + result.getScore());
        List<Integer> route = result.getTraveler().getVisits().stream().map(Visit::id).toList();
        System.out.println("Route: " + route);
    }

    public static record Visit(int id) {
    }

    @PlanningEntity
    public static class Traveler {
        @PlanningListVariable(allowsUnassignedValues = true)
        private List<Visit> visits;

        public Traveler() {
            this.visits = new ArrayList<>();
        }

        public List<Visit> getVisits() {
            return visits;
        }

        public void setVisits(List<Visit> visits) {
            this.visits = visits;
        }
    }

     @PlanningSolution
     public static class RoadTripPlan {
        @PlanningEntityProperty
        private Traveler traveler;

        @ProblemFactCollectionProperty
        @ValueRangeProvider
        private List<Visit> visits;

        @PlanningScore
        private HardMediumSoftLongScore score;

        public RoadTripPlan() {
        }

        public RoadTripPlan(Traveler traveler, List<Visit> visits) {
            this.traveler = traveler;
            this.visits = visits;
        }

        public Traveler getTraveler() {
            return traveler;
        }

        public List<Visit> getVisits() {
            return visits;
        }

        public HardMediumSoftLongScore getScore() {
            return score;
        }
    }

    /**
     * Class containing the costs (distance and fuel) of all mappings
     */
    final static class CostMap {
        private Map<Integer, Map<Integer, Cost>> distanceMap = new HashMap<>();

        public CostMap() {
            distanceMap = createDistanceMap();
        }

        public long calculateTotalDrivingTime(Traveler traveler) {
            var visits = traveler.getVisits();
            if (visits.isEmpty()) {
                return 0;
            }

            long totalDrivingTime = 0;
            int previousLocation = 1; // Always start at 1, end at 100

            for (Visit visit : visits) {
                totalDrivingTime += this.distanceMap.get(previousLocation).get(visit.id()).distance();
                previousLocation = visit.id();
            }

            totalDrivingTime += this.distanceMap.get(previousLocation).get(100).distance();

            return totalDrivingTime;
        }

        public long calculateTotalFuelCosts(Traveler traveler) {
            var visits = traveler.getVisits();
            if (visits.isEmpty()) {
                return 0;
            }

            long totalFuelCost = 0;
            int previousLocation = 1;

            for (Visit visit : visits) {
                totalFuelCost += this.distanceMap.get(previousLocation).get(visit.id()).fuel();
                previousLocation = visit.id();
            }

            totalFuelCost += this.distanceMap.get(previousLocation).get(100).fuel();

            return totalFuelCost;
        }

        private Map<Integer, Map<Integer,Cost>> createDistanceMap() {
            String input = """
                 1 37 60 5\s
                 1 59 9 59\s
                 1 72 63 1\s
                 2 15 61 1\s
                 2 20 16 39\s
                 2 26 54 6\s
                 2 37 26 36\s
                 2 58 54 8\s
                 2 60 25 9\s
                 2 76 72 9\s
                 2 80 35 21\s
                 2 90 73 1\s
                 2 95 23 26\s
                 2 100 20 10\s
                 3 15 30 0\s
                 3 37 51 6\s
                 3 40 75 1\s
                 3 41 19 9\s
                 3 56 27 24\s
                 3 59 17 20\s
                 3 73 84 11\s
                 3 76 78 4\s
                 4 7 58 9\s
                 4 29 42 24\s
                 4 46 38 17\s
                 4 61 33 6\s
                 4 80 61 3\s
                 4 83 49 15\s
                 4 98 20 5\s
                 5 11 37 19\s
                 5 34 28 1\s
                 5 55 84 5\s
                 5 79 76 7\s
                 5 80 62 11\s
                 5 85 10 77\s
                 5 87 43 14\s
                 5 92 57 13\s
                 5 99 28 14\s
                 6 8 22 1\s
                 6 15 63 16\s
                 6 24 16 45\s
                 6 30 68 10\s
                 6 31 13 20\s
                 6 35 24 17\s
                 6 39 58 7\s
                 6 40 77 1\s
                 6 41 17 17\s
                 6 51 75 12\s
                 6 74 52 14\s
                 6 82 87 10\s
                 7 6 37 15\s
                 7 8 23 37\s
                 7 10 97 1\s
                 7 24 53 5\s
                 7 25 59 2\s
                 7 35 13 53\s
                 7 47 75 11\s
                 7 52 6 103\s
                 7 74 54 10\s
                 8 6 22 29\s
                 8 16 92 4\s
                 8 18 59 12\s
                 8 21 62 14\s
                 8 25 74 13\s
                 8 26 87 2\s
                 8 39 78 9\s
                 8 46 49 19\s
                 8 55 40 12\s
                 8 58 42 8\s
                 8 63 21 39\s
                 8 83 49 13\s
                 9 5 28 15\s
                 9 18 31 3\s
                 9 22 61 1\s
                 9 48 66 3\s
                 9 60 45 13\s
                 9 79 66 15\s
                 9 92 42 3\s
                 10 13 49 20\s
                 10 15 94 11\s
                 10 31 75 8\s
                 10 37 89 7\s
                 10 85 49 19\s
                 10 88 57 8\s
                 10 93 49 15\s
                 10 96 77 3\s
                 11 1 67 9\s
                 11 2 44 8\s
                 11 13 30 27\s
                 11 15 75 9\s
                 11 22 10 21\s
                 11 24 60 1\s
                 11 34 61 13\s
                 11 48 24 32\s
                 11 53 7 87\s
                 11 54 18 31\s
                 11 55 66 10\s
                 11 61 66 5\s
                 11 62 7 32\s
                 11 82 45 18\s
                 11 89 77 6\s
                 11 92 64 7\s
                 12 16 85 7\s
                 12 27 47 13\s
                 12 32 30 2\s
                 12 65 47 10\s
                 12 77 79 10\s
                 13 11 30 17\s
                 13 50 47 16\s
                 13 59 50 1\s
                 13 74 7 12\s
                 13 100 23 23\s
                 14 18 30 13\s
                 14 32 50 10\s
                 14 39 62 4\s
                 14 47 23 33\s
                 14 63 76 7\s
                 14 79 82 3\s
                 14 85 13 37\s
                 14 100 45 2\s
                 15 14 41 21\s
                 15 25 44 7\s
                 15 27 56 13\s
                 15 37 81 12\s
                 15 44 35 28\s
                 15 48 78 3\s
                 15 60 43 22\s
                 15 63 42 13\s
                 16 4 62 11\s
                 16 41 69 14\s
                 16 53 13 17\s
                 16 60 78 2\s
                 16 81 12 40\s
                 16 84 21 2\s
                 16 90 24 21\s
                 16 97 9 54\s
                 17 7 69 5\s
                 17 12 47 2\s
                 17 28 35 5\s
                 17 35 68 8\s
                 17 37 85 6\s
                 17 42 69 11\s
                 17 46 53 6\s
                 17 47 40 21\s
                 17 64 41 8\s
                 17 66 39 22\s
                 17 81 41 11\s
                 17 88 43 4\s
                 17 91 24 14\s
                 17 93 16 61\s
                 17 100 42 14\s
                 18 30 19 50\s
                 18 31 45 16\s
                 18 49 45 1\s
                 18 53 34 14\s
                 18 59 49 17\s
                 18 64 34 27\s
                 18 66 32 14\s
                 18 67 34 23\s
                 19 6 91 1\s
                 19 12 46 4\s
                 19 30 24 18\s
                 19 39 68 1\s
                 19 40 47 10\s
                 19 42 85 1\s
                 19 47 19 39\s
                 19 53 64 11\s
                 19 72 90 2\s
                 19 81 64 12\s
                 20 8 48 14\s
                 20 16 44 7\s
                 20 29 7 22\s
                 20 38 31 17\s
                 20 42 31 25\s
                 20 47 77 10\s
                 20 66 49 13\s
                 20 83 62 13\s
                 20 95 11 4\s
                 21 16 63 4\s
                 21 28 12 55\s
                 21 31 58 12\s
                 21 40 48 13\s
                 21 44 15 57\s
                 21 57 19 12\s
                 21 79 66 4\s
                 22 9 61 15\s
                 22 10 17 18\s
                 22 11 10 14\s
                 22 12 71 4\s
                 22 16 16 28\s
                 22 33 30 21\s
                 22 47 71 6\s
                 22 64 65 12\s
                 22 66 62 8\s
                 22 77 69 7\s
                 22 82 40 18\s
                 22 91 51 7\s
                 22 94 84 4\s
                 23 4 23 13\s
                 23 20 43 12\s
                 23 58 26 8\s
                 23 64 8 39\s
                 23 66 6 72\s
                 23 67 17 20\s
                 23 68 46 20\s
                 23 86 51 17\s
                 23 89 25 12\s
                 23 98 41 7\s
                 24 1 58 7\s
                 24 12 78 12\s
                 24 27 100 4\s
                 24 29 40 8\s
                 24 57 61 5\s
                 24 69 62 16\s
                 24 86 71 10\s
                 25 34 24 23\s
                 25 44 26 38\s
                 25 61 36 3\s
                 25 81 49 4\s
                 25 86 33 29\s
                 26 39 10 60\s
                 26 41 65 11\s
                 26 43 80 9\s
                 26 44 57 8\s
                 26 57 44 14\s
                 26 78 9 67\s
                 26 80 84 1\s
                 26 94 95 8\s
                 27 9 39 22\s
                 27 19 1 318\s
                 27 21 33 16\s
                 27 36 26 20\s
                 27 45 83 12\s
                 27 53 64 8\s
                 27 60 78 1\s
                 27 89 45 21\s
                 28 5 26 3\s
                 28 44 25 20\s
                 28 48 68 11\s
                 28 52 51 18\s
                 28 64 8 35\s
                 28 68 51 8\s
                 28 81 66 11\s
                 28 82 40 11\s
                 28 96 10 56\s
                 28 98 37 15\s
                 29 20 7 102\s
                 29 68 22 23\s
                 29 81 36 21\s
                 29 83 55 13\s
                 29 87 12 47\s
                 29 99 23 3\s
                 29 100 6 38\s
                 30 4 11 40\s
                 30 29 38 12\s
                 30 33 23 31\s
                 30 55 85 10\s
                 30 90 44 1\s
                 30 99 29 4\s
                 31 3 40 10\s
                 31 6 13 65\s
                 31 11 49 9\s
                 31 19 88 6\s
                 31 25 65 11\s
                 31 42 18 2\s
                 31 64 64 4\s
                 31 71 32 7\s
                 31 89 80 7\s
                 31 94 67 9\s
                 32 2 32 2\s
                 32 6 33 21\s
                 32 15 30 19\s
                 32 19 66 8\s
                 32 27 66 12\s
                 32 41 17 36\s
                 32 56 32 14\s
                 32 72 52 3\s
                 32 84 82 13\s
                 32 85 56 1\s
                 32 92 11 9\s
                 33 5 22 4\s
                 33 8 59 16\s
                 33 20 20 30\s
                 33 39 32 29\s
                 33 46 45 7\s
                 33 61 42 9\s
                 33 69 28 13\s
                 33 76 39 15\s
                 33 84 45 2\s
                 33 86 24 17\s
                 34 24 80 1\s
                 34 26 77 12\s
                 34 38 37 26\s
                 34 41 50 10\s
                 34 42 59 1\s
                 34 57 33 16\s
                 34 58 31 30\s
                 34 59 34 26\s
                 34 81 68 13\s
                 34 89 17 9\s
                 34 90 73 7\s
                 34 92 47 10\s
                 34 96 10 32\s
                 34 100 41 17\s
                 35 33 47 18\s
                 35 34 50 14\s
                 35 43 11 88\s
                 35 48 55 3\s
                 35 49 20 29\s
                 35 52 14 29\s
                 35 62 68 11\s
                 35 65 76 7\s
                 35 88 35 10\s
                 36 6 64 13\s
                 36 33 24 13\s
                 36 40 43 14\s
                 36 45 57 9\s
                 36 53 51 16\s
                 36 56 71 10\s
                 36 62 50 17\s
                 36 69 22 15\s
                 36 76 39 8\s
                 36 99 34 15\s
                 36 100 34 11\s
                 37 12 81 12\s
                 37 19 105 5\s
                 37 24 4 210\s
                 37 41 35 9\s
                 37 56 58 3\s
                 37 69 65 2\s
                 37 76 95 10\s
                 38 6 30 32\s
                 38 20 31 31\s
                 38 26 70 2\s
                 38 32 4 73\s
                 38 35 15 3\s
                 38 51 46 15\s
                 38 81 61 3\s
                 38 82 66 5\s
                 38 86 58 4\s
                 38 90 80 10\s
                 38 95 26 23\s
                 39 12 76 9\s
                 39 20 30 11\s
                 39 43 70 1\s
                 39 50 63 11\s
                 39 77 55 18\s
                 39 88 34 28\s
                 39 99 19 43\s
                 40 11 21 30\s
                 40 12 71 5\s
                 40 27 46 14\s
                 40 47 65 5\s
                 40 55 87 9\s
                 40 68 25 40\s
                 40 74 30 20\s
                 40 78 27 13\s
                 40 82 31 1\s
                 40 88 47 13\s
                 40 100 50 14\s
                 41 2 16 20\s
                 41 7 25 13\s
                 41 23 39 7\s
                 41 27 75 6\s
                 41 45 14 32\s
                 41 49 12 38\s
                 41 58 39 18\s
                 41 73 76 8\s
                 41 77 33 16\s
                 41 79 24 1\s
                 41 83 46 10\s
                 41 87 29 5\s
                 41 97 61 8\s
                 41 99 45 4\s
                 42 7 28 10\s
                 42 14 69 1\s
                 42 30 63 8\s
                 42 31 18 44\s
                 42 51 67 15\s
                 42 61 49 18\s
                 42 68 51 17\s
                 42 70 28 20\s
                 42 93 82 8\s
                 42 94 52 9\s
                 42 100 27 14\s
                 43 6 33 18\s
                 43 8 24 5\s
                 43 31 41 8\s
                 43 33 44 6\s
                 43 45 15 2\s
                 43 61 27 28\s
                 43 69 25 9\s
                 43 75 25 20\s
                 43 77 51 3\s
                 43 85 63 5\s
                 44 7 36 14\s
                 44 56 54 8\s
                 44 58 35 18\s
                 44 67 30 11\s
                 44 76 51 14\s
                 44 79 51 12\s
                 44 84 61 16\s
                 44 94 38 11\s
                 44 98 40 2\s
                 45 6 20 15\s
                 45 7 17 38\s
                 45 14 67 7\s
                 45 43 15 29\s
                 45 44 37 26\s
                 45 46 40 15\s
                 45 53 69 13\s
                 45 65 78 10\s
                 45 92 7 13\s
                 45 99 58 1\s
                 46 4 38 20\s
                 46 18 42 1\s
                 46 25 41 17\s
                 46 44 29 22\s
                 46 51 30 28\s
                 46 72 73 1\s
                 46 80 33 10\s
                 46 82 61 17\s
                 47 5 37 4\s
                 47 13 49 15\s
                 47 15 48 17\s
                 47 20 77 13\s
                 47 39 84 1\s
                 47 55 116 1\s
                 47 72 100 7\s
                 47 75 94 1\s
                 47 84 75 2\s
                 47 87 79 2\s
                 48 2 27 6\s
                 48 7 65 4\s
                 48 11 24 30\s
                 48 25 57 9\s
                 48 55 43 7\s
                 48 60 51 8\s
                 48 66 66 15\s
                 48 73 49 8\s
                 48 86 37 2\s
                 48 100 31 21\s
                 49 7 33 14\s
                 49 20 28 3\s
                 49 26 66 9\s
                 49 31 13 67\s
                 49 57 48 16\s
                 49 66 56 17\s
                 50 7 20 21\s
                 50 9 48 14\s
                 50 10 86 5\s
                 50 14 64 12\s
                 50 34 53 11\s
                 50 42 8 104\s
                 50 66 47 18\s
                 50 78 65 2\s
                 50 81 65 3\s
                 50 87 38 1\s
                 50 95 33 27\s
                 51 7 54 2\s
                 51 14 12 46\s
                 51 38 46 1\s
                 51 39 71 2\s
                 51 47 22 4\s
                 51 50 62 2\s
                 51 68 54 9\s
                 51 85 26 25\s
                 51 86 53 13\s
                 51 89 21 37\s
                 51 98 33 2\s
                 52 2 43 15\s
                 52 3 15 55\s
                 52 26 91 2\s
                 52 36 58 10\s
                 52 37 54 1\s
                 52 41 28 29\s
                 52 53 81 8\s
                 52 65 80 11\s
                 52 76 93 2\s
                 52 96 60 17\s
                 52 99 68 3\s
                 53 2 45 19\s
                 53 4 54 17\s
                 53 16 13 41\s
                 53 19 64 1\s
                 53 23 61 14\s
                 53 35 69 15\s
                 53 37 64 6\s
                 53 39 4 62\s
                 53 49 58 4\s
                 53 56 93 6\s
                 53 74 29 25\s
                 53 75 68 8\s
                 53 80 74 3\s
                 53 92 68 8\s
                 53 94 85 2\s
                 53 97 7 107\s
                 53 100 40 16\s
                 54 18 43 3\s
                 54 22 23 32\s
                 54 24 57 12\s
                 54 34 77 6\s
                 54 39 7 62\s
                 54 40 33 18\s
                 54 75 70 5\s
                 54 84 35 6\s
                 54 93 66 4\s
                 54 97 15 51\s
                 54 98 57 2\s
                 54 100 46 2\s
                 55 7 58 13\s
                 55 12 84 11\s
                 55 25 86 10\s
                 55 28 87 11\s
                 55 41 38 20\s
                 55 44 63 16\s
                 55 57 68 4\s
                 55 70 19 22\s
                 55 78 64 7\s
                 55 83 82 1\s
                 55 89 98 4\s
                 56 14 78 3\s
                 56 23 54 18\s
                 56 26 102 4\s
                 56 28 62 11\s
                 56 35 24 6\s
                 56 46 42 22\s
                 56 58 32 11\s
                 56 84 113 5\s
                 56 88 59 10\s
                 57 5 16 18\s
                 57 21 19 49\s
                 57 25 18 38\s
                 57 26 44 1\s
                 57 35 49 15\s
                 57 42 48 11\s
                 57 46 42 11\s
                 57 48 39 15\s
                 57 50 47 10\s
                 57 58 50 13\s
                 57 72 50 15\s
                 57 75 55 18\s
                 58 2 54 5\s
                 58 5 53 3\s
                 58 10 97 11\s
                 58 19 66 15\s
                 58 21 37 1\s
                 58 64 24 21\s
                 58 75 45 4\s
                 58 77 71 1\s
                 58 96 39 16\s
                 59 2 51 12\s
                 59 8 38 2\s
                 59 24 66 7\s
                 59 36 47 19\s
                 59 44 36 4\s
                 59 49 46 11\s
                 59 63 25 4\s
                 59 78 83 6\s
                 59 89 34 21\s
                 60 2 25 28\s
                 60 6 21 46\s
                 60 30 59 9\s
                 60 39 65 11\s
                 60 40 77 6\s
                 60 82 78 8\s
                 60 89 58 5\s
                 60 94 40 11\s
                 61 11 66 5\s
                 61 39 74 10\s
                 61 48 69 10\s
                 61 63 43 22\s
                 61 71 45 12\s
                 61 72 72 2\s
                 61 82 57 5\s
                 61 95 41 8\s
                 61 100 38 22\s
                 62 9 64 4\s
                 62 10 27 4\s
                 62 19 64 14\s
                 62 20 29 4\s
                 62 23 60 11\s
                 62 27 64 4\s
                 62 39 4 8\s
                 62 60 64 12\s
                 62 77 56 12\s
                 62 79 70 13\s
                 62 85 49 20\s
                 62 98 46 13\s
                 62 100 39 15\s
                 63 5 76 3\s
                 63 12 48 14\s
                 63 34 59 12\s
                 63 38 32 8\s
                 63 40 100 8\s
                 63 44 52 11\s
                 63 47 89 9\s
                 63 52 10 53\s
                 63 54 95 6\s
                 63 62 89 11\s
                 63 68 78 10\s
                 63 96 68 3\s
                 64 4 25 31\s
                 64 8 59 11\s
                 64 17 41 18\s
                 64 30 33 9\s
                 64 36 22 33\s
                 64 51 17 22\s
                 64 68 53 6\s
                 65 11 47 2\s
                 65 15 57 13\s
                 65 22 43 4\s
                 65 30 15 30\s
                 65 47 29 26\s
                 65 57 31 25\s
                 65 70 83 5\s
                 66 6 61 7\s
                 66 19 40 2\s
                 66 20 49 11\s
                 66 21 14 26\s
                 66 37 78 9\s
                 66 38 32 16\s
                 66 41 44 20\s
                 66 49 56 6\s
                 66 52 44 1\s
                 66 60 44 9\s
                 66 76 59 14\s
                 66 92 41 22\s
                 67 6 72 9\s
                 67 41 55 9\s
                 67 52 58 5\s
                 67 68 52 11\s
                 67 86 52 13\s
                 68 8 68 13\s
                 68 17 31 29\s
                 68 35 57 4\s
                 68 39 17 59\s
                 68 41 44 4\s
                 68 55 62 4\s
                 68 66 51 18\s
                 68 70 44 19\s
                 68 72 38 24\s
                 68 92 55 10\s
                 69 8 48 8\s
                 69 51 27 1\s
                 69 72 59 2\s
                 70 5 68 12\s
                 70 9 71 13\s
                 70 13 54 9\s
                 70 18 49 5\s
                 70 20 26 4\s
                 70 22 58 2\s
                 70 24 13 61\s
                 70 25 70 11\s
                 70 27 92 5\s
                 70 28 75 5\s
                 70 35 43 5\s
                 70 42 28 34\s
                 70 56 64 15\s
                 70 78 46 8\s
                 70 93 87 5\s
                 71 2 25 9\s
                 71 9 41 6\s
                 71 20 10 41\s
                 71 74 14 29\s
                 71 77 45 3\s
                 71 85 41 21\s
                 71 89 60 4\s
                 71 99 17 43\s
                 72 5 65 4\s
                 72 8 52 5\s
                 72 17 67 9\s
                 72 27 89 8\s
                 72 35 49 16\s
                 72 37 26 33\s
                 72 44 50 1\s
                 72 53 39 9\s
                 72 55 27 6\s
                 72 83 80 11\s
                 72 86 53 9\s
                 73 28 68 12\s
                 73 85 42 19\s
                 73 97 20 20\s
                 73 99 31 26\s
                 74 7 54 17\s
                 74 8 63 12\s
                 74 11 23 27\s
                 74 34 38 1\s
                 74 38 37 22\s
                 74 51 39 1\s
                 74 55 66 14\s
                 74 79 62 6\s
                 75 8 11 13\s
                 75 16 81 3\s
                 75 23 53 4\s
                 75 33 51 2\s
                 75 40 83 7\s
                 75 45 10 25\s
                 75 70 33 18\s
                 75 71 43 4\s
                 75 72 41 11\s
                 75 96 73 7\s
                 76 4 38 8\s
                 76 21 46 20\s
                 76 25 33 16\s
                 76 30 29 32\s
                 76 39 42 5\s
                 76 41 74 5\s
                 76 43 81 9\s
                 76 61 69 4\s
                 76 68 36 13\s
                 76 74 35 16\s
                 76 84 20 20\s
                 76 97 32 13\s
                 77 30 77 8\s
                 77 63 60 10\s
                 77 70 11 31\s
                 77 76 89 4\s
                 77 78 56 11\s
                 77 79 26 1\s
                 77 88 37 12\s
                 77 91 78 5\s
                 78 11 10 67\s
                 78 36 54 3\s
                 78 48 21 10\s
                 78 49 58 15\s
                 78 65 57 6\s
                 78 68 17 48\s
                 78 84 30 11\s
                 78 88 35 24\s
                 78 95 36 8\s
                 79 1 43 3\s
                 79 22 80 6\s
                 79 41 24 27\s
                 79 46 58 14\s
                 79 51 80 7\s
                 79 54 73 4\s
                 79 57 62 6\s
                 79 68 63 15\s
                 79 71 49 17\s
                 79 88 41 22\s
                 80 1 17 43\s
                 80 25 62 5\s
                 80 37 47 13\s
                 80 40 84 10\s
                 80 41 20 27\s
                 80 44 37 26\s
                 80 47 81 8\s
                 80 61 36 26\s
                 80 93 82 10\s
                 80 97 80 7\s
                 80 100 35 9\s
                 81 7 78 3\s
                 81 29 36 12\s
                 81 43 72 2\s
                 81 65 53 12\s
                 81 85 49 2\s
                 81 94 86 2\s
                 82 20 59 12\s
                 82 68 43 4\s
                 82 70 85 9\s
                 82 73 38 2\s
                 82 74 35 14\s
                 82 92 77 7\s
                 82 93 5 149\s
                 82 100 55 15\s
                 83 3 27 5\s
                 83 4 49 9\s
                 83 11 80 3\s
                 83 18 52 19\s
                 83 19 64 16\s
                 83 32 28 12\s
                 83 45 42 4\s
                 83 48 80 5\s
                 83 50 43 19\s
                 83 63 35 17\s
                 83 65 65 15\s
                 83 78 88 8\s
                 83 91 41 20\s
                 83 99 69 7\s
                 84 4 55 10\s
                 84 10 5 176\s
                 84 12 82 11\s
                 84 17 37 10\s
                 84 31 76 11\s
                 84 52 101 4\s
                 84 86 20 47\s
                 84 90 3 51\s
                 84 99 33 20\s
                 85 2 65 6\s
                 85 3 60 2\s
                 85 5 10 3\s
                 85 13 23 11\s
                 85 14 13 54\s
                 85 61 45 16\s
                 85 69 38 2\s
                 85 73 42 7\s
                 85 76 26 15\s
                 85 87 52 6\s
                 85 92 66 13\s
                 85 94 59 13\s
                 85 95 42 5\s
                 85 98 8 68\s
                 86 6 68 6\s
                 86 7 75 11\s
                 86 19 46 4\s
                 86 23 51 2\s
                 86 38 58 12\s
                 86 68 16 47\s
                 86 74 21 8\s
                 86 79 79 5\s
                 86 90 22 39\s
                 86 91 43 20\s
                 86 92 68 14\s
                 87 20 5 130\s
                 87 25 46 12\s
                 88 22 40 10\s
                 88 27 63 5\s
                 88 30 40 21\s
                 88 39 34 11\s
                 88 49 27 21\s
                 88 63 57 11\s
                 88 70 29 34\s
                 88 75 36 14\s
                 88 76 55 16\s
                 88 79 41 25\s
                 88 83 56 9\s
                 88 95 7 139\s
                 88 96 56 9\s
                 88 97 40 19\s
                 88 100 8 115\s
                 89 12 14 4\s
                 89 16 94 4\s
                 89 35 57 8\s
                 89 52 53 10\s
                 89 57 50 9\s
                 89 68 69 14\s
                 89 70 88 8\s
                 89 91 29 33\s
                 90 5 45 8\s
                 90 8 104 5\s
                 90 12 83 3\s
                 90 14 57 2\s
                 90 15 92 3\s
                 90 30 44 4\s
                 90 45 93 1\s
                 90 46 87 3\s
                 90 65 44 16\s
                 90 72 70 1\s
                 90 75 95 2\s
                 90 94 97 2\s
                 91 3 43 13\s
                 91 4 8 73\s
                 91 14 10 15\s
                 91 36 6 103\s
                 91 47 30 28\s
                 91 55 85 12\s
                 91 77 78 12\s
                 91 83 41 3\s
                 91 88 42 19\s
                 91 96 15 34\s
                 92 4 57 13\s
                 92 6 25 6\s
                 92 9 42 13\s
                 92 32 11 33\s
                 92 42 16 61\s
                 92 93 77 9\s
                 92 97 73 11\s
                 93 2 75 6\s
                 93 29 56 13\s
                 93 38 65 2\s
                 93 43 73 11\s
                 93 48 69 9\s
                 93 59 72 11\s
                 93 73 42 23\s
                 93 79 96 9\s
                 93 84 45 1\s
                 93 95 52 11\s
                 94 7 26 2\s
                 94 13 51 18\s
                 94 14 48 3\s
                 94 15 7 29\s
                 94 21 37 27\s
                 94 26 95 2\s
                 94 75 53 11\s
                 94 83 1 449\s
                 94 90 97 11\s
                 94 99 69 4\s
                 94 100 48 20\s
                 95 2 23 25\s
                 95 7 43 15\s
                 95 20 11 87\s
                 95 27 56 10\s
                 95 62 33 28\s
                 95 63 57 2\s
                 95 67 45 1\s
                 95 74 16 17\s
                 95 80 41 16\s
                 95 82 51 4\s
                 95 88 7 72\s
                 95 90 56 12\s
                 96 2 68 14\s
                 96 4 19 25\s
                 96 5 29 23\s
                 96 14 16 42\s
                 96 22 65 5\s
                 96 26 81 2\s
                 96 31 77 8\s
                 96 35 60 5\s
                 96 37 93 9\s
                 96 50 62 10\s
                 96 52 60 6\s
                 96 66 16 38\s
                 96 68 58 13\s
                 96 74 43 7\s
                 96 89 17 13\s
                 96 95 50 5\s
                 96 100 49 14\s
                 97 18 36 28\s
                 97 26 10 57\s
                 97 38 64 6\s
                 97 43 75 1\s
                 97 44 49 13\s
                 97 60 71 1\s
                 97 67 66 2\s
                 97 96 71 5\s
                 98 4 20 27\s
                 98 5 15 53\s
                 98 34 39 22\s
                 98 36 23 35\s
                 98 39 50 15\s
                 98 68 38 3\s
                 98 72 75 12\s
                 98 86 28 34\s
                 99 20 23 13\s
                 99 27 50 3\s
                 99 39 19 41\s
                 99 43 57 3\s
                 99 66 49 21\s
                 99 67 50 6\s
                 99 72 41 11\s
                 99 77 56 17\s
                 99 78 20 27\s
                 99 86 14 51\s
                 99 93 43 3\s
                 100 3 28 32\s
                 100 9 37 18\s
                 100 61 38 22\s
                 100 64 39 2\s
                 100 77 41 9\s
                 100 79 41 2\s
                 100 83 49 1\s
                 100 88 8 57\s
                 100 91 37 10\s
                 100 99 27 32
                 """;

            return CostMatrixCalculator.calculate(input);
        }
    }

    public static record Cost(int distance, int fuel) {}

    public static class RoadTripConstraintProvider implements ConstraintProvider {
        private final CostMap costMap = new CostMap();

        @Override
        public Constraint[] defineConstraints(ConstraintFactory factory) {
            return new Constraint[]{
                    minimizeTravelTime(factory),
                    doNotExceedTotalFuelCost(factory),
                    maximizeVisitedLocations(factory)
            };
        }

        /**
         * Creates a constraint which will reduce the SOFT score by 1 for each second driven by a Santa.
         */
        Constraint minimizeTravelTime(ConstraintFactory factory) {
            return factory.forEachIncludingUnassigned(Traveler.class)
                    .penalizeLong(HardMediumSoftLongScore.ONE_SOFT, costMap::calculateTotalDrivingTime)
                    .asConstraint("minimizeTravelTime");
        }

        /**
         * Creates a constraint which will reduce the HARD score by 1 for each fuel cost above the budget.
         */
        Constraint doNotExceedTotalFuelCost(ConstraintFactory factory) {
            return factory.forEach(Traveler.class)
                    .penalizeLong(HardMediumSoftLongScore.ONE_HARD, t -> Math.max(0,costMap.calculateTotalFuelCosts(t) - 73)) //73 is out budget
                    .asConstraint("doNotExceedFuelBudget");
        }

        /**
         * Creates a constraint which will increase the MEDIUM score by 1 for each location visited.
         */
        Constraint maximizeVisitedLocations(ConstraintFactory factory) {
            return factory.forEachIncludingUnassigned(Traveler.class)
                    .reward(HardMediumSoftLongScore.ONE_MEDIUM, t -> t.getVisits().size())
                    .asConstraint("more visits is better");
        }
    }

    public static class CostMatrixCalculator {
        private static final int NUM_LOCATIONS = 100;
        private static final int MISSING_FIELD_INDICATOR = Integer.MAX_VALUE;

        public static Map<Integer, Map<Integer, Cost>> calculate(String input) {
            System.out.println("Completing Distance Graph");
            int[][] distanceGraph = completeGraph(input, 2);// distances;
            System.out.println("Completing Fuel Graph");
            int[][] fuelGraph = completeGraph(input, 3);// fuel;

            System.out.println("Combining graphs to cost matrix");
            // Convert to nested HashMap
            Map<Integer, Map<Integer, Cost>> costMap = new HashMap<>();
            for (int i = 0; i < NUM_LOCATIONS; i++) {
                Map<Integer, Cost> innerMap = new HashMap<>();
                for (int j = 0; j < NUM_LOCATIONS; j++) {
                    int distance = 0;
                    if (distanceGraph[i][j] != MISSING_FIELD_INDICATOR) {
                        distance = distanceGraph[i][j];
                    }
                    int fuel = 0;
                    if (fuelGraph[i][j] != MISSING_FIELD_INDICATOR) {
                        fuel = fuelGraph[i][j];
                    }

                    innerMap.put(j + 1, new Cost(distance, fuel));  //Convert back to 1-based indexing
                }
                costMap.put(i + 1, innerMap);  // Convert back to 1-based indexing
            }

            return costMap;
        }

        /**
         * Complete a graph using a partially defined matrix using a String based definition.
         * Each line in the string should be structured as: {start} {end} {value} {value}.
         * It is assumed these inputs are undirected, so start -> end would be the same as end ->start
         *
         * @param input the input string.
         * @param valueIndex the index in the split string which contains the value we want to use in the matrix
         * @return a fully initialized matrix with the optimal paths between each entry.
         */
        public static int[][] completeGraph(String input, int valueIndex) {
            int[][] graph = new int[NUM_LOCATIONS][NUM_LOCATIONS];
            for (int[] row : graph) {
                Arrays.fill(row, MISSING_FIELD_INDICATOR);
            }

            // populate known distances
            for(String line : input.split(System.lineSeparator())) {
                String[] parts = line.trim().split("\\s+");
                int start = Integer.parseInt(parts[0]) - 1;
                int end = Integer.parseInt(parts[1]) - 1;
                int value = Integer.parseInt(parts[valueIndex]);
                // Assuming undirected graph, so mapping both ways
                graph[start][end] = value;
                graph[end][start] = value;
            }

            // Locations pointing to themselves should always have cost = 0
            for (int i = 0; i < NUM_LOCATIONS; i++) {
                graph[i][i] = 0;
            }

            // Dijkstra's algorithm to find shortest paths
            for (int source = 0; source < NUM_LOCATIONS; source++) {
                PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
                int[] distances = new int[NUM_LOCATIONS];
                Arrays.fill(distances, MISSING_FIELD_INDICATOR);
                distances[source] = 0;
                pq.offer(new int[]{source, 0});

                while (!pq.isEmpty()) {
                    int[] current = pq.poll();
                    int currentNode = current[0];
                    int currentDist = current[1];

                    // If we've found a longer path, skip
                    if (currentDist > distances[currentNode]) continue;

                    for (int neighbor = 0; neighbor < NUM_LOCATIONS; neighbor++) {
                        // No direct connection, so skip this edge
                        if (graph[currentNode][neighbor] == MISSING_FIELD_INDICATOR) continue;

                        int newDistance = currentDist + graph[currentNode][neighbor];
                        // Update if we've found a shorter path
                        if (newDistance < distances[neighbor]) {
                            distances[neighbor] = newDistance;
                            graph[source][neighbor] = newDistance;
                            pq.offer(new int[]{neighbor, newDistance});
                        }
                    }
                }
            }
            return graph;
        }
    }
}

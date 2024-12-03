///usr/bin/env jbang "$0" "$@" ; exit $?
/// This file requires Jbang to run it.
//DEPS ai.timefold.solver:timefold-solver-core:1.16.0

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.IntStream;

import static ai.timefold.solver.core.api.score.stream.Joiners.equal;

public class Dec03TaskAssigning {

    public static void main(String... args) {
        SolverFactory<TaskAssigningPlan> solverFactory = SolverFactory.create(
                new SolverConfig()
                        .withSolutionClass(TaskAssigningPlan.class)
                        .withEntityClasses(Task.class)
                        .withConstraintProviderClass(TaskAssigningConstraintProvider.class)
                        // Stop the solver if no better solution is found for 3 seconds.
                        .withTerminationConfig(new TerminationConfig()
                                .withUnimprovedSecondsSpentLimit(3L)));

        var solver = solverFactory.buildSolver();
        TaskAssigningPlan plan = createPlanFromInput();

        TaskAssigningPlan solution = solver.solve(plan);
        System.out.println("Score:" + solution.score);

        StringBuilder assignments = new StringBuilder("Assignments: ").append(System.lineSeparator());
        solution.tasks.forEach(t -> assignments
                .append(t.id)
                .append("/").append(t.employee.id())
                .append("$").append(t.getCost())
                .append("   "));
        System.out.println(assignments);
    }

    public static TaskAssigningPlan createPlanFromInput() {
        int inputSize = 100;
        String[] split = input().split(System.lineSeparator());
        List<Task> tasks = new ArrayList<>(inputSize);
        for (int i = 0; i < split.length; i++) {
            String singleLine = split[i];
            Map<Integer, Integer> costMap = new HashMap<>();
            String[] singleCosts = singleLine.trim().split(" ");
            for (int j = 0; j < singleCosts.length; j++) {
                String cost = singleCosts[j];
                costMap.put(j, Integer.parseInt(cost));
            }
            tasks.add(new Task(i, costMap));
        }

        List<Employee> employees = IntStream.range(0, inputSize).mapToObj(Employee::new).toList();

        return new TaskAssigningPlan(tasks, employees);
    }

    public static record Employee(int id) {
    }

    @PlanningEntity
    public static class Task {
        @PlanningId
        private Integer id;
        @PlanningVariable
        private Employee employee;

        private Map<Integer, Integer> costPerEmployee = new HashMap<>();

        public Task() {
        }

        public Task(int id, Map<Integer, Integer> costMap) {
            this.id = id;
            this.costPerEmployee = costMap;
        }

        public int getCost() {
            if (employee == null) {
                return 0;
            } else {
                return costPerEmployee.get(employee.id());
            }
        }

        public Employee getEmployee() {
            return employee;
        }
    }

    @PlanningSolution
    public static class TaskAssigningPlan {
        @PlanningEntityCollectionProperty
        private List<Task> tasks;

        @ProblemFactCollectionProperty
        @ValueRangeProvider
        private List<Employee> employees;


        public TaskAssigningPlan(List<Task> tasks, List<Employee> employees) {
            this.tasks = tasks;
            this.employees = employees;
        }

        @PlanningScore
        private HardSoftScore score;

        public TaskAssigningPlan() {
        }

    }

    public static class TaskAssigningConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    minimizeCost(constraintFactory),
                    noUnassignedTasks(constraintFactory),
                    employeesShouldOnlyHaveASingleTask(constraintFactory)
            };
        }

        protected Constraint noUnassignedTasks(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(Task.class)
                    .filter(t -> t.employee == null)
                    .penalize(HardSoftScore.ONE_HARD)
                    .asConstraint("No unassigned tasks");
        }

        protected Constraint employeesShouldOnlyHaveASingleTask(ConstraintFactory constraintFactory) {
            return constraintFactory.forEachUniquePair(Task.class, equal(Task::getEmployee))
                    .penalize(HardSoftScore.ONE_HARD)
                    .asConstraint("No double assigment for employees");
        }

        protected Constraint minimizeCost(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(Task.class)
                    .penalize(HardSoftScore.ONE_SOFT, Task::getCost)
                    .asConstraint("Minimize Cost");
        }

    }

    public static String input() {
        return """
                52 89 40 77 89 14 9 77 92 77 52 53 96 96 92 76 33 81 92 84 36 81 47 55 87 35 31 71 6 20 8 10 75 54 50 12 38 5 20 93 70 63 95 96 61 53 35 25 60 64 42 46 68 20 61 53 61 28 86 16 51 32 39 19 28 82 31 99 2 30 7 23 53 12 55 60 75 6 8 9 42 45 80 42 77 42 45 13 57 62 66 73 21 94 36 10 100 29 46 69
                 20 17 80 96 14 43 4 69 5 29 59 18 61 22 82 35 36 8 7 22 15 83 27 33 27 7 7 64 83 32 70 53 92 79 33 46 60 34 43 58 6 9 54 66 66 95 62 95 91 63 74 34 88 40 22 59 43 86 34 31 58 37 35 95 53 63 39 56 27 30 21 48 99 53 5 34 45 32 64 82 9 53 31 38 82 15 51 28 17 28 15 49 89 34 9 57 31 76 88 92
                 17 34 50 77 71 31 74 26 100 20 96 31 96 15 86 90 50 9 96 97 6 88 74 5 41 100 33 36 96 2 32 100 4 50 19 76 100 59 48 68 36 61 6 39 17 70 55 41 53 96 56 47 55 18 45 9 84 76 98 67 81 10 49 92 91 96 96 5 28 28 76 23 49 63 8 56 80 4 58 16 70 46 75 52 6 28 26 3 20 28 63 50 95 52 42 98 32 76 39 31
                 55 63 85 35 31 46 51 70 21 24 53 96 76 100 70 23 29 97 95 91 12 50 27 53 11 50 36 99 3 24 81 69 72 25 61 100 77 69 38 2 48 22 74 60 61 23 51 89 5 20 14 2 19 70 36 38 44 68 17 40 38 97 37 63 27 51 3 65 4 70 82 98 97 15 96 5 28 4 31 38 24 88 78 71 48 62 50 75 7 76 75 13 20 58 80 30 17 36 47 49
                 70 13 14 26 49 80 24 48 31 42 46 98 28 20 65 57 98 87 36 65 49 4 80 23 29 91 34 41 23 52 59 97 15 56 32 64 32 78 90 6 80 78 52 88 79 20 26 74 34 39 26 60 83 82 67 21 51 7 43 69 67 3 2 57 91 35 98 9 21 60 88 40 6 96 34 2 65 80 66 68 38 5 5 87 89 76 51 30 39 96 57 81 15 24 63 78 17 50 85 67
                 99 72 15 91 70 94 95 50 35 30 90 96 6 44 51 76 100 4 52 26 40 60 89 78 94 16 67 50 31 80 54 62 23 45 33 83 49 9 58 65 68 5 14 54 14 76 25 81 79 40 54 17 69 90 91 95 66 25 47 52 55 4 95 28 53 69 64 7 64 16 44 38 78 73 50 20 26 86 91 14 69 25 71 79 56 23 5 45 10 49 89 40 68 32 100 24 77 68 92 74
                 77 83 59 20 49 97 66 21 82 42 4 67 32 44 40 89 34 56 23 21 55 99 51 58 81 63 88 30 14 49 90 62 34 98 73 73 99 24 8 39 14 29 18 54 68 6 35 15 93 29 38 45 41 86 45 69 26 46 72 62 99 53 67 15 78 53 90 69 86 27 97 29 57 92 59 35 20 8 97 91 99 62 88 67 3 54 96 26 52 56 75 75 57 89 39 40 16 91 21 45
                 37 78 99 75 77 2 39 53 54 90 89 77 57 69 97 27 5 52 69 54 25 89 83 75 44 57 13 68 33 30 41 5 16 70 54 66 96 24 2 72 35 81 68 79 36 27 16 49 90 40 63 71 41 71 57 34 95 60 94 99 86 30 58 75 26 60 90 100 41 12 37 13 72 27 77 60 60 13 47 7 81 40 82 30 41 73 76 96 34 10 24 17 31 48 60 19 38 73 99 18
                 79 6 19 90 57 16 64 15 50 2 78 27 92 25 69 86 39 50 38 99 72 13 15 86 27 36 67 83 55 27 83 55 81 52 2 58 74 86 9 34 20 32 2 51 93 43 20 97 58 87 91 88 100 56 33 23 87 18 75 21 13 7 72 35 84 35 59 47 9 29 83 37 38 34 35 46 96 83 50 42 65 68 31 34 45 72 76 41 94 19 3 46 7 71 17 69 55 16 62 14
                 23 75 22 81 44 22 28 67 75 81 58 68 29 42 20 51 22 99 93 80 66 69 17 49 60 12 40 5 76 99 16 79 23 73 20 53 21 49 80 74 26 65 32 73 70 23 70 22 70 29 10 4 87 66 71 64 14 28 72 80 28 2 23 81 8 56 48 28 30 28 36 94 49 34 44 5 29 93 63 17 60 42 76 76 94 2 34 83 61 64 46 22 83 39 12 22 100 98 86 15
                 95 17 89 42 71 83 12 85 40 20 17 22 96 34 45 22 16 50 2 27 6 73 72 55 11 28 95 15 60 62 82 73 57 65 76 88 100 65 22 72 9 89 94 59 95 29 55 49 79 32 23 61 57 23 70 55 79 4 83 39 77 26 25 6 71 28 65 80 9 88 37 38 6 55 68 18 48 84 49 61 67 22 11 72 58 37 87 10 84 44 36 93 59 15 42 29 77 42 40 14
                 53 56 60 77 55 98 16 84 66 84 98 41 34 42 46 89 47 85 73 22 63 18 64 49 83 8 21 20 17 65 84 57 27 47 79 47 47 4 70 69 11 21 64 9 79 7 83 55 46 100 36 28 36 38 75 6 49 40 38 42 13 84 52 30 43 32 78 20 44 48 15 43 4 62 61 61 86 30 49 58 90 43 57 53 41 37 67 50 3 96 23 45 5 80 44 22 51 28 16 79
                 100 5 22 64 22 16 53 83 5 7 37 6 87 2 88 82 87 48 57 16 48 86 34 38 45 60 69 81 91 94 79 30 95 59 31 21 72 78 70 39 5 50 90 56 5 35 11 67 8 10 72 45 40 16 55 47 50 60 9 38 71 71 5 43 62 45 87 65 55 68 35 39 80 97 95 97 98 44 69 54 24 74 68 48 61 40 17 44 11 97 74 87 83 47 72 19 34 82 7 50
                 3 58 22 83 14 72 55 82 66 100 70 39 23 39 8 70 80 19 32 51 59 6 56 24 59 76 64 85 94 46 4 68 10 90 28 79 95 24 64 10 39 94 60 53 30 87 73 58 94 43 58 27 94 14 75 26 39 28 4 20 37 32 50 100 34 80 66 40 9 19 81 62 7 37 65 68 100 43 34 22 53 64 75 23 69 5 24 92 99 77 50 90 33 50 90 49 95 9 85 57
                 62 2 93 90 67 92 4 37 15 42 50 11 65 9 83 23 24 32 3 29 88 37 81 37 100 24 94 90 44 11 99 49 83 21 79 56 19 52 72 50 81 14 30 78 90 77 28 52 92 100 55 89 13 23 31 67 93 7 58 15 22 44 44 87 79 13 60 19 81 50 2 31 48 83 24 56 12 39 33 71 57 11 99 66 47 65 7 87 85 10 54 79 94 51 54 2 38 61 55 80
                 50 16 74 46 20 79 74 30 92 16 69 36 62 69 47 32 79 55 71 52 73 53 17 54 82 13 56 53 6 6 28 73 78 62 25 50 86 61 99 8 12 44 29 46 85 61 27 35 90 27 99 47 79 13 87 74 60 80 88 96 6 26 28 14 5 20 4 53 59 62 32 5 88 42 32 59 71 90 96 45 64 89 4 97 63 6 87 92 47 14 35 10 26 89 29 90 96 6 11 16
                 9 40 96 14 87 53 42 90 18 76 54 49 30 84 26 96 49 63 53 31 73 91 80 2 63 51 57 18 44 29 2 73 19 45 35 92 100 85 15 79 21 23 45 4 40 94 9 70 48 42 6 39 41 23 90 98 19 36 28 32 69 43 66 79 42 6 81 37 24 37 55 45 79 48 58 30 82 81 27 63 99 33 70 50 54 39 10 7 59 69 60 45 84 32 70 84 68 23 70 73
                 3 12 100 7 58 74 27 70 17 69 94 47 15 89 38 41 41 81 75 48 27 78 100 45 70 34 73 37 96 81 6 32 75 22 28 85 4 92 17 90 53 68 45 68 28 73 76 86 53 76 58 81 21 59 35 54 97 6 21 10 16 72 61 43 84 85 69 87 24 76 15 33 59 27 73 6 58 7 83 31 55 92 20 86 23 5 97 30 70 30 90 56 94 35 36 18 81 6 63 51
                 25 74 10 27 3 36 55 72 78 48 87 42 85 100 91 38 84 94 78 27 12 57 31 88 80 7 50 39 13 44 13 86 96 57 69 44 19 65 36 25 88 73 89 82 14 9 74 45 78 58 21 2 78 86 53 89 3 91 88 54 80 78 73 77 71 30 12 8 24 41 86 18 2 73 57 71 67 94 56 79 2 89 15 76 9 87 98 43 82 38 86 66 96 57 53 69 30 14 78 58
                 50 59 34 73 34 70 55 58 55 26 51 68 36 37 93 51 99 62 80 97 59 39 65 24 14 34 42 31 63 86 90 61 19 40 56 78 7 45 33 84 57 66 63 67 68 50 12 66 24 41 89 52 15 37 9 23 43 98 55 87 3 87 12 73 45 26 56 12 49 72 44 62 14 74 61 19 71 91 33 16 76 26 74 70 11 47 86 99 48 91 94 40 97 90 2 62 72 11 94 97
                 18 83 20 42 87 89 84 70 36 18 3 19 48 63 91 21 31 96 27 30 28 11 41 54 58 34 63 68 48 14 6 82 18 28 89 91 8 10 57 87 67 19 20 35 45 61 25 44 37 71 24 29 80 9 57 28 50 26 88 27 24 90 72 39 33 62 36 64 5 52 37 27 33 23 8 34 25 49 72 47 23 86 7 52 49 72 11 34 50 98 96 39 89 80 77 4 64 46 9 85
                 22 10 72 99 77 66 71 19 81 68 12 22 85 62 37 2 52 100 26 43 62 42 73 93 45 100 27 28 76 49 71 24 9 91 81 100 83 28 58 98 51 6 26 94 100 20 59 28 94 64 49 20 83 87 94 41 2 68 37 69 52 83 7 78 2 57 60 23 99 26 12 12 54 69 8 94 3 84 3 36 62 57 33 96 38 71 59 18 15 74 96 98 56 37 50 55 85 38 2 40
                 23 78 17 4 75 98 29 61 77 12 15 94 3 37 47 21 59 43 4 26 51 84 97 41 98 30 87 53 34 41 38 73 39 33 45 54 69 15 80 61 61 19 55 46 79 56 8 87 36 56 24 58 90 89 39 38 19 81 84 49 44 57 4 30 20 51 94 55 100 12 89 51 46 37 17 14 23 78 18 38 17 13 81 80 56 85 97 42 70 90 65 85 38 41 22 23 79 66 24 46
                 23 84 95 47 95 75 15 22 51 85 45 42 65 72 16 63 84 97 68 31 9 35 39 57 59 92 3 58 91 98 80 89 80 97 6 41 62 95 99 99 80 77 6 52 81 29 42 64 19 48 78 54 98 76 68 2 49 24 72 10 53 42 26 12 51 94 19 25 27 7 28 6 19 54 72 41 74 49 52 65 43 51 73 5 19 87 85 92 3 8 13 64 24 3 57 86 20 37 15 54
                 25 68 74 41 38 94 7 6 99 74 4 52 44 58 64 12 80 86 52 28 11 32 30 43 5 16 20 66 79 99 21 33 100 15 91 68 31 15 8 56 46 92 13 63 11 34 55 73 49 93 65 31 83 24 38 3 88 17 22 82 74 86 45 27 56 82 13 33 55 27 82 80 89 62 35 68 64 80 35 97 82 92 7 40 57 78 60 10 62 79 39 73 70 98 78 99 13 63 97 83
                 51 76 35 79 94 13 42 17 16 89 46 85 63 15 44 12 84 66 61 95 64 19 14 85 53 37 71 80 83 65 32 97 53 74 76 86 83 2 99 27 68 44 11 34 53 30 67 16 4 37 87 84 8 58 54 12 74 40 77 57 43 88 60 5 73 21 59 56 54 41 37 30 39 51 43 65 43 54 77 39 97 6 37 96 52 56 28 88 13 67 66 91 21 13 77 15 76 8 25 39
                 66 79 8 70 23 21 22 83 22 75 20 85 30 33 42 35 40 8 45 40 81 21 14 66 84 8 52 44 33 24 78 21 74 58 52 44 87 73 34 60 45 87 61 83 64 95 79 41 91 95 43 80 61 99 52 58 47 45 7 46 55 35 77 47 25 25 54 9 96 3 93 41 12 40 13 85 70 37 95 30 49 17 68 60 68 99 98 34 72 34 37 44 19 67 47 23 13 13 90 24
                 72 57 24 85 56 5 98 81 9 21 54 7 87 69 41 66 55 50 54 76 90 67 76 60 64 67 11 87 87 87 75 59 86 82 19 28 38 50 67 5 2 56 5 49 79 100 32 78 47 91 63 34 22 81 7 56 85 59 41 65 29 49 4 46 84 81 64 68 48 40 53 40 79 33 68 74 19 25 24 72 3 87 56 3 7 32 20 61 91 87 40 94 69 11 86 81 42 33 40 88
                 19 34 80 40 26 25 48 84 2 100 68 73 18 80 54 26 49 79 14 17 65 48 23 33 79 27 84 89 62 33 81 92 38 13 12 34 82 97 57 80 63 73 43 66 57 84 48 30 59 97 40 12 92 15 40 40 14 100 33 21 89 86 22 81 80 9 42 45 62 97 67 55 79 82 2 53 16 18 44 76 12 82 100 25 81 42 7 30 8 44 67 23 74 33 100 44 37 58 41 9
                 21 32 79 73 14 34 17 67 23 73 83 53 4 90 83 64 60 73 58 14 38 5 36 42 56 13 36 81 61 63 37 84 31 40 11 10 37 33 22 29 64 30 30 79 33 79 43 14 59 23 24 5 39 62 21 93 89 70 78 56 13 67 49 82 29 48 7 77 94 86 49 63 52 99 29 45 66 66 32 80 79 58 25 10 75 79 38 72 95 99 30 67 20 46 59 80 43 22 69 33
                 32 62 56 66 18 20 36 73 35 6 75 34 37 65 87 90 74 27 48 18 51 27 29 54 41 17 40 47 14 96 10 46 84 18 56 20 43 76 18 17 23 68 85 63 94 4 78 6 18 66 51 36 11 33 13 22 40 51 16 67 84 8 18 60 18 95 39 61 20 74 19 65 71 52 30 22 79 53 60 29 84 95 82 66 56 11 48 99 64 12 77 67 88 5 53 19 31 70 86 99
                 45 40 5 96 84 88 41 81 71 46 71 45 61 64 2 99 83 73 29 23 36 99 40 41 5 50 57 75 14 75 85 93 47 67 68 45 53 77 68 82 90 94 16 13 100 52 79 54 23 82 48 38 45 15 43 82 11 20 83 77 68 85 39 69 48 2 15 95 74 29 81 92 54 96 89 22 46 4 76 100 91 38 36 41 58 30 99 13 6 79 51 26 78 61 62 97 58 62 10 12
                 85 19 16 59 25 41 7 10 10 39 20 33 73 84 77 37 25 84 90 12 22 54 19 11 42 61 88 20 95 5 100 79 9 62 60 72 34 100 68 63 5 18 73 96 39 78 84 32 100 82 3 56 24 95 71 27 68 21 57 23 49 20 9 88 95 98 97 18 98 4 30 98 47 75 3 71 98 73 55 42 75 14 11 49 79 22 41 62 20 89 6 74 71 77 90 34 82 90 81 43
                 8 12 6 9 2 17 41 96 80 61 91 36 6 82 11 62 23 63 34 67 18 36 53 97 97 99 71 46 61 40 68 16 4 69 87 99 92 79 41 54 45 44 94 9 60 57 88 68 97 83 85 94 12 13 79 8 8 85 46 43 44 10 84 92 40 55 79 94 75 59 32 3 40 46 25 67 70 27 46 87 79 100 38 20 96 94 99 92 14 63 50 20 47 73 50 80 20 16 59 13
                 58 95 61 62 77 65 34 59 86 76 61 24 84 87 30 42 92 66 36 53 82 90 9 50 78 65 33 8 25 92 59 36 87 29 44 49 45 65 73 13 50 72 99 10 65 70 13 50 85 92 82 86 91 83 43 31 31 52 65 88 22 33 63 41 87 13 95 66 60 4 61 19 62 68 75 50 78 10 85 74 17 31 63 25 18 100 59 35 88 16 91 96 51 94 26 12 60 3 70 39
                 43 3 31 25 69 18 42 72 43 74 51 16 41 30 94 27 56 45 99 7 36 56 69 36 100 36 19 30 44 91 83 12 32 56 43 66 52 5 50 66 82 100 87 46 36 75 31 21 78 27 66 76 78 94 14 15 61 100 83 41 24 28 43 84 29 75 71 18 23 88 31 41 45 45 51 86 67 11 13 96 16 48 20 99 64 8 86 21 66 82 18 97 8 83 64 12 77 77 54 74
                 32 64 36 71 8 22 85 62 21 11 11 48 77 37 40 30 28 64 29 94 13 90 76 74 56 93 83 46 71 9 24 61 82 29 20 53 66 33 61 38 80 37 62 74 8 15 62 10 23 80 43 14 66 92 87 67 68 57 73 34 3 11 2 60 47 72 68 53 4 87 32 27 75 34 100 26 67 8 84 93 79 86 19 66 45 52 2 65 30 4 44 68 35 21 58 77 5 78 68 69
                 47 6 13 71 77 13 91 3 53 92 42 98 58 11 100 38 95 59 52 98 17 21 44 85 12 16 55 24 67 34 32 83 99 13 74 9 59 77 45 8 47 83 52 76 17 74 7 89 37 41 31 63 54 50 58 21 5 95 52 33 35 64 52 24 64 32 65 11 8 36 45 47 3 59 35 17 84 5 90 45 7 71 3 23 39 43 96 9 79 99 76 96 41 70 19 21 41 49 46 95
                 3 57 55 87 58 26 94 72 44 56 42 75 70 58 17 88 17 85 84 39 13 69 78 36 15 85 68 75 16 81 48 20 40 3 34 61 86 26 80 18 18 53 48 87 27 30 61 71 13 79 41 64 26 44 42 88 52 51 84 14 25 27 53 48 27 19 33 22 81 72 15 27 69 38 81 14 91 6 16 18 30 95 56 58 62 3 76 32 87 96 46 59 9 8 94 99 27 97 4 54
                 66 42 86 82 5 41 48 100 64 58 44 14 95 90 68 23 91 36 73 28 79 37 13 97 40 99 69 63 62 91 88 35 45 86 23 10 85 61 73 3 76 62 96 68 71 76 25 53 50 66 34 95 2 91 60 40 45 50 86 4 79 2 13 46 81 99 27 36 51 57 35 74 25 29 30 82 20 70 51 50 12 98 68 10 45 7 65 99 99 85 22 28 66 28 68 38 15 14 86 80
                 47 62 43 68 87 43 61 76 29 99 7 81 94 16 62 27 21 96 31 59 11 15 96 43 32 80 96 43 2 39 50 66 59 3 60 30 20 72 24 12 90 68 39 100 93 15 21 39 77 99 52 41 82 8 82 77 41 57 68 42 99 39 83 63 57 37 76 96 40 74 59 80 44 93 32 15 57 73 74 48 25 47 41 88 61 75 22 91 10 88 46 87 75 50 78 51 51 6 16 97
                 84 95 20 69 72 8 53 3 85 65 57 76 63 47 80 34 69 35 62 52 54 35 62 69 50 35 55 15 45 98 3 56 63 51 98 8 55 13 89 85 69 6 63 95 21 80 40 80 11 61 29 95 100 65 72 35 77 78 12 68 6 31 65 92 83 2 71 85 85 14 85 48 57 48 59 84 4 11 21 83 77 28 95 57 54 84 20 68 55 21 60 93 43 39 82 28 63 13 66 51
                 39 9 71 18 56 4 79 17 45 15 57 22 17 46 94 82 20 34 83 9 63 30 28 23 86 86 82 90 26 6 51 86 2 37 92 81 68 16 89 70 71 15 60 91 100 76 35 17 89 51 30 29 46 15 25 31 77 87 84 26 80 2 40 55 58 7 25 72 59 62 10 65 70 40 5 80 67 46 44 64 80 97 54 65 15 74 90 65 71 40 82 4 78 48 22 30 34 70 94 88
                 51 3 26 31 67 86 66 57 46 44 36 33 30 47 20 93 93 90 98 98 40 76 77 30 59 38 7 97 98 77 91 81 65 60 53 93 12 2 94 59 70 9 89 48 60 39 95 62 91 51 47 9 86 25 54 58 74 17 95 35 29 79 70 82 77 49 84 98 72 84 44 76 68 8 55 46 81 100 79 72 8 27 5 83 71 93 7 5 2 79 72 70 20 10 60 88 27 36 98 50
                 33 86 40 47 39 2 76 43 72 21 29 16 77 70 35 52 36 54 99 66 30 58 5 13 66 20 54 96 38 39 48 86 8 42 94 43 11 2 34 84 6 4 78 57 37 67 91 51 46 51 85 34 39 93 83 64 92 84 11 89 90 72 6 74 51 5 68 70 80 11 15 66 33 76 57 70 35 20 80 100 32 25 61 11 53 99 62 5 46 65 32 71 14 23 94 33 88 97 73 53
                 40 58 91 32 36 61 94 32 68 71 91 22 47 41 72 38 84 20 38 51 45 46 59 64 17 71 77 88 43 80 29 42 80 25 30 52 26 17 35 57 53 70 6 79 26 39 61 11 55 63 31 7 84 26 39 42 53 49 24 46 80 31 52 3 82 50 14 76 4 37 29 92 29 69 66 38 52 96 60 7 3 40 3 64 5 77 7 25 11 35 43 97 52 37 4 18 90 35 92 59
                 77 21 40 61 95 74 27 62 39 4 33 6 67 80 84 9 8 74 53 27 45 93 88 49 55 3 73 88 65 30 45 71 44 2 30 55 81 17 64 31 11 54 56 23 66 44 28 81 43 72 86 48 40 47 33 79 92 77 14 39 20 18 67 77 19 40 48 83 50 78 62 84 95 48 55 30 47 16 78 26 43 67 95 20 96 90 16 25 47 84 15 15 47 76 78 95 28 84 15 70
                 28 48 71 95 27 13 70 89 23 95 62 10 20 7 6 13 65 47 55 83 56 75 30 91 17 37 47 34 58 92 98 5 80 21 31 68 49 31 87 84 94 74 8 26 16 70 25 75 2 58 40 60 6 95 43 85 63 31 42 4 68 18 90 41 7 34 66 60 38 93 94 53 52 99 68 65 22 14 96 44 61 15 79 23 18 59 82 55 57 25 96 5 48 61 71 51 75 40 55 37
                 19 53 2 21 62 28 81 68 40 78 35 95 22 29 31 94 57 43 77 15 55 44 96 91 47 84 76 31 92 29 67 51 100 4 90 35 34 70 37 56 14 58 63 49 43 45 4 70 42 94 66 49 34 83 90 16 53 91 55 53 96 75 67 41 16 66 19 8 70 25 78 32 12 41 49 26 56 83 18 58 57 50 44 22 12 15 18 46 31 69 25 80 96 35 94 68 66 92 69 51
                 4 96 5 65 72 5 25 55 88 32 45 53 87 73 55 93 42 70 56 58 19 94 20 16 30 74 9 71 76 83 40 17 13 10 19 49 3 84 17 36 20 72 19 25 44 99 38 35 5 32 92 11 85 6 41 22 66 46 12 53 61 17 81 43 20 8 67 34 38 96 61 48 67 44 7 49 14 100 7 49 66 91 11 89 40 38 89 76 34 95 32 86 92 93 82 93 75 41 27 39
                 56 35 15 14 14 51 68 49 100 61 83 89 30 18 6 35 2 22 9 90 84 12 87 57 75 97 95 5 23 44 93 42 54 24 66 13 59 5 62 79 89 73 73 79 67 95 6 41 30 48 12 71 90 15 9 26 41 66 14 32 46 6 98 39 76 41 37 51 7 8 46 19 5 35 87 35 36 68 32 36 7 91 8 39 69 83 45 16 34 81 86 2 100 20 45 91 10 99 85 72
                 85 73 30 6 91 74 10 92 71 62 62 32 84 6 28 98 53 24 27 35 18 66 84 56 26 36 63 38 25 97 96 33 2 90 6 91 88 80 7 30 71 97 26 58 85 79 37 53 74 73 13 14 83 3 14 8 46 90 86 89 85 31 71 21 28 22 51 23 20 51 85 98 55 96 14 68 59 49 50 83 99 43 94 80 65 61 63 92 42 30 50 5 12 69 17 28 16 41 97 48
                 91 55 97 47 13 74 12 24 2 90 36 62 85 8 81 55 70 42 12 22 22 23 4 30 96 94 22 72 55 15 70 56 95 6 87 57 32 20 48 56 28 64 26 39 76 20 47 74 7 35 69 44 58 63 55 28 90 12 34 75 53 19 2 32 20 51 82 72 8 68 72 98 10 22 7 55 25 80 53 39 26 88 69 98 23 58 20 75 33 8 32 89 51 91 10 53 70 78 35 33
                 42 61 43 6 58 89 17 32 54 34 42 92 72 96 8 74 90 9 5 42 50 95 88 73 79 34 52 96 97 29 6 8 12 92 42 41 78 3 34 92 47 10 95 23 41 98 18 74 38 79 6 40 7 2 37 28 34 41 48 83 33 32 88 39 37 50 11 42 74 11 61 46 71 3 70 93 36 91 4 75 17 53 23 61 12 31 24 69 97 76 67 47 28 52 90 71 20 41 97 66
                 9 48 7 21 39 88 95 96 50 59 10 92 12 80 2 73 92 93 63 72 83 83 98 28 13 42 51 97 69 8 37 10 31 60 56 46 90 83 52 38 40 12 36 47 86 14 91 14 16 88 83 46 20 42 77 90 46 25 4 98 11 34 42 2 50 7 43 87 22 41 10 48 15 45 7 30 6 31 27 41 73 62 70 78 64 51 89 76 88 76 89 84 76 39 19 80 77 15 18 38
                 41 82 44 13 32 7 70 74 41 53 23 74 22 22 6 15 46 65 47 75 73 28 10 44 79 58 3 41 18 7 6 53 50 77 19 39 99 81 62 24 63 44 98 7 82 20 99 7 52 62 34 26 90 84 31 83 70 22 11 62 27 90 63 40 73 54 39 44 89 91 94 4 15 1 18 93 98 94 21 97 35 48 14 42 76 51 29 81 76 10 19 8 17 7 29 5 7 49 79 70
                 55 4 7 95 67 41 46 17 47 83 20 66 60 8 18 68 11 81 81 34 24 5 57 6 28 48 73 38 8 6 71 26 57 62 51 15 56 64 36 32 16 97 49 22 81 88 2 60 23 32 3 7 43 34 27 6 55 40 37 69 11 98 77 55 99 20 30 80 7 20 25 81 30 53 75 50 57 26 69 90 5 36 29 87 57 92 38 10 29 83 71 55 82 6 19 32 72 2 72 2
                 35 49 59 93 43 77 93 7 88 8 63 81 34 90 45 38 51 69 60 82 62 64 67 31 9 19 82 83 98 66 64 51 37 25 34 55 77 76 30 73 99 79 40 68 99 53 56 53 26 43 8 33 46 29 88 12 6 85 2 39 98 93 14 49 57 76 56 58 36 57 42 51 44 83 96 81 43 62 8 16 36 87 30 83 10 13 90 41 79 4 96 99 48 75 72 45 42 7 28 24
                 57 29 16 16 50 42 84 62 99 59 47 55 8 53 35 98 35 52 41 92 3 41 92 48 58 31 89 4 74 96 49 11 37 57 57 21 93 77 78 2 53 37 35 7 39 39 22 11 30 38 33 30 33 5 10 67 52 29 27 88 94 95 37 83 6 85 77 55 7 71 33 8 92 91 96 76 88 57 44 2 59 53 94 7 65 43 14 2 57 26 4 54 98 9 77 77 62 23 77 44
                 18 64 90 98 86 95 25 2 40 87 83 79 33 61 95 27 9 86 51 38 4 23 97 28 23 64 78 24 68 91 22 12 26 90 75 18 53 80 20 17 53 15 53 16 54 63 44 54 6 35 41 8 24 25 86 7 12 76 63 58 98 43 14 91 4 59 47 87 48 39 85 19 96 60 14 75 23 20 82 69 76 75 45 26 69 28 34 48 66 95 70 9 47 36 62 82 44 67 33 71
                 53 34 60 39 66 2 16 5 99 39 78 87 100 43 7 52 32 12 27 62 79 46 78 54 96 76 43 69 22 53 23 85 77 30 99 7 81 82 56 14 22 19 12 17 33 21 59 75 68 49 49 7 45 52 53 63 40 27 63 81 7 94 40 11 62 29 100 24 67 7 61 24 86 67 55 77 45 76 4 29 38 26 9 88 63 59 9 7 63 74 7 10 98 36 80 58 3 28 76 39
                 18 25 86 95 81 74 48 66 7 44 60 30 50 84 4 55 16 37 97 51 84 64 91 76 93 66 21 37 59 88 69 19 49 16 35 33 84 57 71 75 98 34 86 50 9 67 41 2 32 75 17 24 54 100 7 54 93 27 99 66 100 21 17 24 19 42 4 64 9 63 15 63 53 95 67 4 79 88 91 66 37 67 27 60 6 8 58 33 37 61 66 4 33 76 52 88 51 12 38 99
                 14 41 36 76 37 7 74 71 52 10 95 39 75 84 11 20 70 58 100 47 70 6 2 7 50 64 66 98 23 15 21 64 75 73 22 20 56 9 99 48 17 33 19 61 30 37 27 84 51 58 68 47 51 19 12 13 89 74 98 70 46 61 67 9 8 73 39 83 3 82 53 13 48 10 40 35 13 26 8 38 48 10 2 23 77 43 82 58 89 16 82 35 31 93 54 97 46 48 84 22
                 29 55 36 29 95 63 96 32 43 83 61 6 44 24 67 86 59 20 17 9 62 72 33 20 25 7 56 70 88 24 26 74 53 53 81 74 82 21 69 100 9 4 100 91 53 5 72 93 16 26 34 30 70 70 83 37 44 39 14 83 90 37 44 50 8 94 65 88 10 65 16 75 9 57 90 75 20 95 82 29 12 65 41 74 12 55 61 66 81 61 15 25 27 83 65 58 8 14 88 58
                 73 98 44 94 82 4 20 93 77 94 21 37 17 88 75 3 14 95 15 45 100 61 96 12 100 42 91 88 60 34 34 19 85 19 37 10 14 85 84 97 69 23 77 3 14 57 21 25 34 83 82 60 9 97 70 53 18 54 25 46 4 70 81 90 5 15 20 5 24 13 3 86 25 66 99 89 18 43 84 74 68 68 7 37 15 67 69 5 15 67 88 38 79 80 8 33 71 65 72 9
                 51 41 39 15 88 80 91 22 52 74 52 3 68 83 96 30 74 40 8 82 46 52 40 11 39 45 44 38 56 66 85 20 2 31 57 78 15 55 97 29 35 9 15 7 64 9 31 25 10 98 67 70 31 67 86 36 83 95 17 33 12 62 68 23 92 8 88 82 15 47 90 28 17 57 60 74 99 29 13 27 17 57 7 91 55 6 86 52 28 77 43 29 35 89 93 10 75 93 25 74
                 65 75 44 69 38 38 36 68 69 18 62 62 57 74 79 64 29 91 52 60 16 49 13 79 34 47 92 7 42 49 25 61 72 77 12 27 26 79 85 23 67 31 59 78 28 54 87 25 12 50 50 60 46 29 28 75 7 64 95 97 12 84 48 5 94 39 96 44 25 50 52 79 44 12 34 42 17 33 41 42 84 36 86 52 48 71 20 63 24 97 17 48 4 73 9 89 20 73 12 27
                 53 36 55 42 68 19 92 43 42 62 47 92 82 29 100 79 81 90 35 89 23 30 79 38 91 51 39 66 10 80 44 96 98 57 33 60 99 57 2 97 69 73 20 22 91 91 45 26 73 14 87 73 9 92 98 89 40 100 89 75 87 57 10 81 48 16 63 37 66 30 6 55 21 35 100 82 93 91 62 69 85 84 90 6 58 93 96 95 26 28 36 46 100 79 100 72 39 94 44 70
                 52 6 98 4 55 97 12 39 78 88 61 79 13 95 45 70 39 68 69 57 38 30 16 71 24 46 36 22 62 83 52 12 93 92 37 81 42 34 20 64 5 87 84 73 21 67 14 99 34 39 100 15 65 47 76 58 35 84 100 100 78 51 69 59 79 33 96 81 12 53 28 100 27 48 70 16 9 62 51 51 69 87 15 86 66 7 54 59 39 24 80 100 78 58 81 69 61 11 80 68
                 88 25 78 2 74 70 15 79 12 39 18 10 56 43 81 4 16 57 60 75 52 34 97 91 74 98 42 6 74 71 63 92 11 73 86 50 90 30 25 49 29 42 49 23 38 90 61 49 43 87 2 37 46 17 35 29 26 94 14 4 61 54 92 98 31 70 86 54 30 91 69 82 93 41 13 54 41 79 97 4 46 41 12 62 51 81 61 16 89 18 73 68 7 46 23 47 95 18 100 18
                 13 76 21 21 65 94 19 72 46 62 57 78 91 10 5 92 8 63 44 13 15 59 89 96 76 39 5 34 17 16 32 89 36 99 3 51 64 65 11 54 48 33 10 69 100 31 50 5 90 19 69 2 61 32 76 26 70 10 14 3 17 20 71 83 46 67 18 18 31 67 28 33 43 89 59 93 9 56 69 94 77 85 47 96 54 13 5 11 79 14 28 73 60 89 26 86 62 40 84 89
                 28 56 27 52 10 8 17 44 17 27 49 47 80 97 12 17 76 32 97 25 47 82 48 62 71 22 74 30 68 61 60 77 63 53 88 60 5 25 17 30 23 23 79 70 41 69 27 97 79 87 87 88 46 49 65 4 26 84 35 46 50 49 65 2 53 17 81 29 64 43 88 35 31 13 8 20 72 51 78 62 48 11 44 24 58 79 71 79 37 22 55 2 4 50 97 10 20 42 7 95
                 14 43 55 62 56 84 15 87 66 31 59 45 76 65 78 40 59 20 19 45 15 61 27 98 85 57 50 53 15 10 4 31 85 16 98 91 93 72 12 74 56 22 62 58 91 87 9 37 21 55 21 59 13 10 15 63 39 98 11 6 82 71 35 40 42 4 56 31 89 63 58 39 38 92 54 22 5 62 6 45 11 89 99 11 57 31 10 7 63 81 72 39 49 67 33 82 13 58 62 77
                 79 7 99 18 50 80 54 33 94 40 100 82 5 32 82 55 55 18 3 57 45 4 28 52 53 89 27 24 31 13 47 70 22 94 21 27 8 54 4 99 24 99 66 45 11 100 36 49 92 51 72 78 55 29 62 44 99 87 26 10 83 65 66 52 4 51 29 39 98 50 36 12 93 34 70 22 88 40 54 78 11 52 5 58 90 30 46 46 55 3 88 74 69 80 84 32 35 93 48 39
                 35 68 32 79 20 92 97 36 93 54 84 3 63 17 23 73 83 89 18 6 68 47 52 61 62 4 4 43 27 93 40 48 62 31 81 7 51 49 40 11 14 50 3 88 61 43 37 35 63 69 59 10 32 29 33 87 81 84 34 13 51 22 41 36 30 11 36 31 23 21 62 67 23 100 35 54 96 51 29 7 81 39 78 80 67 99 4 24 74 26 11 23 77 86 23 3 12 20 52 3
                 28 14 31 41 41 13 21 5 53 83 33 3 88 79 73 100 49 58 30 99 100 30 57 81 100 55 70 34 96 58 39 28 40 16 22 34 98 21 23 81 94 100 61 59 70 35 97 8 62 82 92 47 32 77 95 86 51 70 81 45 23 37 91 50 56 28 90 84 10 87 87 100 40 68 33 84 46 41 27 28 89 4 62 53 53 7 96 2 79 51 88 84 80 55 29 91 76 33 16 72
                 58 17 39 4 21 23 31 85 19 12 10 70 31 21 59 60 71 13 78 26 35 85 97 32 66 47 21 48 18 99 17 41 86 86 30 6 18 40 77 68 89 95 21 65 58 52 74 69 39 56 55 64 15 12 50 58 3 90 13 25 57 35 36 55 90 91 34 39 86 80 29 19 20 84 6 60 4 30 67 93 77 96 21 7 12 89 100 69 6 82 41 39 15 66 93 80 28 3 78 10
                 60 30 95 61 65 3 29 64 77 26 29 17 13 26 86 5 25 56 22 27 16 71 56 55 57 70 12 86 33 39 31 49 38 31 71 83 54 43 4 44 20 24 16 97 19 25 61 47 74 76 26 41 86 65 24 92 48 32 13 88 35 51 26 64 58 97 96 51 30 83 29 70 88 30 58 83 66 33 75 39 81 81 4 45 34 92 36 68 21 53 80 96 86 27 35 96 46 23 33 93
                 57 51 18 22 74 17 33 64 78 28 50 88 40 90 87 44 34 34 10 98 89 90 100 13 4 100 89 77 56 75 58 62 69 67 18 32 52 12 15 45 4 24 19 81 71 34 36 36 87 15 82 51 90 41 66 37 12 84 40 89 88 83 89 70 91 43 34 94 67 25 85 45 80 82 82 75 58 47 36 20 41 62 5 50 57 58 52 8 90 10 67 23 89 9 18 10 13 45 37 18
                 47 11 27 33 28 36 18 77 62 74 24 70 60 31 27 30 29 16 63 73 42 84 36 40 41 11 64 42 15 25 77 28 53 75 77 72 27 27 31 34 26 44 30 90 67 84 79 39 90 2 52 77 56 64 96 37 23 97 62 63 60 12 89 30 66 18 53 10 22 57 53 22 59 45 45 48 33 46 50 5 45 61 8 84 87 35 25 9 64 67 57 55 44 69 87 14 94 10 54 77
                 25 63 82 40 100 82 98 82 27 17 58 38 23 67 8 22 35 13 17 18 81 39 20 98 92 18 66 18 25 24 27 33 25 87 74 95 71 35 69 85 29 47 24 95 17 43 55 54 39 58 22 60 87 70 93 80 29 16 87 36 5 86 7 13 16 77 70 5 8 83 6 68 88 84 26 93 58 12 71 81 78 99 25 33 96 69 48 59 92 16 66 89 51 80 20 29 17 95 82 83
                 71 49 72 93 56 74 64 73 19 90 65 66 76 79 69 12 100 86 62 22 18 68 54 33 3 83 71 72 13 41 10 45 56 3 17 94 60 38 15 14 47 29 73 83 18 3 33 15 37 6 26 10 65 9 15 28 92 47 86 61 96 85 14 100 59 75 52 61 50 98 73 4 43 72 70 2 72 13 42 86 36 18 55 22 24 91 93 58 62 9 36 6 23 28 29 59 52 49 64 13
                 91 83 7 55 56 82 47 85 21 68 33 64 48 86 41 97 53 99 88 4 9 51 21 2 66 54 24 26 23 46 41 38 21 89 86 82 27 77 61 30 99 45 61 63 60 100 6 70 98 11 10 53 58 97 38 48 86 21 7 9 7 98 12 17 61 2 29 15 48 31 57 20 43 45 51 51 88 16 85 7 56 36 37 87 78 41 7 97 9 49 72 20 99 13 24 18 89 91 20 78
                 75 92 57 9 38 23 97 29 59 90 34 11 97 11 75 99 21 23 72 30 2 28 3 87 51 18 20 71 70 69 47 16 53 61 25 39 86 23 7 15 67 3 41 18 51 61 2 68 95 38 75 46 71 36 4 56 43 8 53 19 38 98 16 67 87 86 91 73 76 37 85 27 23 61 80 50 46 30 7 91 80 93 91 4 59 82 94 3 48 71 11 43 41 86 66 96 45 17 62 63
                 55 67 74 67 79 4 69 64 7 62 38 61 65 92 50 60 45 51 37 43 78 11 64 67 49 24 89 72 81 82 84 37 22 11 29 12 39 88 52 62 11 63 31 95 22 65 8 60 68 66 12 49 4 47 35 39 5 98 26 100 14 23 92 20 12 53 41 93 48 65 7 89 5 15 40 23 12 38 7 39 44 69 44 56 69 19 87 81 49 71 77 6 15 43 20 80 5 79 11 98
                 73 49 61 100 70 57 61 14 30 39 96 52 94 11 67 86 80 67 35 33 95 16 94 65 45 22 81 94 30 100 62 51 56 66 49 7 59 35 56 67 71 46 24 40 89 57 38 59 79 11 68 50 57 16 80 100 15 47 28 51 83 83 3 14 58 8 62 99 77 9 65 49 41 69 99 10 86 11 92 14 6 10 10 100 46 41 68 10 86 99 66 70 54 13 28 74 84 56 46 51
                 31 26 18 78 91 78 33 49 89 42 53 30 32 16 78 19 19 69 14 59 98 69 59 57 53 48 74 47 84 12 95 86 31 63 40 70 92 64 29 63 6 37 72 47 90 20 84 52 82 89 66 23 32 15 81 76 56 11 15 70 50 3 88 43 49 24 55 24 37 44 73 45 18 63 32 91 75 77 34 71 59 55 50 80 2 59 61 60 48 71 99 4 82 82 27 89 44 11 79 71
                 74 96 66 62 59 42 78 95 35 31 69 65 11 13 28 53 88 27 84 33 85 66 48 28 46 26 3 41 30 51 19 67 83 51 45 77 97 41 10 33 41 22 9 10 18 85 68 8 97 20 62 73 50 10 71 52 11 4 86 39 15 90 49 39 74 100 67 95 31 75 3 37 45 81 30 13 18 25 8 97 55 29 87 94 30 40 61 12 71 50 34 5 91 57 6 82 20 64 63 31
                 82 32 93 38 11 26 50 39 99 29 2 65 29 89 42 54 91 63 4 79 48 20 11 82 88 79 55 14 30 81 65 11 12 92 92 92 41 28 50 4 60 19 76 89 95 74 85 93 71 19 54 66 76 35 43 45 75 73 75 69 46 54 5 96 32 90 52 13 74 83 95 50 14 85 77 91 26 20 12 98 66 60 45 38 73 20 82 86 31 96 19 57 79 18 75 36 31 63 81 7
                 74 66 97 6 60 95 10 85 14 6 78 89 39 46 51 19 91 25 94 86 64 58 77 64 75 58 69 43 2 31 97 9 94 71 83 2 16 62 71 85 33 54 95 38 29 45 86 3 55 37 69 49 96 100 45 11 11 65 90 97 84 84 30 22 33 45 89 86 95 83 91 59 28 85 4 11 66 60 61 75 73 86 46 24 100 43 67 57 30 27 26 83 13 86 67 51 65 14 74 100
                 55 41 50 41 37 6 3 36 6 61 69 17 44 35 65 100 94 86 94 64 7 61 94 41 69 91 51 51 61 84 56 40 96 64 9 85 47 27 64 100 49 32 36 27 34 54 37 79 2 85 28 2 45 86 66 51 45 72 12 62 81 54 63 87 18 58 19 86 85 41 9 76 98 38 96 43 50 100 31 18 57 83 57 14 4 31 53 49 14 67 51 59 64 92 11 23 38 93 45 31
                 41 19 53 62 96 97 31 51 82 69 84 58 83 13 97 31 65 85 62 61 7 40 75 91 90 21 94 15 65 69 65 22 16 56 31 65 91 77 39 19 39 40 35 26 13 54 21 34 31 89 28 66 5 52 61 37 28 81 8 82 33 26 80 40 20 58 82 100 93 93 61 8 59 20 68 10 76 70 84 71 47 57 13 88 24 46 81 41 64 89 76 37 83 32 10 62 80 57 11 22
                 55 36 47 5 13 64 90 12 47 91 46 31 68 46 71 13 25 55 67 76 23 83 14 95 23 34 87 29 6 78 14 73 49 95 63 44 77 89 22 81 41 73 90 55 54 64 98 32 16 8 18 87 56 54 98 82 22 69 50 69 66 37 85 76 55 15 56 37 2 83 100 24 76 18 33 15 91 12 32 12 76 65 53 48 25 71 70 93 78 38 25 69 11 79 82 32 59 50 52 96
                 54 91 25 79 90 51 34 34 61 64 37 89 77 62 18 14 62 93 46 45 41 50 8 97 24 59 68 61 55 42 2 75 48 2 94 86 15 80 97 61 24 97 57 99 18 63 25 77 18 71 59 99 84 24 39 79 7 57 25 64 57 13 80 90 53 15 91 21 8 96 71 95 52 52 33 21 62 33 47 31 86 37 15 63 50 59 74 26 92 34 29 9 23 60 21 60 93 4 67 5
                 44 55 40 69 84 26 96 55 91 26 52 21 16 82 69 62 38 67 53 11 3 30 9 45 74 82 98 40 19 64 60 98 63 14 15 73 19 24 31 26 59 83 85 9 3 30 26 99 57 69 59 2 21 96 66 46 40 8 65 57 69 50 25 69 77 38 18 36 37 29 5 14 55 47 96 39 66 89 58 81 40 57 74 40 19 71 62 77 92 38 15 51 13 4 87 32 32 66 59 61
                 60 27 70 8 77 84 12 96 13 5 42 27 8 17 34 77 94 89 89 88 3 12 12 12 78 69 20 68 32 47 16 9 42 56 41 90 13 36 50 77 68 85 22 48 92 19 77 55 62 38 97 38 98 73 50 24 54 98 34 27 16 27 97 30 85 13 33 47 24 67 88 54 14 41 83 12 28 72 82 48 61 42 97 36 31 67 67 4 86 46 93 92 96 20 19 56 6 78 77 98
                 46 38 67 42 82 85 57 73 84 38 12 45 2 99 38 23 47 33 100 50 52 52 55 36 57 22 50 36 10 96 4 67 88 90 46 74 56 42 40 91 72 14 42 92 51 46 50 66 73 25 82 88 11 17 16 42 99 10 82 29 27 89 95 67 46 41 98 71 40 40 29 18 93 79 92 86 39 66 54 91 7 45 94 18 48 56 94 11 60 82 77 41 55 6 16 12 19 98 62 13
                 10 90 89 61 61 3 54 11 7 37 52 32 11 78 54 60 98 59 25 36 76 87 8 25 52 15 95 66 40 85 24 16 61 59 31 32 71 17 100 36 86 19 79 12 24 61 53 12 49 24 71 62 11 91 8 21 51 38 7 7 8 80 54 11 22 23 12 56 31 53 10 61 6 75 29 53 68 46 41 94 25 85 68 17 39 35 6 52 18 14 92 22 29 73 88 16 62 23 57 79
                 45 50 73 15 41 41 47 7 76 90 50 13 66 38 72 18 88 31 57 79 33 12 73 33 69 32 54 23 80 95 5 31 69 9 96 3 83 24 3 15 15 59 70 4 73 35 43 61 43 46 74 92 8 79 24 41 11 84 37 73 71 3 63 77 5 31 8 4 59 45 17 96 26 35 66 99 51 89 40 79 93 31 49 66 41 27 19 100 20 18 100 43 79 46 97 45 66 11 44 29
                 60 25 73 59 70 48 44 83 23 60 36 83 59 25 71 63 95 53 50 25 17 83 46 30 36 73 32 23 97 87 51 21 30 99 96 44 79 22 54 51 35 22 63 77 58 18 40 86 22 13 11 11 62 27 18 32 76 68 69 42 51 75 68 12 64 83 93 84 15 25 52 71 40 28 63 49 38 81 78 86 100 19 57 94 41 26 57 44 40 36 39 30 23 42 5 24 53 64 76 4
                """;
    }
}

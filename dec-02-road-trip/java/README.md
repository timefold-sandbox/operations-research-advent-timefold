This is an implementation of the roadtrip problem in Java using Timefold.

For more advanced examples, check out the [Timefold Quickstarts Repository](https://github.com/TimefoldAI/timefold-quickstarts).
This project leans towards the [Vehicle Routing Quickstart](https://github.com/TimefoldAI/timefold-quickstarts/tree/stable/java/vehicle-routing).

## Analysis

### Assumptions

- I have assumed that the distances and fuel costs are not directional. Ex. entry `1 37 60 5` means both 1->37 and 37->1 have _a distance 60_ and _fuel cost of 5_.
- I have assumed that the roadtrip is from 1->100 and _not a roundtrip (1->100->1)_.
- My assumption was that we would prefer to take a detour to see more cities, before minimizing our distance. (It's a roadtrip after all :-).

Both these assumptions could easily be implemented if requested ;)

### Domain model

In this case, I have chosen to model the domain in a very simple way and have moved everything related to distance/fuel calculation to a helper class. 

```mermaid
classDiagram
direction RL
    class Visit {
        +int id
    }


    class Traveler {
        <<@PlanningEntity>>
        List~Visit~ visits
        
    }

    Visit <-- Traveler:  @PlanningListVariable((allowsUnassignedValues = true)
```

### Constraints

Note: This is implemented in class `RoadTripConstraintProvider`, which contains some additional documentation.

- HARD: Do not use more fuel than the budget (73)
- MEDIUM: Try to visit as many locations as possible
- SOFT: Minimize travel distance

## Results

The first entry in the table is the base case with the listed assumptions.
The other entries are some alternative interpretations.

| Constraints Enabled        | Bi-Directional Data Used | Distance | Fuel Used | Route                                                                                                                                                                  |
|----------------------------|--------------------------|----------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ALL                        | Yes                      | 2122     | 70        | 39 locations: [1, 18, 46, 44, 72, 19, 69, 90, 45, 99, 39, 43, 97, 60, 16, 75, 47, 87, 81, 85, 32, 2, 3, 15, 6, 66, 52, 26, 57, 38, 51, 74, 24, 11, 5, 34, 14, 42, 100] |
| ALL except maximize visits | Yes                      | 52       | 15        | 3 location: [1, 80, 100]                                                                                                                                               |
| ALL                        | No                       | 1451     | 72        | 28 locations: [1, 37, 69, 51, 7, 25, 81, 85, 3, 15, 48, 86, 19, 6, 40, 12, 32, 2, 90, 14, 83, 4, 61, 72, 5, 34, 42,100]                                                |
| ALL except maximize visits | No                       | 80soft   | 43        | 4 locations: [1, 59, 2, 100]                                                                                                                                           |

## Tech

### Technologies used

- [Timefold Solver](https://docs.timefold.ai/timefold-solver/latest/introduction), an Open Source AI Solver.
- [JBang](https://www.jbang.dev/documentation/guide/latest/index.html), a tool which helps create single Java file scripts with dependencies.

### Running the application

- You need to install JBang.
- Go to the correct folder.
- Execute `jbang Dec02Roadtrip.java`


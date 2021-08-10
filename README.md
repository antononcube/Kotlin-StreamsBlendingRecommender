# Kotlin Streams Blending Recommender

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

Kotlin implementation of a Streams Blending Recommender (SBR) framework.

Generally speaking, Kotlin-SBR is a "computer scientist" implementation of a recommendation system
based on sparse linear algebra. See the article [AA1] for details.
Object-Oriented Programming (OOP) is used.

This implementation is loosely based on the software monads 
[SMRMon-WL](https://github.com/antononcube/MathematicaForPrediction/blob/master/MonadicProgramming/MonadicSparseMatrixRecommender.m),
[AAp1], and
[SMRMon-R](https://github.com/antononcube/R-packages/tree/master/SMRMon-R), [AAp2],
and very closely follows the SBR Raku implementation [AAr3]. 

## Implementation properties

The current implementation is mostly a "reference implementation" that is supposed to be 
clear to OOP-knowledgeable programmers. (And definitely to Java and Kotlin programmers.)

Hence, some implementation steps are easier to understand than faster to compute.

Since Kotlin is a "first class citizen" in IntelliJ IDEA this implementation can 
be studied using the automatically generate Unified Modeling Language (UML) diagrams.

Another reference implementation -- also using OOP -- is given with the Raku package [AAp3].

## References

### Articles

[AA1] Anton Antonov, 
["Mapping Sparse Matrix Recommender to Streams Blending Recommender"](https://github.com/antononcube/MathematicaForPrediction/tree/master/Documentation/MappingSMRtoSBR), 
(2019),
[GitHub/antononcube](https://github.com/antononcube).

### Packages, repositories

[AAp1] Anton Antonov,
[Monadic Sparse Matrix Recommender Mathematica package](https://github.com/antononcube/MathematicaForPrediction/blob/master/MonadicProgramming/MonadicSparseMatrixRecommender.m),
(2018),
[GitHub/antononcube](https://github.com/antononcube/).

[AAp2] Anton Antonov,
[Sparse Matrix Recommender Monad R packages](https://github.com/antononcube/R-packages/tree/master/SMRMon-R),
(2018),
[R-packages at GitHub/antononcube](https://github.com/antononcube/R-packages).

[AAp3] Anton Antonov,
[Streams Blending Recommender Raku package](https://github.com/antononcube/Raku-ML-StreamsBlendingRecommender),
(2021),
[GitHub/antononcube](https://github.com/antononcube).

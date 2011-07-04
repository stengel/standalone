Note that the C code coclique3.c  only assumes numbers as input
that identify the extreme equilibria as parts of the
bipartite graph, nothing about the x and y themselves.

Communication is via standard input and output.

A possible input is:

1 2
3 4
5 7
5 4 
2 6


it should create:

Connected component 1:
{1}  x  {2}

Connected component 2:
{3, 5}  x  {4}
{5}  x  {4, 7}

Connected component 3:
{2}  x  {6}


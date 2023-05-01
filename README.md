# DockerManager

A java tool made for complex to deploy test scenarios with multiple devices involved and in which precise coordination and flexibility on test configurations is a must.

The name is misleading, as this tool is generic enough to work with software other than Docker.
In fact the tool can run any command that you would run in a normal shell.

The basic working of the tool is as follows.
There are two entities (and two Main classes): [CoordinatorMain](src/CoordinatorMain) and [ClientMain](src/ClientMain).
The Clients are the ones that will execute the instructions for each test scenario.
The Coordinator is responsible to distributing to each Client its respective instruction for each test, while making sure all Clients start each test at the same time.

For each test scenario, a test file with a set of variables should be defined.
DockerManager will then, based on said variables, on their possible values and how those values should be iterated and repeated, generate all the possible test configurations, and coordinate the Clients to execute each of said tests.
The file [testInput.txt](textInput.txt) contains an example of a test file.

For VLDB reviewers: this tool was used to help executing the multiple experiments in our submission.
Please check [here](https://github.com/AndreRijo/potiondb-vldb-configs-rep) for the list of test files and more that were used.
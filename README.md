# DockerManager

A java tool made for complex to deploy test scenarios with multiple devices involved and in which precise coordination and flexibility on test configurations is a must.

The name is misleading, as this tool is generic enough to work with software other than Docker.
In fact the tool can run any command that you would run in a normal shell.

The basic working of the tool is as follows.
There are two entities (and two Main classes): [CoordinatorMain](src/CoordinatorMain.java) and [ClientMain](src/ClientMain.java).
The Clients are the ones that will execute the instructions for each test scenario.
The Coordinator is responsible for distributing to each Client its respective instruction for each test, while making sure all Clients start each test at the same time.

For each test scenario, a test file with a set of variables should be defined.
DockerManager will then, based on said variables, on their possible values and how those values should be iterated and repeated, generate all the possible test configurations, and coordinate the Clients to execute each of said tests.
The file [testInput.txt](testInput.txt) contains an example of a test file.
It is possible to supply multiple test configuration files at once to DockerManager.

For VLDB reviewers: this tool was used to help executing the experiments in our submission.
Please check [here](https://github.com/AndreRijo/potiondb-vldb-configs-rep) for the list of test files and more that were used.

## Supported parameters

The following optional parameters are supported by both ClientMain and CoordinatorMain:
- `sleep timeToSleep`: sleeps `timeToSleep` seconds before starting the client/coordinator. May be useful when preparing for multiple sets of experiments;
- `coordconfigs true/false`: if `true`, the client will receive the test configurations automatically from the coordinator. If `false`, the client will read from the standard input the test configurations. The value for this parameter must be the same for the clients and coordinator. Default: `true`;

ClientMain supports the following optional parameters:
- `mode modeName`: three possible values: `time` ends the execution as soon as the time for the test ends; `active` watches until the command executed by the client finishes, and informs the coordinator when it does finish; `passive` waits until the coordinator informs that all clients with `active` have ended. Default: `time`;
- `port number`: the port to listen to the Coordinator. Only relevant if coordconfigs is set to true. Default: `6666`;
- `savelog true/false`: if true, a log with the standard output of the commands executed will be produced. Default: `false`.

Note: When running clients in `mode active`, the clients will not lock forever in case a test fails: after the allocated test time ends, if the test is still running, the client interrupts it and reports a timeout to the coordinator. All the clients (and the coordinator) will then proceed to the next test.

CoordinatorMain supports the following optional parameters:
- `waitfor numberoftheclients`: list with the number of the clients (starting at 0) that were started with `mode active`. The coordinator will wait for a notification from all of those clients before marking a test as completed;
- `autorepair value`: if this parameter is supplied, the coordinator will, at the end, repeat all tests that had a timeout. If `value` is true, only tests that timeout are repeated. If `value` is a number, all tests that end early by at least `value` seconds will also be repeated. Thus, `autorepair` can be used to repair both tests that hang or that crash early; 
- `repair listofrepairs`: list with the numbers of the tests to be executed. Useful for repeating tests that failed or only a subset of the tests. Tests for different configurations are separated by a `|`, tests in the same configuration are split by `,`. Can use ranges too. Example for 3 configuration files: `"2,4-8,11,15-18|2-8|0-4"`. Note: Always encapsulate the list with `""` in order to avoid issues with the usage of `|`;
- `nodes listofnodes`: list of ip:port of the clients, separated by comma. Overrides the list defined in all test configurations. Useful if the set of machines changed but the test configurations is the same. 


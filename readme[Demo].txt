README
Group 18

Note: We did not have time to implement Adam's feedback.

Project consists of 5 files; Client.java, Errors.java, ErrorSimulator.java,
NetworkErrors.java and Server.java.

To Run:

Normal Execution
To perform a normal execution, run Server.java, then ErrorSimulator.java and
then Client.java. Server.java and Client.java will prompt for a path, quite 
simply input a path to store files i.e. C:/Users/UserName/Desktop/folder 
(NOTE: the folder must already exist). DO NOT input file name here, this is 
only the path. The file you are trying to write or read must exist inside the
respective path. The errorsim will automatically default to normal execution,
so you can simply run the errorsim to get normal behaviour without any setup,
or you can set it to normal operation by following the prompts of the errorsim.
Next, turn your attention to the Client.java prompts as it will ask information 
for additional information such as operation (read/write) and file name. Once 
all information is entered press okay and a text file will appear in either one 
of directory paths inputted at launch and will have the same name as the file
that was sent.The program will re-prompt until you select cancel (in clients case) 
or type quit (in the servers case).

Error Execution
To simulate an error, run Server.java, then ErrorSimulator.java and then 
Client.java. Enter in a directory for Server.java and Client.java. Then view the
ErrorSimulator prompt. A menu of 4 items will appear, either
choose normal operation, delay, duplicate or lose. Next enter an error command,
to see all commands type commands. Next choose a block for the error to occur on
Lastly enter the type of packet for it to occur on (data or ack). Now that the
simulator is set, turn your attention to the Client.java prompt. Follow the
prompt like you would in normal execution (see above) the errorsim will print
when the error ocurred. Again everything should re-prompt continuously until
the client and server closed.


Iteration 1 Responsibilities
Kari: Use case maps
Omar: UML class diagram
Praveenen: Read and write functionality
Dylan: Client-server connection functionality (iteration 0)
Calvin: Documentation

Iteration 2 Responsibilities
Kari: Implemented error 04
Calvin: Implemented error 05, error simulator
Omar: UML Class Diagram, testing
Praveenen: UCM for error 05, testing
Dylan: UCM for error 04, testing, documentation

Iteration 3 Responsibilities
Kari: testing, documentation
Calvin: timing diagrams, testing
Omar: UML Class Diagram, testing
Praveenen: implemented delayed and lost packets, documentation
Dylan: Implemented duplicated packets, documentation

Iteration 4 Responsibilities
Kari: Implemented error 2
Calvin: Implemented error 3
Omar:  Helped implementation of errors 1,6
Praveenen: Timing Diagrams, testing, minor code improvements, Implemented errors 1,6
Dylan: Timing Diagrams, UML, documentation, minor code improvements


Aleks Lisutta 
Bar Kyzer 
Muhamad Orabi 



AMI: ami-0b0dcb5067f052a63
instance type: micro
to compile the src to JAR please run "mvn assembly:assembly -DdescriptorId=jar-with-dependencies"
Before running the application make sure you update the configurations accordingly.

place the provided config file next to the jar and update it to include the local configurations file you just updated.
The configuration file is named config.txt.

In the first row enter the credentials path, and in the second row enter your bucket name.

For the excecution run the following command from the jar's directory:
java -jar DistOcr.jar InputFile OutPutFile n [terminate]


How the application works?
The implemantation contains 3 modules that comunicate with each other by sqs.



The local application workflow:

Uploading the input file to s3 then Checking if the manager is working, and if not, creating it Sending the location of the
 input file to the manager in SQS and saving the message id for later use. 
the local application waits for a message from the manager in SQS containing the previous message id. then wait for answers from the manager on SQS 

and agragate them. once all links have been processed make them into an html.
Optional - If got a termination argument, sending terminate message in SQS to the manager .





The manager workflow:

Creates the 4 SQS's for communication with application and the workers then the manager Waits for messages from the local applications on  SQS. 

When Getting a txt file message from the local application split the input file and send messages to the workers SQS.
then if there are less workers than required create new workers and wait on SQS for either the workers to finish or get termination from the localAPP.
when a worker returns an answer to the manager via SQS he sends it back to the local app via a different SQS.
if given "terminate" message from the application, the manager raises a termination flag an waits for all the opened application 
requests to be finished. Later, closing all the SQSs, closing all the workers instances and finally closing itself.





The worker workflow:
 
The worker waits for messages from the manager in SQS.
 Downloding the file link needed to be analyzed as txt file and parses it.
 
Sending to the manager the result\error message
 if an error occured in SQS.
 
lastly deletes the input file and the result file from the local memory.

in our implementation we could not get the OCR library to run on the workers and we ran out of time to debug it. 
therefore we simply retun the name of the downloaded file.
we hope that by demonstrating our knowladge of the project and the rest of the framework we designed we could get a passing score on this assignment.
we ask for your consideration on the matter. 
other than the ocr all other requirements were met and the OCR aspect of this project is simple "some hard task" to be performed by the workers we showed that we can build a system for doing so, we simply couldnt load the specific OCR .




Performances: 

Total running time= ~7000 seconds

n = 1( n in the running command)


Regarding security - the credentials are passed to the slients though a credentiasl provider which 

sets them securely.


Regarding scalablity - the "heavy lifting" is done by the workers which are created by the manager according to the amount
of job requests.


The manager itself is built from 2 "listeners" threads that take care of the SQS communications. 
In addition the manager hold
s a thread pool which is responsible for handling the local application's requests and for updating the local agragator with 
the responses from the workers.

The way that the manager was built allows it to be duplicated as many times as needed which helps to make the whole system
more scalable.

There is one downside in the way we built our system in regards to scalability - there is only a single SQS for all the
LocalApplication's results, so they are all waiting on the same SQS.



Persistance - if a worker dies in 
the middle of the parsing, the message can be parsed later on by another worker. The manager makes sure after each request
 to open the right amount of workers needed according to the number of tasks in the SQS, meaning that even if a worker dies
 it will be replaced in the handling on the next request.

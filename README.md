<h3 align="center">OPIA Mobile App</h3>

<p align="center"> Opia is an open source tool for on-device testing to better support developers in testing security vulnerabilities during evolution and maintenance of mobile apps. 
It allows developers/testers to (i) execute record-and-replay testing; 
(ii) automatically extract information of local databases to automatically conduct SQL-injection attacks;  
(iii) collect execution logs with the purpose of identifying leaks of sensitive information via logs, and 
(iv) extract data stored in local databases and shared preferences to identify sensitive information that is not properly encrypted. </p>

## Technologies

Opia has three important components, a mobile application and a web server. In this repository you can find the mobile application.
The Mobile Application is an Android Java application, it has an accessibility service running in background to observe and replay user's actions. 
Also, it can send requests to the Web Server using HTTP and it can save actions in the NoSQL database through the Firebase Client. 

The Web Server, well known as Execution Engine uses a microframework for Python called Flask. 
It is in charge of executing Android Debug Bridge (ADB) commands remotely and processing the outputs.

Finally, the NoSQL database is hosted in Firestore.

## Installation

### Prerequisites

Mobile Application -> Opia requires Android 6 or higher. We use Android Studio in order to install Opia on real devices or emulators.

Web Server -> Opia requires Python 3 or higher. After cloning/downloading the repository (https://github.com/TheSoftwareDesignLab/OPIA-web-server), run the following command in the root folder:

    $ source bin/activate

Then, change the directory to /app:

    $ cd app

Finally, to start the server:

    $ python3 app.py

## Testing your first app

First you need to enable Opia Service in Settings > Accessibility in your device. Then, you will be able to select the app you want to test.
Next, write the IP Address where the server is running, in order to create a communication between the Mobile Application and the Web Server.

To record, tap the record button, you will see that an 'Eye' button will appear on the top of the screen. 
You should tap that button to finish the record.

To replay, search the app you recorded and the execution to replay and touch the 'Replay' button (a rounded arrow).
To see the log of a replay, search the execution and touch the button with the four horizontal lines. 

To extract tables and shared preferences, touch the button Test Integrity and do not encrypt it! 
To see extracted tables and shared preferences, search again the app and you will see two new buttons, 'Show tables' and 'Show shared preferences'.
If there are not new buttons, that means the application has her backup encrypted.

To execute SQL injectio attacks, touch the 'Injection' button in a previous record.

## Examples

To see Opia running on real application, watch the following playlist:

https://www.youtube.com/playlist?list=PLF5U8kfVgRcJrDapP-nvNXv9TrjqGi6LJ



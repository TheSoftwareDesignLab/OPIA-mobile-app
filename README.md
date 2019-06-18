<p align="justify">
Opia is an open source tool for on-device testing to better support developers in testing security vulnerabilities during evolution and maintenance of mobile apps. 
It allows developers/testers to:
</p>

<ol>
    <li>execute record-and-replay testing
    <li>automatically extract information of local databases to automatically conduct SQL-injection attacks
    <li>collect execution logs with the purpose of identifying leaks of sensitive information via logs, and
    <li>extract data stored in local databases and shared preferences to identify sensitive information that is not properly encrypted
</ol>


## Technologies
<p align="justify">
Opia has three important components, a mobile application and a web server. In this repository you can find the mobile application.
The Mobile Application is an Android Java application, it has an accessibility service running in background to observe and replay user's actions. 
Also, it can send requests to the Web Server using HTTP and it can save actions in the NoSQL database through the Firebase Client.
</p>

<p align="justify">
The Web Server, well known as Execution Engine uses a microframework for Python called Flask. 
It is in charge of executing Android Debug Bridge (ADB) commands remotely and processing the outputs.
</p>
<p align="justify">
Finally, the NoSQL database is hosted in Firestore.
</p>

## Installation

### Prerequisites

<p align="justify">
Mobile Application -> Opia requires Android 6 or higher. We use Android Studio in order to install Opia on real devices or emulators.
</p>

<p align="justify">
Web Server -> Opia requires Python 3 or higher. After cloning/downloading the repository (<a href="https://github.com/TheSoftwareDesignLab/OPIA-web-server"> Opia Web Server </a>), run the following command in the root folder:
</p>

    $ source bin/activate

Then, change the directory to /app:

    $ cd app

Finally, to start the server:

    $ python3 app.py

## Testing your first app

<p align="justify">
First you need to enable Opia Service in Settings > Accessibility in your device. Then, you will be able to select the app you want to test.
Next, write the IP Address where the server is running, in order to create a communication between the Mobile Application and the Web Server.
</p>

<p align="justify">
To record, tap the 'Record' button and use the app as you would usually. You will see that an 'Eye' button will appear on the top of the screen. 
You should tap that button to finish the record.
All the records will be visible once you search again the app.
</p>

<p align="justify">
To replay, search the app you recorded and the execution to replay and touch the 'Replay' button (a rounded arrow).
To see the log of a replay, search the execution and touch the button with the four horizontal lines. 
</p>

<p align="justify">
To extract tables and shared preferences, touch the 'Test Integrity' button and do not encrypt it! 
To see extracted tables and shared preferences, search again the app and you will see two new buttons, 'Show tables' and 'Show shared preferences'.
If there are not new buttons, that means the application has her backup encrypted.
</p>

<p align="justify">
To execute SQL injection attacks, touch the 'Injection' button in a previous record.
</p>

## Examples

To see Opia running on real application, watch the following playlist:
<a href="https://www.youtube.com/playlist?list=PLF5U8kfVgRcJrDapP-nvNXv9TrjqGi6LJ"> Opia Examples </a>

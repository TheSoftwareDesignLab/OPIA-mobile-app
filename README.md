
Opia is an open source tool for on-device testing to better support developers in testing security vulnerabilities during evolution and maintenance of mobile apps. 
It allows developers/testers to:

1.  execute record-and-replay testing
2.  automatically extract information of local databases to automatically conduct SQL-injection attacks
3.  collect execution logs with the purpose of identifying leaks of sensitive information via logs, and
4.  extract data stored in local databases and shared preferences to identify sensitive information that is not properly encrypted

To learn more about Opia and see a real execution, watch the following video: 

<p align="center">
<iframe width="500" height="282" src="https://www.youtube.com/embed/5Q53WsH_Ov0" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</p>

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

To see Opia running on real applications, watch the following playlist:
<a href="https://www.youtube.com/playlist?list=PLF5U8kfVgRcJrDapP-nvNXv9TrjqGi6LJ"> Opia Examples </a>

## Empirical Study

<p align="justify">
Here you will find the results of an empirical study to evaluate Opia in terms of (i) Opia's ability to record and replay a sequence of events in native and hybrid apps; (ii) Opia's capability to find leaked information and (iii) Opia's ability to execute SQL Injection attacks. The context of this study consists of (i) a set of 10 native apps and (ii) a set of 10 hybrid apps, both from the Google Play Store. Both were randomly selected, but hybrid apps were found using the ionic showcase, then searching them in the Google Play Store. Its size varies from 2.8 MB to 41 MB and there is diversity in the categories.
</p>

The list of apps used to test Opia is presented below:

| Name| Category| Size(MB)|Tech| Package|
| :-------------: | :----------: | :-----------: |:----------: | :-----------: |
| Hyperli | Shopping | 29 | Hybrid | com.hyperli.consumer |
| PluraLing | Education | 9.8 | Hybrid | io.mothertone.pluraling_lite |
| WishKaro | Lifestyle | 4.7 | Hybrid | com.apps.wishkaro |
| Screen Addict | Entertainment | 2.8 | Hybrid | io.pintado.screenaddict |
| Boneafite | Social | 9.5 | Hybrid | com.boneafite.boneafite |
| Townylive | Photography | 13 | Hybrid | com.yashin.citylive} |
| Konflist | Events | 3.8 | Hybrid | in.aniket.konflist |
| Zinkerz Daily ACT | Education | 37 | Hybrid | com.zinkerz.zinkerzdailyac  |
| BabyHandle | Lifestyle | 6.6 | Hybrid | com.babyhandle.mobile  |
| Geo Challenge | Game  and  Trivia | 41 | Hybrid | com.wetpalm.GeoChallenge  |
| Gmail | Communication | Varies | Native | com.google.android.gm  |
| Uber | Maps and Navigation | Varies | Native | com.ubercab  |
| Duolingo | Education | Varies | Native | com.duolingo  |
| Omni Notes | Productivity | Varies | Native | it.feio.android.omninotes |
| K-9 Mail | Communication | 4.6 | Native | com.fsck.k9  |
| Journal with Narrate | Lifestyle | 11 | Native | com.datonicgroup.narrate.app |
| OpenTasks | Productivity | 2.6 | Native | org.dmfs.tasks |
| Authenticator | Tools | 5.9 | Native | com.google.android.apps.authenticator2 |
| Netflix | Entertainment | Varies | Native | com.netflix.mediaclient  |		
| My Calendar | Health and Fitness | Varies | Native | com.popularapp.periodcalendar |

### Record & Replay

<p align="justify">
To evaluate if Opia is capable of reproducing user's behavior, we conducted a case study that shows that Opia not only recognizes interactions with GUI elements, but is able to find them and perform the corresponding action when replaying a record. It means, we wanted to know whether we could keep a reliable control of the user's actions to use it later to test security vulnerabilities. To do this, we (i) performed 10 events (GUI element interaction) in 20 apps, including clicks, text inputs and scrolls, and (ii) observed the amount of events replayed by Opia.
</p>

<p align="justify">
The following picture depicts the number of events that Opia was able to reproduce in each of the applications involved in the case study. Green columns correspond to hybrid applications, while the blue ones show the results of native applications. Based on the graphic, Opia could reproduce more than half of the sequence of events in 65% of the apps. In particular, the reproduction of events was successful (more than half) in 80% of native applications, while in hybrid applications it was only 50%. Specially, there are two hybrid applications in which it was not possible to reproduce any of the previously recorded events. 

<img src="/assets/imgs/rq1.png"
     alt="Number of events replayed by Opia in hybrid and native apps"/>

<p align="justify">
To understand this behavior, it is necessary to remember how Opia works. When Opia is recording, it detects when the user interacts with any GUI Element. Each GUI element is represented as a node in the Node Tree Debugging, which is saved in the database with another attributes of the event. In consequence, to replay a previous record, Opia should traverse the tree to find the nodes involved in the sequence. However, the Accessibility Service, running in background, only recognizes Android elements customized by the app. That means, if the elements used by the app are different from the components provided by Android, Opia does not save the element and does not perform the action.
</p>

<p align="justify">
Now, when observing the number of events that Opia was able to recognize, we found that, during the execution of <i>Zinkerz Daily ACT</i> and <i>Geo Challenge</i>, no event was recognized or saved. It is worth noting that hybrid apps are a combination of native elements and web components, thus, Opia is not able to recognize all the elements, in particular, web components.
</p>

<p align="justify">
On the other hand, Opia could not replay all the sequence of actions in native apps because the Accessibility Service should search among the tree for the corresponding node, but it creates a node for each GUI element. In consequence, when the layout is too complex, for instance a Linear Layout inside a Relative Layout inside another layout, the service creates a huge amount of nodes and it stops the search. Thus, Opia cannot find the elements.
</p>

<p align="justify">
The following picture presents a node of the Node Tree Debugging of a native app, <i>Gmail</i>, it is possible to see that a single node of a native app contains many nodes, in this case, that node is the top bar to search mails and it contains more than five nodes.
</p>

<p align="justify">
On the other hand, the next picture depicts the whole Node Debugging Tree of an hybrid app, <i>Geo Challenge</i>. It only contains a single node, a Frame Layout, but it does not have child nodes, because the service is not able to recognize web components.
</p>

### Information Exposure

<p align="justify">
Many developers save user's data on mobile devices to offer offline functionalities, the common ways are using local databases or settings files, called Shared Preferences. Nevertheless, most of developers do not worry about keeping that data safe (i.e preserve data immutable and private). Besides, during development time, developers often write console messages to prove if a determined feature is working, some of those messages contain sensible information such as credit cards, passwords, emails, telephone numbers, etc. Plus, sometimes they forget to remove those messages and user's information is exposed. 
</p>

<p align="justify">
That's why we conducted a second case study to estimate whether Opia is useful to detect leaked information in Android apps. In particular, we (i) extracted backups, (ii) obtained tables and shared preferences, and (iii) analyzed logs after every execution.
</p>

<p align="justify">
The following picture depicts the percentage of apps that encrypt their data, thus Opia cannot extract tables or shared preferences. 30% of the apps take care about confidentiality and privacy by preventing the extraction of the backup while 70% store data in plain text without any access control. 
</p>

<p align="justify">
Besides, the next picture illustrates the amount of tables extracted against the amount of those tables that have any type of codification. We expected and corroborated that native apps had more tables while hybrid apps did not have a huge volume of tables because they use different techniques to store data. However, both types of apps maintain table data in plain text, i.e none of their tables is secure.
</p>

<p align="justify">
A similar situation is represented in the above picture, where there is a comparison between the total amount of Shared Preferences files and the amount of files without any security. There is not a single file with codification, all of them are understandable for everyone.
</p>

<p align="justify">
It is worth noting that tables and shared preferences can store a huge range of information from daily usage of an app to sensible information such as passwords, conversations or payment methods. Those information could be used by an attacker to understand how a user behaves and execute an spoofing attack, steal her identity or even, perform a ransomware. Plus, the user does not have the possibility to prevent or improve the way in which apps are manipulating her data.
</p>

<p align="justify">
On the other hand, this picture presents the results of analyzing the logs after every execution. We found that 70% of the analyzed apps do not leave information printed in console but 30% print data. For instance, urls where the app is saving/retrieving resources, a track of the internal components (i.e logs when starting or finishing an intent, saving new information, deleting data), table names, dates, transaction ids, etc.
</p>

### SQL Injection

<p align="justify">
Based on the previous studies (Record & Replay, Information Exposure), we conducted a case study to evaluate if Opia is capable of execute SQL injection attacks. The main purpose is to test if developers are handling properly user inputs by injecting malign strings. To this, we (i) recorded a sequence of events, (ii) extracted table names and attributes, and (iii) replayed the previous record. 
</p>

<p align="justify">
After performing the injection actions, we found that none of the apps crashed nor a table was dropped. However, Opia generated a list of malign strings based on the tables extracted and replayed three times the sequence of events without interruption. 
</p>

<p align="justify">
Nevertheless, the sample of applications used to carry out the case study shows that developers have good practices to handle user inputs. Therefore, there was no evidence of damage to the databases and no leaks of information. Opia is a dynamic tool, it is testing to discover if an app has vulnerabilities by trying to exploit them. Opia does not analyze the code statically to find how developers handle user inputs and SQL queries, in consequence, there is not certainty of the effectiveness of the attack. 
</p>

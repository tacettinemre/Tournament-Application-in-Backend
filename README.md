# Backend Engineering Case Study

# HOW TO RUN?
The application can be run using docker-compose build, and then docker-compose up. In this process, tests are also executed. After some time, you will see Server Listening on Port 8080. After that, the application is running. Also, there are volumes attached to the container for persistent MySQL and Redis storage.

## **Database Design**
I created 3 MySQL tables. These are users, user_groups, and tournaments. 
- users Table:
  This table has columns: id, coins, level, country, group_id, score, hasReward. Some of these columns are trivial, I will explain the necessary ones below:
    - group_id is an integer column that stores the id of the group that this user belongs in the tournament. If the user is not in a tournamnet, this field is 'null'.
    - hasReward is an integer field that indicates if the user has a reward from previous tournament. If the user's hasReward is 1, this means that user was the first in their group in the previous tournaments, if hasReward is 2, this means the user was second. If its 0, this means that user does not have any unclaimed rewards.
    - As explained above, users table has a foreign key to group_id field of the user_groups table.
    - In addition, users table has an index on group_id field which speeds up some of the expensive queries in the application.

 - user_groups Table: This table has columns: group_id, tournament_id, group_status, countries, version.
   - tournament_id is the tournament that this group belongs to.
   - group_status is a string that can have values: waiting, active, completed. Indicating the state of the group.
   - countries is a string that stores the countries of the users in that group. This field is used to find suitable groups for users since it keeps track of both the number of users, and their countries.
   - version is added to implement optimistic locking in the process of assigning groups to users.
   - This table also has a foreing key and an index on tournament_id.
  
 - tournaments Table: This table has columns: id, is_active. is_active indicates the status of the tournament, 0 means it is ended, 1 means it is still active. This table also has an index in the is_active column.

### Reasoning for this design: In this database design, I aimed the efficiency and simplicity. With this setup I can do my search for a suitable group pretty fast (thanks to countries field in the user_groups). In addition, I can store necessary data for the application's operations.

### IMPORTANT NOTE ABOUT THE SCORE FIELD: I am aware that the score field in the users table might be unnecessary since I used **Redis** to implement the leaderboards. But I thought that it is easier to debug and see the changes like this, and also storing this score in users table might be beneficial in the future.


## Code Design
I seperated the application logic to Controllers, Services, and other related packages such as Config, Test, Repositories, Models etc. 

## Flow of the Application
After running the application, users can be created using /createUser endpoint. After creating the user, the level of the users can be updated using /updateLevel endpoint, this endpoint takes userId as a parameter and updates the necessary fields of the user. If the user is in an active group of a tournament, /updateLevel also updates the score field. 

If there is an active tournament, and user is qualified to enter the tournament, user can use tournaments/enterTournament endpoint with their userId. This endpoint searches for a suitable group for the user.

### Group Finding Logic
When a user makes a request to enter a tournament, all available groups are listed in a descending order according to the number of users in that group. This improves the performance of this operation since users try to fill the groups that has the most number of users, this will make them start the tournament faster. To check if the group is suitable, I simply check the countries field of the group and count the number of countries. If the count < 5, and the user's country is not in the group's countries, user is permitted to enter the group. 

However, concurrent requests to enter the tournament might trouble us. So, I implemented optimistic locking in the group finding phase. According to this protocol, if the current group that user is trying to join is modified by other requests in the process (checked by the version field in the user_groups), then the user aborts its transaction and tries to find a group again. After a limited amount of retries, we abort the process as a whole, and return a runtime error. 

After a user enters to a group, it waits until the group becomes full and active. After that, the users in that group can participate in the tournament by updating their progress with /updateLevel.

During a tournament, users can check their group's leaderboard, country leaderboard, and their rankings in their group by the requests: /tournaments/getGroupLeaderboard?groupId=, /tournaments/getCountryLeaderboard, and /getGroupRank?userId=. the group leaderboard consist of tuples (userId, score), and country leaderboard has also tuples (country, score).

After the tournament ends at 20.00 UTC, the top ranking 2 users in each group can claim their rewards using /claimReward?userId= 

## Real-Time Leaderboards
To create real-time leaderboards, I used Redis which is an in-memory database. When a tournament starts, it also creates the country:leaderboard Redis key that stores the place of our sorted set. Initially, all entries in the country leaderboard has the value 0. In the process of group forming, each user joining to a group adds their id, and score (initially 0) to their respective leaderboard. These group leaderboards created with the keys "group:leaderboard:{groupId}". Later in the tournament, each update to the score of the users is applied to these leaderboards. By using Redis sorted sets, my application implements Real-Time Country and Group Leaderboards. 





# Discord-Bot
Multi-Purpose Discord bot programmed using Java and dependencies imported using Maven.  Discord4J API was used for this project.  
## Functionalities
### Deleted Messages Logger 
* If any messages are deleted, the bot retrieves those messages from the cache, the text channel the message was sent in, and the user who deleted this message.  It then sends a message in the channel where the message was deleted and sends information about the deleted messages.
  * An important behavior to mention is that it cannot log messages that were sent before the bot is launched, but are deleted after the bot is launched.  This is because deleted    messages are not stored in Discord.  The bot caches all messages sent and simply retrieves it from its own cache when it is deleted and logs it.  
### Message Matcher
* The user can setup a message to be sent by the bot in response to another message.
    * For example, "!match ping ANDD pong" makes it so that whenever the message "ping" is sent, the bot responds with "pong".  
### Reminder Service
* The user can schedule a reminder through the use of the !reminder command.  Type !reminder for more information about the syntax.
    * The bot uses multithreading to devote the reminder to its own thread.  This ensures that the use of this service does not interrupt other commands as the thread is to be inactive until the reminder time expires.
### Prefix Change
* The user can change the command prefix to other symbols.  By default, it is **!** but it can be changed to any character now.  This helps increase the versatility of the bot when combined with other bots that may have conflicting command usage.
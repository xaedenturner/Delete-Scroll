This project uses Anroid Accessibility Events to stop the user from using Instagram Reels.
The current process works a bit like this:
1. The program asks for user permission to have access to accessibility features
2. An event is set which looks for any ui on the screen with the 'INSTAGRAM_PACKAGE' id, so it is only active when Instagram is loaded
3. In order for the program to work reliable even after Instagram has software updates, I've set it so instead of looking for specific id's to determine if there are reels on the screen it looks for different ui features and assigns them 'scores'
4. If the total score goes past the maximum threshold (3 at the moment) the event will automatically excecute the 'back' action through the accessibility features

I'm hoping to update this eventually so that instead of automatically exiting out, the program will just block any scroll detection
That way it will be possible to watch instagram reels the user's friends have sent them while still not allowing the user to scroll
There are still many errors with the reel detection, with a lot of false positives with screen overlays such as comments on posts and chats, so I'm sure the detection framework is far from optimal

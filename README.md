# Slack DJ
## Control a Mopidy server via Slack
### How to use
Requires a Mopidy server that is running on localhost:3000 on the same machine that this node application is ran on. This uses Spotify as the source of music so that needs to be configured on Mopidy.

Environment variables for the items below also need to be set;

Variable | Use
------------ | -------------
SLACK_TOKEN | Bot access token to required Slack Workspace
SLACK_DJ_ID | The Slack ID for the bot that handles requests
SLACK_CHANNEL | Channel where you want requests and responses to be handled
BAN_USER | An array of strings of user IDs that you do not want to make requests
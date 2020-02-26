const { parseMessage } = require("./lib/requestWorkflow");
const { RTMClient } = require(`@slack/rtm-api`);
const nodeCleanup = require(`node-cleanup`);
const Mopidy = require(`mopidy`);

const token = process.env.SLACK_TOKEN;
const conversationId = process.env.SLACK_CHANNEL;

// Creating RTM connection to Slack
const rtm = new RTMClient(token);
rtm.start().catch(console.error);
rtm.on(`hello`, async () => {
  await rtm.sendMessage(`Just getting ready!`, conversationId);
  mopidy.connect();
});
rtm.on(`message`, event => {
  console.log(event);
  if (event.subtype === undefined && event.channel === conversationId) {
    parseMessage(mopidy, rtm, event);
  }
});

// Creating connection to Mopidy server
const mopidy = new Mopidy({
  autoConnect: false,
  webSocketUrl: `ws://localhost:6680/mopidy/ws`,
  backoffDelayMax: 10000
});

mopidy.on(`state`, console.log);
mopidy.on(`event`, console.log);
mopidy.on(`state:online`, () => {
  rtm.sendMessage(`Throw me some tunes! I'm ready to go!`, conversationId);
  mopidy.tracklist.setConsume({ value: true });
});

nodeCleanup(function(exitCode, signal) {
  rtm.sendMessage(`I'm away here! All the best!`, conversationId);
});

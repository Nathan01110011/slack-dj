const { parseMessage } = require("./lib/requestWorkflow");
const { RTMClient } = require(`@slack/rtm-api`);
const nodeCleanup = require(`node-cleanup`);
const Mopidy = require(`mopidy`);

const token = process.env.SLACK_TOKEN;
const conversationId = process.env.SLACK_CHANNEL;
var offline = true;
var offlineMessageSent = false;

// Creating RTM connection to Slack
const rtm = new RTMClient(token);
rtm.start().catch(console.error);
rtm.on(`hello`, async () => {
  await rtm.sendMessage(`Hello everyone!`, conversationId);
  mopidy.connect();
});
rtm.on(`message`, event => {
  console.log(event);
  if (
    event.subtype === undefined &&
    event.channel === conversationId &&
    offline === false
  ) {
    parseMessage(mopidy, rtm, event);
  } else if (offline === true) {
    rtm.sendMessage(`Music server not up yet!`, conversationId);
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
  offlineMessageSent = false;
  offline = false;
});
mopidy.on(`state:offline`, () => {
  if (offlineMessageSent === false) {
    rtm.sendMessage(`Give me a second, music server offline.`, conversationId);
    offlineMessageSent = true;
    offline = true;
  }
});

nodeCleanup(function(exitCode, signal) {
  rtm.sendMessage(`I'm away here! All the best!`, conversationId);
});

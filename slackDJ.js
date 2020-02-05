const { RTMClient } = require("@slack/rtm-api");
const nodeCleanup = require("node-cleanup");
const Mopidy = require("mopidy");
const { setMusicState, addSong, showPlaying } = require("./functions.js");

const mopidy = new Mopidy({
  webSocketUrl: "ws://localhost:6680/mopidy/ws"
});

mopidy.on("state", console.log);
mopidy.on("event", console.log);

const token = process.env.SLACK_TOKEN;
const botID = process.env.SLACK_DJ_ID;
const conversationId = process.env.SLACK_CHANNEL;
const rtm = new RTMClient(token);

rtm.start().catch(console.error);
rtm.on("hello", async () => {
  const res = await rtm.sendMessage("Hello!", conversationId);
  console.log("Message sent: ", res.ts);
});

rtm.on("message", event => {
  //   console.log(event);
  parseMessage(event);
});

function parseMessage(message) {
  const botCheck = message.text.split(/\s(.+)/);
  console.log(message);
  if (botCheck[0] === botID) {
    if (userCheck(message.user)) {
      return;
    }

    console.log("Bot is mentioned");

    const command = botCheck[1].split(/\s(.+)/);
    console.log("command = " + command[0] + ", options = " + command[1]);
    mopidyFunctionality(command[0].toLowerCase(), command[1]);
  }
}

async function mopidyFunctionality(command, options) {
  var response;
  if (command === "what") {
    response = await showPlaying(mopidy, rtm).catch();
  } else if (command === "play") {
    response = await addSong(options, mopidy).catch();
  } else if (command === "music") {
    response = await setMusicState(options, mopidy).catch();
  }
  rtm.sendMessage(response, conversationId);
}

function userCheck(userID) {
  if (userID === process.env.BAN_USER) {
    rtm.sendMessage(
      "Sorry, I've been ordered to ignore you :grinning:",
      conversationId
    );
    return true;
  }
  return false;
}

nodeCleanup(function(exitCode, signal) {
  rtm.sendMessage(`I'm away here! All the best!`, conversationId);
});

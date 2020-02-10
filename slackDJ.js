const { RTMClient } = require("@slack/rtm-api");
const nodeCleanup = require("node-cleanup");
const Mopidy = require("mopidy");
const { commands } = require("./commands");

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

  if (botCheck[0] === botID) {
    if (userCheck(message.user)) {
      return;
    }

    console.log("Bot is mentioned");
    try {
      const command = botCheck[1].split(/\s(.+)/);
      console.log("command = " + command[0] + ", options = " + command[1]);
      mopidyFunctionality(command[0].toLowerCase(), command[1]);
    } catch (err) {
      console.error("Incorrect command format");
    }
  }
}

async function mopidyFunctionality(command, options) {
  var response;
  const comm = commands.filter(f => f.name == command);
  if (comm == null || options === undefined) {
    response = "Unrecognised command";
  } else {
    response = await comm[0].execute(options, mopidy).catch();
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

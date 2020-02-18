const { RTMClient } = require(`@slack/rtm-api`);
const nodeCleanup = require(`node-cleanup`);
const Mopidy = require(`mopidy`);
const { commands } = require(`./commands`);

const token = process.env.SLACK_TOKEN;
const botID = process.env.SLACK_DJ_ID;
const conversationId = process.env.SLACK_CHANNEL;

const rtm = new RTMClient(token);
rtm.start().catch(console.error);
rtm.on(`hello`, async () => {
  await rtm.sendMessage(`Just getting ready!`, conversationId);
  mopidy.connect();
});
rtm.on(`message`, event => {
  console.log(event);
  if (event.subtype === undefined && event.channel === conversationId) {
    parseMessage(event);
  }
});

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

function parseMessage(message) {
  const botCheck = message.text.split(/\s(.+)/);

  if (botCheck[0] === botID) {
    if (userCheck(message.user)) {
      return;
    }
    try {
      const command = botCheck[1].split(/\s(.+)/);
      mopidyFunctionality(command[0].toLowerCase(), command[1], message.user);
    } catch (err) {
      console.error(`Incorrect command format`);
    }
  }
}

async function mopidyFunctionality(command, options, user) {
  var response;
  const comm = commands.filter(f => f.name == command);
  if (comm.length === 0) {
    response = `Sorry, that doesn't seem like a valid command.`;
  } else {
    response = await comm[0].execute(mopidy, options, user, command).catch();
  }
  rtm.sendMessage(response, conversationId);
}

// TODO: Better way to block users
function userCheck(userID) {
  if (process.env.BAN_USER.includes(userID)) {
    rtm.sendMessage(
      `Sorry, I've been ordered to ignore you :grinning:`,
      conversationId
    );
    return true;
  }
  return false;
}

// TODO: Cooldown on song requests

nodeCleanup(function(exitCode, signal) {
  rtm.sendMessage(`I'm away here! All the best!`, conversationId);
});

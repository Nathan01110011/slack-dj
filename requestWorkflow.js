const { commands } = require(`./commands`);

const botID = process.env.SLACK_DJ_ID;
const conversationId = process.env.SLACK_CHANNEL;

function parseMessage(mopidy, rtm, message) {
  const botCheck = message.text.split(/\s(.+)/);
  if (botCheck[0] === botID) {
    if (userCheck(message.user)) {
      return;
    }
    try {
      const command = botCheck[1].split(/\s(.+)/);
      mopidyFunctionality(
        mopidy,
        rtm,
        command[0].toLowerCase(),
        command[1],
        message.user
      );
    } catch (err) {
      console.error(`Incorrect command format`);
    }
  }
}

async function mopidyFunctionality(mopidy, rtm, command, options, user) {
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

exports.parseMessage = parseMessage;

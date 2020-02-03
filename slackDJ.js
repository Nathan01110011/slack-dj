const { RTMClient } = require("@slack/rtm-api");
var nodeCleanup = require("node-cleanup");
const Mopidy = require("mopidy");

// TODO: This will need changed to a localhost port when moved to the pi
const mopidy = new Mopidy({
  webSocketUrl: "ws://localhost:6680/mopidy/ws"
});

mopidy.on("state", console.log);
mopidy.on("event", console.log);

const token = process.env.SLACK_TOKEN;
const rtm = new RTMClient(token);
const conversationId = process.env.SLACK_CHANNEL;

rtm.start().catch(console.error);

rtm.on("hello", async () => {
  const res = await rtm.sendMessage("Hello!", conversationId);
  console.log("Message sent: ", res.ts);
});

rtm.on("message", event => {
  //   console.log(event);
  parseMessage(event);
});

rtm.on("goodbye", async () => {
  const res = await rtm.sendMessage("Robo is offline!", conversationId);
  console.log("Message sent: ", res.ts);
});

nodeCleanup(function(exitCode, signal) {
  rtm.sendMessage(`I'm away here! All the best!`, conversationId);
});

function parseMessage(message) {
  const botCheck = message.text.split(/\s(.+)/);

  if (botCheck[0] === process.env.SLACK_DJ_ID) {
    console.log("Bot is mentioned");
    const command = botCheck[1].split(/\s(.+)/);
    console.log("command = " + command[0]);
    console.log("options = " + command[1]);

    mopidyFunctionality(command[0].toLowerCase(), command[1]);
  }
}

function mopidyFunctionality(command, options) {
  if (command === "what") {
    showPlaying();
  } else if (command === "play") {
    addSong(options);
  } else if (command === "music") {
    setMusicState(options);
  }
}

async function showPlaying() {
  let currentlyPlaying;
  try {
    const track = await mopidy.playback.getCurrentTrack();
    if (track) {
      currentlyPlaying =
        "Currently playing: " + track.name + " by " + track.artists[0].name;
      rtm.sendMessage(currentlyPlaying, conversationId);
    } else {
      currentlyPlaying = "No current track";
      rtm.sendMessage("No music is currently playing!", conversationId);
    }
  } catch (e) {
    console.warn("Something went wrong", e);
    currentlyPlaying = "error happened";
    rtm.sendMessage(
      "Don't know what's happened here ¯\\_(ツ)_/¯",
      conversationId
    );
  }
  return currentlyPlaying;
}

async function addSong(options) {
  const tracks = await mopidy.library.search({
    query: {
      track_name: options
    }
  });

  const trackURI = tracks[0].tracks[0].uri;

  mopidy.tracklist.add({ uris: [trackURI] })

}

async function setMusicState(state) {

    if (state === "pause") {
        mopidy.playback.pause();
    } else if (state === "play") {
        mopidy.playback.play();
    }
}
var currentSong;
var skipCount = 0;
var users = [];

async function skipTrack(mopidy, options, user, command) {
  await resetVarsCheck(mopidy).catch();
  const userCheck = multipleVoteCheck(user);
  if (userCheck !== undefined) {
    return userCheck;
  }
  return voteCountCheck(mopidy, command);
}

function multipleVoteCheck(user) {
  if (users.includes(user)) {
    return `Only one vote allowed per song I'm afraid.`;
  } else {
    users.push(user);
  }
}

async function resetVarsCheck(mopidy) {
  const track = await mopidy.playback.getCurrentTrack();
  if (currentSong === undefined) {
    currentSong === (await mopidy.playback.getCurrentTrack());
  } else if (currentSong.name !== track.name) {
    currentSong = await mopidy.playback.getCurrentTrack();
    skipCount = 0;
    users = [];
  }
}

async function voteCountCheck(mopidy, command) {
  command === "skip" ? skipCount++ : skipCount--;
  if (skipCount > 2) {
    mopidy.playback.next();
    skipCount = 0;
    users = [];
    return `Okay then, I hope that wasn't an absolute *_tune and a half!_*`;
  }
  return `Skip count is at ${skipCount}.`;
}

module.exports.skipTrack = skipTrack;

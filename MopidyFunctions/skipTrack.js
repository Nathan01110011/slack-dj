var currentSong;
var skipCount = 0;
var users = [];
var difference=0

async function skipTrack(mopidy, options, user) {
  await resetVarsCheck(mopidy).catch();
  const userCheck = multipleVoteCheck(user);
  if (userCheck !== undefined) {
    return userCheck;
  }
  if (command === "keep") {
    difference = -1;
  } else {
    difference = 1;
  }
  return voteCountCheck(mopidy, difference);
}

function multipleVoteCheck(user) {
  if (users.includes(user)) {
    return "Sorry mate this is a democracy, not a dictatorship.";
  } else {
    users.push(user);
  }
}

async function resetVarsCheck(mopidy) {
  const track = await mopidy.playback.getCurrentTrack();

  if (currentSong === undefined) {
    currentSong === (await mopidy.playback.getCurrentTrack());
  } else if (currentSong.name !== track.name) {
    currentSong = undefined;
    skipCount = 0;
    users = [];
  }
}

async function voteCountCheck(mopidy) {
  if (skipCount === 0) {
    skipCount + difference;
    currentSong = await mopidy.playback.getCurrentTrack();
    const count = 3 - skipCount;
    return "" + count + " more skips needed.";
  } else if (skipCount < 2) {
    skipCount + difference;
    const count = 3 - skipCount;
    return "" + count + " more skips needed.";
  } else {
    mopidy.playback.next();
    skipCount = 0;
    return "Okay then, I hope that wasn't an absolute *_tune and a half!_*";
  }
}

module.exports.skipTrack = skipTrack;

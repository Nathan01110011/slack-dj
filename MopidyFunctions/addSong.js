// TODO: Handle multiple requests from different users - arrays probably would work
// TODO: Add ability to specify artist
// TODO: Handle multiple artists for one song

var toBeQueued;
var requestUser;

async function addSong(mopidy, options, user, command) {
  if (options === undefined && command === "play") {
    return "Specify something to play.";
  } else if (command === "play") {
    return songCheck(mopidy, options, user);
  } else if (
    command === "yes" &&
    toBeQueued !== undefined &&
    user === requestUser
  ) {
    return songConfirmed(mopidy);
  } else if (
    command === "no" &&
    toBeQueued !== undefined &&
    user === requestUser
  ) {
    toBeQueued = undefined;
    user = undefined;
    return "Oh dear. Try a better search term.";
  } else if (requestUser !== user) {
    return "This wasn't your request!";
  } else {
    return "You need to ask for a song first.";
  }
}

async function songCheck(mopidy, options, user) {
  const tracks = await mopidy.library
    .search({
      query: {
        track_name: options
      },
      uris: [`spotify:`]
    })
    .catch(function(error) {
      return `Something is broke :(`;
    });

  if (tracks[0].tracks === undefined) {
    return `Can't find anything like that :thinking_face:`;
  }

  toBeQueued = tracks[0].tracks[0];
  requestUser = user;
  var trackSelected = toBeQueued.name + ` by ` + toBeQueued.artists[0].name;
  return `So that's "` + trackSelected + `" to be played?`;
}

async function songConfirmed(mopidy) {
  mopidy.tracklist.add({ uris: [toBeQueued.uri] });
  toBeQueued = undefined;
  user = undefined;
  const currentState = await mopidy.playback.getState().catch(function(error) {
    return `Something is broke :(`;
  });
  if (currentState === `stopped`) {
    await mopidy.playback.play().catch();
    return `Playing that now!`;
  }
  return `Song added to the queue.`;
}

module.exports.addSong = addSong;

// TODO: Handle multiple artists for one song
// TODO: Cooldown on song requests

var toBeQueued = [];
var songList = [];
var userCooldown = [];

async function addSong(mopidy, options, user, command) {
  var req = toBeQueued.filter(f => f.reqUser == user);
  if (options === undefined && command === "play") {
    return "Specify something to play.";
  } else if (command === "play") {
    return songCheck(mopidy, options, user, req).catch();
  } else if (command === "yes" && req.length > 0) {
    const confirmed = await songConfirmed(mopidy, req);
    return confirmed;
  } else if (command === "no" && req.length > 0) {
    toBeQueued.splice(toBeQueued.indexOf(req));
    return "oh dear";
  } else {
    return "Have you requested a song yet?";
  }
}

async function songCheck(mopidy, options, user, req) {
  if (req) {
    toBeQueued.splice(toBeQueued.indexOf(req));
  }
  const tracks = await mopidy.library
    .search({
      query: {
        any: [options]
      },
      uris: [`spotify:`]
    })
    .catch(function(error) {
      return `Something is broke :(`;
    });

  console.log(tracks);

  if (tracks.length === 0 || tracks[0].tracks == undefined) {
    return `Can't find anything like that :thinking_face:`;
  }

  toBeQueued.push({ reqUser: user, track: tracks[0].tracks[0] });
  requestUser = user;

  var selectedTrack = tracks[0].tracks[0];
  const username = `<@` + user + `> `;
  return (
    username +
    `So that's ` +
    selectedTrack.name +
    ` by ` +
    selectedTrack.artists[0].name +
    ` to be played?`
  );
}

async function songConfirmed(mopidy, req) {
  const songUri = req[0].track.uri;

  if (songList.includes(songUri)) {
    return "This has already been played today.";
  }
  songList.push(songUri);
  mopidy.tracklist.add({ uris: [songUri] });
  toBeQueued.splice(toBeQueued.indexOf(req));

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

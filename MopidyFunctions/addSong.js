// TODO: Searching by artist and confirmation of song choice

async function addSong(options, mopidy) {
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
    return `Something is broke :(`;
  }

  const track = tracks[0].tracks[0];
  var trackSelected = track.name + ` by ` + track.artists[0].name;
  mopidy.tracklist.add({ uris: [track.uri] });

  const currentState = await mopidy.playback.getState().catch(function(error) {
    return `Something is broke :(`;
  });
  if (currentState === `stopped`) {
    await mopidy.playback.play().catch();
  }
  return trackSelected + ` added to queue.`;
}

module.exports.addSong = addSong;

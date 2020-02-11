async function helpCommand(options, mopidy) {
  var helpInstructions =
    `Hello! You can tell me what to do with the following commands!` +
    `\n *play* "_insert search term_" : Queue up a specfic song!` +
    `\n *music* "pause/play" : start or stop playback ` +
    `\n *what is playing* : I'll tell you what's currently playing` +
    `\n *belter time* : queues up an absolute belter`;

  return helpInstructions;
}

module.exports.helpCommand = helpCommand;

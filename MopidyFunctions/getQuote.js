async function getQuote() {
  const quote = quoteArray[Date.now() % (quoteArray.length - 1)];
  return quote;
}

const quoteArray = [
  `Do you know what this bathroom says to me? Aqua. Which is French for water. It’s like being inside an enormous Fox’s Glacier Mint. Which, again, to me is a bonus.`,
  `The temperature inside this apple pie is over 1000 degrees. If I squeeze it, a jet of molten bramley apple will squirt out. Could go your way; could go mine. Either way, one of us is going down!`,
  `Let me tell you something about the Titanic: people forget that on the Titanic’s maiden voyage there were over 1000 miles of uneventful, very pleasurable cruising before it hit the iceberg.`,
  `My favourite Beatles album? Tough one! I think I’d have to say… ‘The Best Of The Beatles’.`,
  `The pace of the Megane is too leisurely to be called quick. Uphill runs become power sappingly mundane whilst overtaking National Express coaches becomes a long, drawn-out affair. Not my words Carol, the words of Top Gear magazine`,
  `Let battle commence!`,
  `The proof is in the pudding, and the pudding, in this case, is football.`,
  `You've got to laugh when you fall off a sofa.`,
  `Right, I'll tell you an anecdote. In 1974 I was catching the London train from Crewe station. It was very crowded; I found myself in a last-minute rush for the one remaining seat beside a tall, good-looking man with collar-length hair, it was the seventies; buckaroo! I looked up and saw it was none other than Peter Purves, it was the height of his Blue Peter career. He said, 'You jammy b******' and quick as a flash, I replied, 'Don't be blue, Peter! Needless to say, I had the last laugh.`,
  `I can read you like a book, and not a very good book. Especially not Bravo Two Zero by Andy McNabb, which actually improves with every read.`,
  `A detective series based in Norwich called "Swallow". Swallow is a detective who tackles vandalism. Bit of a maverick, not afraid to break the law if he thinks it's necessary. He's not a criminal, you know, but he will, perhaps, travel 80mph on the motorway if, for example, he wants to get somewhere quickly...Think about it. No one had heard of Oxford before Inspector Morse.`,
  `Later we'll be taking dedications for anyone wrongly turned down for planning permission. Also, I'll be asking: Which is the worst monger? Fish, iron, rumour or war?`,
  `Divorced. I've got access to the kids... But they don't wanna see me.`,
  `I’d just like to fly a helicopter all around Norfolk. You know, swoop down over a field. Scare a donkey so that it falls into a river. Hover over one of those annoying families that go on holidays on bikes. And shout at them “get out of the area!” and watch them panic!`,
  `Britain has some of the safest roads in Europe. But this isn't Britain...This is der Autobahn!`,
  `Convoy? Michael, you’re hanging around with a man who uses a collective term for a single vehicle.`,
  `Yes, it's an extender! Fantastic. That is the icing on the cake. You know, if King Arthur had an extender on his table. Well, it wouldn't have been round.`,
  `Chap there parked on the wrong side of the pumps. Amazing the number of people who still think the petrol cap to a Ford Focus is on the offside rear.`,
  `I was a bit bored so I dismantled my Corby Trouser Press. I can’t put it back together again. Will that show up on my bill?`,
  `Quick tip for yourself: if you’re ever doing an after-dinner speech, you say “My Lords, Ladies and Gentlemen, sorry I’m late, I just popped to the toilet. And while I was there, I saw some graffiti and it said ‘I used to be indecisive, but now I’m not so sure.’ Straight away you’ve got them by the jaffas.`,
  `Farmers. If you see a lovely field with a family having a picnic, and a nice pond in it, you fill in the pond with concrete, you plow the family into the soil, you blow up the tree, and use the leaves to make a dress for your wife who is also your brother.`,
  `Go to London, I guarantee you’ll either be mugged or not appreciated. Catch the train to London, stopping at Rejection, Disappointment, Backstabbing Central and Shattered Dreams Parkway.`,
  `Do you want to hear the good news or the bad news?\nThe good news.\nWell, Rawlinson's say you can have another fifty of the shop-soiled chocolate oranges if you plug them again tomorrow.\nExcellent. And the bad news?\nThe accountants say that since you've definitely not got a second series at the BBC you're going to have to sack everyone at Pear Tree Productions and close the office down. Otherwise they're going to declare you bankrupt on Friday.\nRight. Still, good news about the chocolate oranges.`,
  `That was ‘Big Yellow Taxi’ by Joni Mitchell, a song in which Joni complains they ‘Paved paradise to put up a parking lot’, a measure which actually would have alleviated traffic congestion on the outskirts of paradise, something which Joni singularly fails to point out, perhaps because it doesn’t quite fit in with her blinkered view of the world. Nevertheless, nice song.`,
  `‘Sunday Bloody Sunday’. What a great song. It really encapsulates the frustration of a Sunday, doesn’t it? You wake up in the morning, you’ve got to read all the Sunday papers, the kids are running round, you’ve got to mow the lawn, wash the car, and you just think ‘Sunday, bloody Sunday!'`,
  `All this wine nonsense! You get all these wine people, don’t you? Wine this, wine that. Let’s have a bit of red, let’s have a bit of white. Ooh, that’s a snazzy bouquet. Oh, this smells of, I don’t know, basil. Sometimes you just want to say, sod all this wine, just give me a pint of…mineral water`
];

module.exports.getQuote = getQuote;

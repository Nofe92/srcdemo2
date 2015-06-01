To kick things off and give people a reason to read this wall of text, here's a striking example of how important getting the right shutter angle is. This is the same scene, recorded at multiple shutter angles:

http://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/Windflower-05237-nevit.JPG/800px-Windflower-05237-nevit.JPG

Interested? Good, let's begin.

This is a camera with a 180° shutter angle:

![http://upload.wikimedia.org/wikipedia/commons/a/ad/Moviecam_schematic_animation.gif](http://upload.wikimedia.org/wikipedia/commons/a/ad/Moviecam_schematic_animation.gif)

As you can see, each physical frame of the film is exposed to photons 50% of the time.
For a 30 frames per second video, a new physical frame is made every 1/30th of a second.
However, since it is only exposed 50% of the time, only 1/60th of a second's worth of exposure has gone onto the frame. The other 1/60th of a second is simply gone, not recorded.

As you can imagine, a higher shutter angle means a longer exposure time. For example, a 270° shutter angle, the frame would get 3/120th of a second's worth of exposure, and 1/120th of a second would be gone. With a 360° shutter angle, the frame will get the full 1/30th worth of exposure, and nothing will be lost.

Now you may wonder: "I don't like data loss! Why not always use 360°? And why is 180° the default?"
The reason for that is because of the human brain. Unlike computers, where the "more data == better" applies, the brain doesn't see animation as such. The eye sees a series of still images, which the brain intuitively and subconsciously interprets and computes motion between the frames. This final "moving" scene is then what you see. The perception of motion is only due to your brain doing some pretty intensive work (that computers take ages to do!) just for you.

The brain is very good at this; even from a low-framerate video, you can usually easily tell what is moving where, and how fast it is moving, without too much conscious brain effort.

However, it is true that if you artificially feed the brain more frames than it needs in order to perceive motion, the overall work will appear smoother.
Thus, higher shutter angle means more frames means smoother motion, right? No.
Despite the fact that feeding more frames helps, the brain still knows that images arrive at a constant rate, thus that **there is a delay between each frame**. It is used to that delay. If you use a 360° shutter angle, **the perceived delay between frame n-1 and frame n is much smaller than what the brain expects**, because the last blended frame in frame n-1 is much closer temporally to the first blended frame in frame n.
As a result, the brain will get confused and will make you feel like things are moving too fast, making you disoriented a bit.

This is why a too high shutter angle is a bad thing.

Wanna see how much of a difference it makes? Here's an example, though it is not the clearest one:

<wiki:gadget url="http://srcdemo2.googlecode.com/git-history/googlecode-wiki-vimeo/gcVimeo.xml" up\_video="http://vimeo.com/5249682" width="800" height="450"/>

As you can see, the higher the exposure time (higher shutter angle), the smoother the water movement looks, but when you go too low (1/30, which is 360° shutter angle), it looks blurry and kinda crappy.

Another good way to see this is simply to look at existing videos made by the TF2 replay renderer! It uses 360° degrees (which means it doesn't care about shutter angle at all, just records and blends all frames).

Here is a sample one:

<a href='http://www.youtube.com/watch?feature=player_embedded&v=cQUNhV6UIGI' target='_blank'><img src='http://img.youtube.com/vi/cQUNhV6UIGI/0.jpg' width='800' height=475 /></a>

Watch in fullscreen to get a better feel. If you don't feel it, try looking at the sides/corners of the screen especially. If you still don't feel it, you've probably watched too many replays by now and are immune to the effect or something.

As you can see, while the video does look very, very smooth, it looks smooth to the point of being sickeningly blurry.

And that, my friends, is why you will [Learn, Live, and Love the 180° shutter angle](http://tylerginter.com/post/11480534977/180-degree-shutter-learn-it-live-it-love-it). It is still not a completely natural effect, but the inter-frame time is much closer to what the brain expects, and the motion blur quality is optimal. It is what is used on film too, and even though today's cameras obviously don't use rotary shutters anymore, they still do use shutter angle simulation, because that's simply what people are used to and find comfortable.  This is also why, for once, loss of data (in this case, half of the frames) is a **good thing**.

I hope this explanation was clear. It is not a very easy-to-explain concept, but it does affect the output of SrcDemo² a lot, so I thought I'd define it more clearly.
/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.audio.queue;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleTrackProvider extends AbstractTrackProvider {

    private volatile ConcurrentLinkedQueue<AudioTrackContext> queue = new ConcurrentLinkedQueue<>();
    private AudioTrackContext lastTrack = null;
    private List<AudioTrackContext> cachedShuffledQueue = new ArrayList<>();
    private boolean shouldUpdateShuffledQueue = true;

    @Override
    public AudioTrackContext getNext() {
        if(!isShuffle()) {
            return queue.peek();
        } else {
            return getAsListOrdered().get(0);
        }
    }

    @Override
    public AudioTrackContext provideAudioTrack(boolean skipped) {
        if (isRepeat() && !skipped && lastTrack != null) {
            return lastTrack.makeClone();
        }
        if (isShuffle()) {
            //Get random int from queue, remove it and then return it
            List<AudioTrackContext> list = getAsListOrdered();

            if (list.isEmpty()) {
                return null;
            }

            shouldUpdateShuffledQueue = true;
            lastTrack = list.get(0);
            queue.remove(lastTrack);
            return lastTrack;
        } else {
            lastTrack = queue.poll();
            return lastTrack;
        }
    }

    @Override
    public AudioTrackContext removeAt(int i) {
        if(queue.size() < i){
            return null;
        } else {
            int i2 = 0;
            for(AudioTrackContext obj : getAsListOrdered()){
                if(i == i2){
                    shouldUpdateShuffledQueue = true;
                    //noinspection SuspiciousMethodCalls
                    queue.remove(obj);
                    return obj;
                }
                i2++;
            }
        }

        return null;
    }

    @Override
    public List<AudioTrackContext> getAsList() {
        return new ArrayList<>(queue);
    }

    @Override
    public synchronized List<AudioTrackContext> getAsListOrdered() {
        if(!isShuffle()){
            List<AudioTrackContext> list = getAsList();

            return list;
        }

        if(!shouldUpdateShuffledQueue){
            return cachedShuffledQueue;
        }

        List<AudioTrackContext> newList = new ArrayList<>();

        //Update the new queue
        int i = 1;
        for (AudioTrackContext atc : getAsList()) {
            newList.add(atc);
            i++;
        }

        Collections.sort(newList);
        cachedShuffledQueue = newList;

        shouldUpdateShuffledQueue = false;
        return newList;
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void add(AudioTrackContext track) {
        shouldUpdateShuffledQueue = true;
        queue.add(track);
    }

    @Override
    public void clear() {
        shouldUpdateShuffledQueue = true;
        queue.clear();
    }
}

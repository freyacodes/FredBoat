/*
 *
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
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
 */

package fredboat.audio.queue;

import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.entity.common.AtcData;
import fredboat.db.entity.common.Tracklist;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by napster on 24.06.17.
 * <p>
 * Keeps a regular playlist and a shuffled one around. Keep in mind that only the one currently used to play is fully up
 * to date. They are synced when switching between regular and shuffled mode. For a more elaborate explanation look at
 * the syncLists() method below.
 * todo profile how often the methods are called for each command
 */
public class PersistentGuildTrackProvider extends AbstractTrackProvider {

    private static final Logger log = LoggerFactory.getLogger(PersistentGuildTrackProvider.class);

    private AudioTrackContext lastTrack = null;
    private final long guildId;
    private final Object tracklistLock;
    private Tracklist regular;
    private Tracklist shuffled;


    public PersistentGuildTrackProvider(long guildId) {
        log.debug("Constructing PersistentGuildTrackProvider({})", guildId);

        this.guildId = guildId;
        this.tracklistLock = new Object();
        regular = Tracklist.load(guildId, "regular");
        shuffled = Tracklist.load(guildId, "shuffled");
    }

    @Override
    public void setShuffle(boolean shuffle) {
        syncLists(shuffle);
        super.setShuffle(shuffle);
    }

    @Override
    public void skipped() {
        lastTrack = null;
    }

    @Override
    public void setLastTrack(AudioTrackContext lastTrack) {
        this.lastTrack = lastTrack;
    }

    @Override
    public AudioTrackContext provideAudioTrack() {
        log.debug("provideAudioTrack() called");
        if (getRepeatMode() == RepeatMode.SINGLE && lastTrack != null) {
            return lastTrack.makeClone();
        }

        if (getRepeatMode() == RepeatMode.ALL && lastTrack != null) {
            //if the queue is being repeated
            //add a fresh copy of the last track back to it
            synchronized (tracklistLock) {
                if (isShuffle()) {
                    shuffled = shuffled.add(lastTrack.trackId)
                            .save();
                } else {
                    regular = regular.add(lastTrack.trackId)
                            .save();
                }
            }
        }

        lastTrack = getNext(true);
        return lastTrack;
    }

    @Override
    public AudioTrackContext peek() {
        return getNext(false);
    }

    //gets the next track, removes it from tracklists if specified
    private AudioTrackContext getNext(boolean remove) {
        log.debug("getNext() called");
        AtcData atcData;
        long trackId;
        synchronized (tracklistLock) {
            if (isShuffle()) {
                if (shuffled.isEmpty()) {
                    return null;
                }
                trackId = shuffled.get(0);
                if (remove) {
                    shuffled.removeAt(0);
                    shuffled = shuffled.save();
                }
            } else {
                if (regular.isEmpty()) {
                    return null;
                }
                trackId = regular.get(0);
                if (remove) {
                    regular.removeAt(0);
                    regular = regular.save();
                }
            }
        }

        atcData = EntityReader.getEntity(trackId, AtcData.class);
        if (atcData == null) {
            return null;
        }

        try {
            return atcData.restoreTrack();
        } catch (IOException e) {
            log.error("Could not decode track with id {} when loading from database", atcData.getTrackId(), e);
            return null;
        }
    }

    @Override
    public List<AudioTrackContext> getAsList() {
        log.debug("getAsList() called");

        syncLists(!isShuffle());
        List<Long> trackIds = Arrays.asList(ArrayUtils.toObject(regular.subArray(0, regular.size())));
        List<AtcData> tracksData = EntityReader.getEntities(trackIds, AtcData.class);
        return tracksFromData(tracksData);
    }

    @Override
    public List<AudioTrackContext> getAsListOrdered() {
        log.debug("getAsListOrdered() called");

        syncLists(!isShuffle());

        List<Long> trackIds;
        if (isShuffle()) {
            trackIds = Arrays.asList(ArrayUtils.toObject(shuffled.subArray(0, shuffled.size())));
        } else {
            trackIds = Arrays.asList(ArrayUtils.toObject(regular.subArray(0, regular.size())));
        }

        List<AtcData> tracksData = EntityReader.getEntities(trackIds, AtcData.class);
        return tracksFromData(tracksData);
    }


    @Override
    public boolean isEmpty() {
        log.debug("isEmpty() called");

        return size() == 0;
    }

    @Override
    public int size() {
        log.debug("size() called");

        if (isShuffle()) {
            return shuffled.size();
        } else {
            return regular.size();
        }
    }


    @Override
    public void add(AudioTrackContext track) {
        log.debug("add({}) called", track.trackId);

        AtcData atcData;
        try {
            atcData = EntityWriter.merge(new AtcData(track));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        synchronized (tracklistLock) {
            shuffled = shuffled.randAdd(atcData.getTrackId())
                    .save();
            regular = regular.add(atcData.getTrackId())
                    .save();
        }
    }

    @Override
    public void addAll(Collection<AudioTrackContext> tracks) {
        log.debug("addAll({}) called", tracks.size());

        List<AtcData> data = new ArrayList<>();

        for (AudioTrackContext track : tracks) {
            try {
                data.add(new AtcData(track));
            } catch (IOException e) {
                log.error("Skipping track with id {} when saving to database", track.getTrackId(), e);
            }
        }
        EntityWriter.persistAll(data);
        synchronized (tracklistLock) {
            for (AtcData atc : data) shuffled.randAdd(atc.getTrackId());
            shuffled = shuffled.save();
            regular = regular.addAll(data.stream().map(AtcData::getTrackId).collect(Collectors.toList()))
                    .save();
        }

    }

    @Override
    public void clear() {
        log.debug("clear() called");

        lastTrack = null;

        synchronized (tracklistLock) {
            shuffled = shuffled.clear()
                    .save();
            regular = regular.clear()
                    .save();
        }
    }

    @Override
    public boolean remove(AudioTrackContext atc) {
        log.debug("remove({}) called", atc.trackId);

        synchronized (tracklistLock) {
            boolean s = shuffled.remove(atc.trackId);
            shuffled = shuffled.save();
            boolean r = regular.remove(atc.trackId);
            regular = regular.save();
            return s && r;
        }
    }

    @Override
    public void removeAll(Collection<AudioTrackContext> tracks) {
        log.debug("removeAll(size: {}) called", tracks.size());

        Collection<Long> trackIds = tracks.stream().map(AudioTrackContext::getTrackId).collect(Collectors.toList());

        removeAllById(trackIds);
    }

    @Override
    public void removeAllById(Collection<Long> trackIds) {
        log.debug("removeAllByIds(size: {}) called", trackIds.size());

        synchronized (tracklistLock) {
            shuffled = shuffled.removeAll(trackIds)
                    .save();
            regular = regular.removeAll(trackIds)
                    .save();
        }
    }

    @Override
    public AudioTrackContext getTrack(int index) {
        log.debug("getTrack({}) called", index);

        long trackId;
        if (isShuffle()) {
            trackId = shuffled.get(index);
        } else {
            trackId = regular.get(index);
        }
        AtcData atcData = EntityReader.getEntity(trackId, AtcData.class);
        try {
            return atcData.restoreTrack();
        } catch (IOException e) {
            log.error("Could not decode track with id {} when loading from database", atcData.getTrackId(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AudioTrackContext> getTracksInRange(int indexA, int indexB) {
        log.debug("getTracksInRange({}, {}) called", indexA, indexB);

        Collection<Long> trackIds = getTrackIdsInRange(indexA, indexB);

        List<AtcData> results = new ArrayList<>();
        for (long trackId : trackIds) {
            AtcData trackData = EntityReader.getEntity(trackId, AtcData.class);
            if (trackData == null) {
                log.warn("Track with id {} not found in the database. Investigate why we are requesting a non-existent track.", trackId);
                continue;
            }
            results.add(trackData);
        }
        return tracksFromData(results);
    }


    @Override
    public long getDurationMillis() {
        log.debug("getDuration() called");

        List<Long> trackIds;
        if (isShuffle()) {
            trackIds = Arrays.asList(ArrayUtils.toObject(shuffled.subArray(0, shuffled.size())));
        } else {
            trackIds = Arrays.asList(ArrayUtils.toObject(regular.subArray(0, regular.size())));
        }

        if (trackIds.isEmpty()) return 0;

        String query = "SELECT SUM(a.duration) FROM AtcData a WHERE a.guildId = :guildId AND a.trackId IN :trackIds";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("guildId", guildId);
        parameters.put("trackIds", trackIds);

        List<Long> result = EntityReader.selectJPQLQuery(query, parameters, Long.class);

        if (result == null || result.isEmpty() || result.get(0) == null) {
            return 0;
        } else {
            log.debug("Duration: {}", result.get(0));
            return result.get(0);
        }
    }

    @Override
    public int streamsCount() {
        log.debug("streamsCount() called");

        List<Long> trackIds;
        if (isShuffle()) {
            trackIds = Arrays.asList(ArrayUtils.toObject(shuffled.subArray(0, shuffled.size())));
        } else {
            trackIds = Arrays.asList(ArrayUtils.toObject(regular.subArray(0, regular.size())));
        }

        if (trackIds.isEmpty()) return 0;


        String query = "SELECT COUNT(a) FROM AtcData a WHERE a.isStream = true AND a.guildId = :guildId AND a.trackId IN :trackIds";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("guildId", guildId);
        parameters.put("trackIds", trackIds);

        return EntityReader.selectJPQLQuery(query, parameters, Long.class).get(0).intValue();
    }

    @Override
    public void reshuffle() {
        log.debug("reshuffle() called");

        synchronized (tracklistLock) {
            shuffled = shuffled.shuffle()
                    .save();
        }
    }

    public List<Long> getTrackIdsInRange(int indexA, int indexB) {
        //make sure startIndex <= endIndex
        int startIndex = indexA < indexB ? indexA : indexB;
        int endIndex = indexA < indexB ? indexB : indexA;
        if (startIndex < 0) startIndex = 0;
        if (endIndex < 0) endIndex = 0;

        int size = size();
        if (startIndex >= size) return Collections.emptyList();
        if (endIndex > size) endIndex = size;

        long[] trackIds;
        if (isShuffle()) {
            trackIds = shuffled.subArray(startIndex, endIndex);
        } else {
            trackIds = regular.subArray(startIndex, endIndex);
        }
        return Arrays.asList(ArrayUtils.toObject(trackIds));
    }

    @Override
    public boolean isUserTrackOwner(long userId, Collection<Long> trackIds) {
        if (trackIds.isEmpty()) {
            return true; //this is kind of a weird answer
        }

        String query = "SELECT COUNT(a) FROM AtcData a WHERE a.trackId IN :trackIds AND a.userId != :userId";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        parameters.put("trackIds", trackIds);
        return 0 == EntityReader.selectJPQLQuery(query, parameters, Long.class).get(0);
    }

    //restore a collection of tracks from data, ignoring exceptions
    private List<AudioTrackContext> tracksFromData(Collection<AtcData> data) {
        List<AudioTrackContext> tracks = new ArrayList<>();
        for (AtcData atcData : data) {
            try {
                tracks.add(atcData.restoreTrack());
            } catch (IOException e) {
                log.error("Skipping track with id {} when loading from database", atcData.getTrackId(), e);
            }
        }
        return tracks;
    }

    //When playing songs from the regular queue, if we sync the shuffled list all the time,  we run into the issue of
    //removing a track from the  middle of the shuffled list, but no way to readd it to its previous place in the
    //shuffled list (neither saving an index nor its neigbours really works as users can add and remove songs in the meantime)
    //in case ;;repeat all gets turned on. The same is applies vice-versa for the regular list while the shuffled tracklist is playing.
    //Thats why we sync the two lists when we are switching between them
    private void syncLists(boolean requestedShuffleMode) {
        log.debug("syncLists() called");

        boolean currentlyShuffled = isShuffle();

        if (requestedShuffleMode == currentlyShuffled) //no actual change of the shuffle mode happening, dont need to sync
            return;

        Tracklist source;
        Tracklist target;
        if (currentlyShuffled) {
            //syncing shuffled -> regular
            source = shuffled;
            target = regular;
        } else {
            //syncing regular -> shuffled
            source = regular;
            target = shuffled;
        }

        synchronized (tracklistLock) {
            //temporary copy of the source tracklist, so we can remove tracks from it without affecting the real tracklist
            Tracklist all = Tracklist.create(0, "tmp", source.subArray(0, source.size()));

            long[] oldList = target.subArray(0, target.size());
            target.clear();

            // - skip track ids that are not in the source tracklist
            for (long trackId : oldList) {
                if (all.remove(trackId)) {
                    target.add(trackId);
                }
            }
            // - add track ids that are in the source list but missing from the target one
            for (long leftOverTrackId : all.subArray(0, all.size())) {
                if (target.equals(shuffled)) {
                    target.randAdd(leftOverTrackId);
                } else {
                    target.add(leftOverTrackId);
                }
            }
            if (target.equals(shuffled)) {
                shuffled = target.save();
            } else {
                regular = target.save();
            }
        }
    }
}

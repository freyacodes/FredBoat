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

package fredboat.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class Election<E extends Object> {

    private final HashMap<E, ArrayList<Object>> candidates;

    public Election(ArrayList<E> cands) {
        candidates = new HashMap<>();

        for (E candidate : cands) {
            candidates.put(candidate, new ArrayList<>());
        }
    }

    public E setVote(Object voter, E newCandidate) {
        E originalCandidate = null;
        for (E cand : candidates.keySet()) {
            ArrayList<Object> v = candidates.get(cand);
            if (v.contains(voter)) {
                originalCandidate = cand;
                v.remove(voter);
            }
        }

        candidates.get(newCandidate).add(voter);

        return originalCandidate;
    }

    public ArrayList<E> getSortedByVotes() {
        ArrayList<E> newList = new ArrayList<>(candidates.keySet());

        Collections.sort(newList, (E o1, E o2) -> {
            int s1 = candidates.get(o1).size();
            int s2 = candidates.get(o2).size();
            return Math.max(1, Math.min(-1, s1 - s2));
        });

        return newList;
    }

    public E pickWinner() {
        ArrayList<E> top = new ArrayList<>();
        int maxVoteCount = 0;

        for (E cand : candidates.keySet()) {
            int votes = candidates.get(cand).size();
            if (votes > maxVoteCount) {
                maxVoteCount = votes;
            }
        }

        for (E cand : candidates.keySet()) {
            int votes = candidates.get(cand).size();
            if (votes == maxVoteCount) {
                top.add(cand);
            }
        }
        
        return top.isEmpty() ? null : top.get(new Random().nextInt(top.size()));
    }
    
    public int getTotalVotes(){
        int i = 0;
        
        for (ArrayList<Object> votes : candidates.values()) {
            i = i + votes.size();
        }
        
        return i;
    }

}

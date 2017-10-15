/*
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
 *
 */

package fredboat.util;

import fredboat.commandmeta.abs.CommandContext;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ArgumentUtil {

    public static final int FUZZY_RESULT_LIMIT = 10;

    private ArgumentUtil() {
    }

    public static List<Member> fuzzyMemberSearch(Guild guild, String term, boolean includeBots) {
        ArrayList<Member> list = new ArrayList<>();

        term = term.toLowerCase();

        for (Member mem : guild.getMembers()) {
            if ((mem.getUser().getName().toLowerCase() + "#" + mem.getUser().getDiscriminator()).contains(term)
                    || (mem.getEffectiveName().toLowerCase().contains(term))
                    || term.contains(mem.getUser().getId())) {

                if (!includeBots && mem.getUser().isBot()) continue;
                list.add(mem);
            }
        }

        return list;
    }

    public static List<Role> fuzzyRoleSearch(Guild guild, String term) {
        ArrayList<Role> list = new ArrayList<>();

        term = term.toLowerCase();

        for (Role role : guild.getRoles()) {
            if ((role.getName().toLowerCase()).contains(term)
                    || term.contains(role.getId())) {
                list.add(role);
            }
        }

        return list;
    }

    public static Member checkSingleFuzzyMemberSearchResult(CommandContext context, String term) {
        return checkSingleFuzzyMemberSearchResult(context, term, false);
    }

    public static Member checkSingleFuzzyMemberSearchResult(CommandContext context, String term, boolean includeBots) {
        List<Member> list = fuzzyMemberSearch(context.guild, term, includeBots);

        switch (list.size()) {
            case 0:
                context.reply(context.i18nFormat("fuzzyNothingFound", term));
                return null;
            case 1:
                return list.get(0);
            default:
                StringBuilder searchResults = new StringBuilder();
                int maxIndex = Math.min(FUZZY_RESULT_LIMIT, list.size());
                for (int i = 0; i < maxIndex; i++) {
                    searchResults.append("\n")
                            .append(list.get(i).getUser().getName())
                            .append("#")
                            .append(list.get(i).getUser().getDiscriminator());
                }

                if (list.size() > FUZZY_RESULT_LIMIT) {
                    searchResults.append("\n[...]");
                }

                context.reply(context.i18n("fuzzyMultiple") + "\n"
                        + TextUtils.asCodeBlock(searchResults.toString()));
                return null;
        }
    }

    public static IMentionable checkSingleFuzzySearchResult(List<IMentionable> list, CommandContext context, String term) {
        switch (list.size()) {
            case 0:
                context.reply(context.i18nFormat("fuzzyNothingFound", term));
                return null;
            case 1:
                return list.get(0);
            default:
                StringBuilder searchResults = new StringBuilder();
                int i = 0;
                for (IMentionable mentionable : list) {
                    if (i == FUZZY_RESULT_LIMIT) break;

                    if (mentionable instanceof Member) {
                        Member member = (Member) mentionable;
                        searchResults.append("\nUSER ")
                                .append(member.getUser().getId())
                                .append(" ")
                                .append(member.getEffectiveName());
                    } else if (mentionable instanceof Role) {
                        Role role = (Role) mentionable;
                        searchResults.append("\nROLE ")
                                .append(role.getId())
                                .append(" ")
                                .append(role.getName());
                    } else {
                        throw new IllegalArgumentException("Expected Role or Member, got " + mentionable);
                    }
                    i++;
                }

                if (list.size() > FUZZY_RESULT_LIMIT) {
                    searchResults.append("\n[...]");
                }

                context.reply(context.i18n("fuzzyMultiple") + "\n"
                        + TextUtils.asCodeBlock(searchResults.toString()));
                return null;
        }
    }

    public static String getSearchTerm(Message message, String[] args, int argsToStrip) {
        String raw = message.getRawContent();
        raw = raw.substring(args[0].length() + args[1].length() + 2); //arg0 is prefix, arg1 is add / remove and + 2 for the 2 spaces
        return raw.substring(raw.indexOf(args[argsToStrip]));
    }

    /**
     * Helper method to combine all the options from command into a single String.
     * Will call trim on each combine.
     *
     * @param args Command arguments.
     * @return String object of the combined args or empty string.
     */
    public static String combineArgs(@Nonnull String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg.trim());
        }
        return sb.toString().trim();
    }

}
